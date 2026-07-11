/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.bukkit.event.BukkitEventApplication;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.event.ModelEventApplication;
import kr.toxicity.model.api.script.AnimationScript;
import kr.toxicity.model.api.tracker.DummyTracker;
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
import java.util.Collections;
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
 * Compat layer for BetterModel 2.x, compiled against 2.2.0. Loaded reflectively by the
 * bootstrap via {@code Class.forName(...).getConstructor(Plugin.class)}.
 *
 * <p>Intra-2.x drift (COMPAT-MATRIX §10) is handled by two constructor probes:
 * {@code event.hitbox.*} (since 2.1.0) and {@code TrackerAnimation}/{@code
 * AnimationOverrideState} (since 2.2.0). 2.2.0-only code paths live in dedicated classes
 * ({@link V2AnimationModern}, {@link V2HitboxModern}) that are never loaded on older
 * sub-lines, so no NoClassDefFoundError can escape.</p>
 */
public final class V2Platform implements BmPlatform {

    private final Plugin ownPlugin;
    private final V2PlayerLimbs playerLimbs;
    private final Set<BmCapability> capabilities;
    private final boolean hitboxEventPackage; // >= 2.1.0
    private final boolean modernAnimation;    // >= 2.2.0
    // One state (rotation holder + last item) per live RenderedBone — see V2BoneState.
    private final Map<RenderedBone, V2BoneState> boneStates = Collections.synchronizedMap(new WeakHashMap<>());
    // Animations reach the pipeline only on the async tracker tick; same-tick tag reads
    // would see nothing, so starts made through this layer are remembered per tracker.
    private final Map<Tracker, BmRunningAnimation> startedAnimations = Collections.synchronizedMap(new WeakHashMap<>());
    // Live dummy trackers per world — BM never auto-spawns them (see summon()).
    private final Map<DummyTracker, UUID> dummies = new ConcurrentHashMap<>();
    private Listener dummySpawnListener;
    private V2Listener listener;
    private boolean signalBuilderRegistered;
    private volatile BmEventSink activeSink;

    public V2Platform(Plugin ownPlugin) {
        this.ownPlugin = ownPlugin;
        this.playerLimbs = new V2PlayerLimbs(this, ownPlugin);
        this.hitboxEventPackage = probe("kr.toxicity.model.api.event.hitbox.HitBoxDamagedEvent");
        this.modernAnimation = probe("kr.toxicity.model.api.tracker.TrackerAnimation");
        // Capability set per COMPAT-MATRIX §11 (v2 column), reduced by sub-line.
        Set<BmCapability> caps = EnumSet.of(
                BmCapability.EVENT_BUS,
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
                BmCapability.PLAYER_HEAD_ITEM,
                // PlayerPerAnimationStart/EndEvent are @since 2.0.0 — whole 2.x line.
                BmCapability.ANIMATION_LIFECYCLE_EVENTS
        );
        if (modernAnimation) {
            caps.add(BmCapability.TRACKER_ANIMATION_API);
            caps.add(BmCapability.BONE_FILTER_ANIMATE); // emulated per bone, see V2AnimationModern
        } else {
            ownPlugin.getLogger().warning("BetterModel " + bmVersion() + " (2.x sub-line "
                    + (hitboxEventPackage ? "2.1.x" : "2.0.x")
                    + ") detected: bone-filtered animation and the TrackerAnimation API are unavailable; "
                    + "bone filters on animate will apply to the whole model. Update BetterModel to 2.2.0+ for full support.");
        }
        this.capabilities = Collections.unmodifiableSet(caps);
    }

    private static boolean probe(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    boolean hasModernAnimation() {
        return modernAnimation;
    }

    V2BoneState boneState(RenderedBone bone) {
        return boneStates.computeIfAbsent(bone, V2BoneState::new);
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
        Plugin bm = Bukkit.getPluginManager().getPlugin("BetterModel");
        return bm != null ? bm.getDescription().getVersion() : "unknown";
    }

    @Override
    public Set<BmCapability> capabilities() {
        return capabilities;
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
        return BetterModel.model(name).map(V2Model::new);
    }

    @Override
    public Optional<BmModel> limb(String name) {
        return BetterModel.limb(name).map(V2Model::new);
    }

    @Override
    public boolean isModeled(Entity entity) {
        return BetterModel.registry(BukkitAdapter.adapt(entity)).isPresent();
    }

    @Override
    public Optional<BmTracker> tracker(Entity entity, String model) {
        return BetterModel.registry(BukkitAdapter.adapt(entity))
                .map(registry -> registry.tracker(model))
                .map(tracker -> new V2Tracker(this, tracker));
    }

    @Override
    public Optional<BmTracker> tracker(UUID entityId, String model) {
        return BetterModel.registry(entityId)
                .map(registry -> registry.tracker(model))
                .map(tracker -> new V2Tracker(this, tracker));
    }

    @Override
    public List<BmTracker> trackers(Entity entity) {
        return BetterModel.registry(BukkitAdapter.adapt(entity))
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
                .map(tracker -> (BmTracker) new V2Tracker(this, tracker))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BmTracker> attach(Entity entity, String model) {
        ModelRenderer renderer = BetterModel.modelOrNull(model);
        if (renderer == null) return Optional.empty();
        return Optional.of(new V2Tracker(this, renderer.create(BukkitAdapter.adapt(entity))));
    }

    @Override
    public boolean removeModel(Entity entity, String model) {
        return BetterModel.registry(BukkitAdapter.adapt(entity))
                .map(registry -> registry.remove(model))
                .orElse(false);
    }

    @Override
    public Optional<BmTracker> summon(Location location, String model) {
        return BetterModel.model(model).map(renderer -> {
            DummyTracker tracker = renderer.create(BukkitAdapter.adapt(location));
            showDummy(tracker, location.getWorld());
            return new V2Tracker(this, tracker);
        });
    }

    // BM never auto-spawns dummy trackers (its own test command calls tracker.spawn(player)
    // by hand), so spawn for the whole world now and keep late joiners covered via listener.
    private void showDummy(DummyTracker tracker, World world) {
        dummies.put(tracker, world.getUID());
        tracker.handleCloseEvent((t, r) -> dummies.remove(tracker));
        for (Player player : world.getPlayers()) {
            tracker.spawn(BukkitAdapter.adapt(player));
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
                tracker.spawn(BukkitAdapter.adapt(player));
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
            listener.unregister();
        }
        activeSink = sink;
        ModelEventApplication application = BukkitEventApplication.of(ownPlugin);
        listener = new V2Listener(this, sink);
        listener.register(application, hitboxEventPackage);
        // Blockbench instruction keyframes "denizen:signal{k=v;...}" -> animationSignal(tracker, signal, meta, null).
        // BM has no removeBuilder, so register once and route through the mutable sink field.
        if (!signalBuilderRegistered) {
            signalBuilderRegistered = true;
            BetterModel.platform().scriptManager().addBuilder("denizen", data -> {
                String signal = data.args() == null ? "" : data.args();
                Map<String, String> metadata = data.metadata().toMap();
                return AnimationScript.of(true, tracker -> {
                    BmEventSink current = activeSink;
                    if (current != null) current.animationSignal(new V2Tracker(this, tracker), signal, metadata, null);
                });
            });
        }
    }

    @Override
    public void shutdown() {
        activeSink = null;
        if (listener != null) {
            listener.unregister();
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
