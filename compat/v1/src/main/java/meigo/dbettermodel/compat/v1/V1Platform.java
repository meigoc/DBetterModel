/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v1;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.script.AnimationScript;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import kr.toxicity.model.api.tracker.Tracker;
import meigo.dbettermodel.compat.api.BmCapability;
import meigo.dbettermodel.compat.api.BmEventSink;
import meigo.dbettermodel.compat.api.BmModel;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.compat.api.BmPlayerLimbs;
import meigo.dbettermodel.compat.api.BmRunningAnimation;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Compat layer for BetterModel 1.15.x. Loaded reflectively by the bootstrap via
 * {@code Class.forName(...).getConstructor(Plugin.class)}.
 */
public final class V1Platform implements BmPlatform {

    /** Capability set per COMPAT-MATRIX §11 (v1 column). */
    private static final Set<BmCapability> CAPABILITIES = java.util.Collections.unmodifiableSet(EnumSet.of(
            BmCapability.ANIMATION_SIGNALS,
            BmCapability.PLAYER_ANIMATION_EVENTS,
            BmCapability.DUMMY_TRACKERS,
            BmCapability.PER_PLAYER_ANIMATION,
            BmCapability.HITBOX_DAMAGE_EVENTS,
            BmCapability.SPAWN_EVENTS,
            BmCapability.SKIN_API,
            BmCapability.MOUNT_EVENTS,
            BmCapability.HITBOX_INTERACT_EVENT,
            BmCapability.HITBOX_INTERACT_AT_EVENT,
            BmCapability.BONE_FILTER_ANIMATE,
            BmCapability.PLAYER_HEAD_ITEM
            // no ANIMATION_LIFECYCLE_EVENTS: 1.x has no per-player animation start/end events
    ));

    private final Plugin ownPlugin;
    private final V1PlayerLimbs playerLimbs;
    // One state (rotation holder + last item) per live RenderedBone — see V1BoneState.
    private final Map<RenderedBone, V1BoneState> boneStates = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    // Animations reach the pipeline only on the async tracker tick; same-tick tag reads
    // would see nothing, so starts made through this layer are remembered per tracker.
    private final Map<Tracker, BmRunningAnimation> startedAnimations = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    // Live dummy trackers per world — BM never auto-spawns them (see summon()).
    private final Map<DummyTracker, UUID> dummies = new ConcurrentHashMap<>();
    private Listener dummySpawnListener;
    private V1Listener listener;
    private boolean signalBuilderRegistered;
    private volatile BmEventSink activeSink;

    public V1Platform(Plugin ownPlugin) {
        this.ownPlugin = ownPlugin;
        this.playerLimbs = new V1PlayerLimbs(this, ownPlugin);
    }

    V1BoneState boneState(RenderedBone bone) {
        return boneStates.computeIfAbsent(bone, V1BoneState::new);
    }

    void markStarted(Tracker tracker, String animation, String typeName) {
        startedAnimations.put(tracker, new BmRunningAnimation(animation, typeName));
    }

    void clearStarted(Tracker tracker, String animation) {
        startedAnimations.computeIfPresent(tracker, (t, a) -> a.name().equals(animation) ? null : a);
    }

    Optional<BmRunningAnimation> startedAnimation(Tracker tracker) {
        return Optional.ofNullable(startedAnimations.get(tracker));
    }

    @Override
    public String bmVersion() {
        return BetterModel.plugin().getDescription().getVersion();
    }

    @Override
    public Set<BmCapability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public Collection<String> modelNames() {
        return BetterModel.modelKeys();
    }

    @Override
    public Collection<String> limbNames() {
        return BetterModel.limbKeys();
    }

    @Override
    public Optional<BmModel> model(String name) {
        return BetterModel.model(name).map(V1Model::new);
    }

    @Override
    public Optional<BmModel> limb(String name) {
        return BetterModel.limb(name).map(V1Model::new);
    }

    @Override
    public boolean isModeled(Entity entity) {
        return BetterModel.registry(entity).isPresent();
    }

    @Override
    public Optional<BmTracker> tracker(Entity entity, String model) {
        return BetterModel.registry(entity)
                .map(registry -> registry.tracker(model))
                .map(tracker -> new V1Tracker(this, tracker));
    }

    @Override
    public Optional<BmTracker> tracker(UUID entityId, String model) {
        return BetterModel.registry(entityId)
                .map(registry -> registry.tracker(model))
                .map(tracker -> new V1Tracker(this, tracker));
    }

    @Override
    public List<BmTracker> trackers(Entity entity) {
        return BetterModel.registry(entity)
                .map(this::wrapTrackers)
                .orElseGet(List::of);
    }

    @Override
    public List<BmTracker> trackers(UUID entityId) {
        return BetterModel.registry(entityId)
                .map(this::wrapTrackers)
                .orElseGet(List::of);
    }

    private List<BmTracker> wrapTrackers(EntityTrackerRegistry registry) {
        return registry.trackers().stream()
                .map(tracker -> (BmTracker) new V1Tracker(this, (EntityTracker) tracker))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BmTracker> attach(Entity entity, String model) {
        ModelRenderer renderer = BetterModel.plugin().modelManager().model(model);
        if (renderer == null) return Optional.empty();
        return Optional.of(new V1Tracker(this, renderer.create(entity)));
    }

    @Override
    public boolean removeModel(Entity entity, String model) {
        return BetterModel.registry(entity)
                .map(registry -> registry.remove(model))
                .orElse(false);
    }

    @Override
    public Optional<BmTracker> summon(Location location, String model) {
        return BetterModel.model(model).map(renderer -> {
            DummyTracker tracker = renderer.create(location);
            showDummy(tracker, location.getWorld());
            return new V1Tracker(this, tracker);
        });
    }

    // BM never auto-spawns dummy trackers (its own test command calls tracker.spawn(player)
    // by hand), so spawn for the whole world now and keep late joiners covered via listener.
    private void showDummy(DummyTracker tracker, World world) {
        dummies.put(tracker, world.getUID());
        tracker.handleCloseEvent((t, r) -> dummies.remove(tracker));
        for (Player player : world.getPlayers()) {
            tracker.spawn(player);
        }
        if (dummySpawnListener == null) {
            dummySpawnListener = new Listener() {
                @EventHandler
                public void onJoin(PlayerJoinEvent event) {
                    spawnDummiesFor(event.getPlayer());
                }

                @EventHandler
                public void onWorldChange(PlayerChangedWorldEvent event) {
                    spawnDummiesFor(event.getPlayer());
                }
            };
            Bukkit.getPluginManager().registerEvents(dummySpawnListener, ownPlugin);
        }
    }

    private void spawnDummiesFor(Player player) {
        UUID worldId = player.getWorld().getUID();
        dummies.forEach((tracker, world) -> {
            if (world.equals(worldId) && !tracker.isSpawned(player.getUniqueId())) {
                tracker.spawn(player);
            }
        });
    }

    @Override
    public BmPlayerLimbs playerLimbs() {
        return playerLimbs;
    }

    @Override
    public void registerEvents(BmEventSink sink) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        activeSink = sink;
        listener = new V1Listener(this, sink);
        Bukkit.getPluginManager().registerEvents(listener, ownPlugin);
        // Blockbench instruction keyframes "denizen:signal{k=v;...}" -> animationSignal(tracker, signal, meta, null).
        // Compiled AnimationScripts outlive this platform (BM caches them per blueprint), so the
        // builder is registered once and routes through the mutable sink field, nulled on shutdown.
        if (!signalBuilderRegistered) {
            signalBuilderRegistered = true;
            BetterModel.plugin().scriptManager().addBuilder("denizen", data -> {
                String signal = data.args() == null ? "" : data.args();
                Map<String, String> metadata = data.metadata().toMap();
                return AnimationScript.of(true, tracker -> {
                    BmEventSink current = activeSink;
                    if (current != null) current.animationSignal(new V1Tracker(this, tracker), signal, metadata, null);
                });
            });
        }
    }

    @Override
    public void shutdown() {
        activeSink = null;
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (dummySpawnListener != null) {
            HandlerList.unregisterAll(dummySpawnListener);
            dummySpawnListener = null;
        }
        dummies.clear();
        startedAnimations.clear();
        boneStates.clear();
    }
}
