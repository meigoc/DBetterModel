/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.services;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ListTag;
import meigo.dbettermodel.compat.api.BmAnimationOptions;
import meigo.dbettermodel.compat.api.BmEventSink;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.compat.api.BmReloadResult;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.denizen.events.BMAnimationEndEvent;
import meigo.dbettermodel.denizen.events.BMAnimationSignalEvent;
import meigo.dbettermodel.denizen.events.BMAnimationStartEvent;
import meigo.dbettermodel.denizen.events.BMHitboxDamagedEvent;
import meigo.dbettermodel.denizen.events.BMHitboxInteractedEvent;
import meigo.dbettermodel.denizen.events.BMModelDespawnEvent;
import meigo.dbettermodel.denizen.events.BMModelDismountedEvent;
import meigo.dbettermodel.denizen.events.BMModelMountedEvent;
import meigo.dbettermodel.denizen.events.BMModelSpawnEvent;
import meigo.dbettermodel.denizen.events.BMPlayerAnimationSignalEvent;
import meigo.dbettermodel.denizen.events.BMReloadEndEvent;
import meigo.dbettermodel.denizen.events.BMReloadStartEvent;
import meigo.dbettermodel.denizen.events.BMTrackerClosedEvent;
import meigo.dbettermodel.denizen.events.BMTrackerCreatedEvent;
import meigo.dbettermodel.denizen.objects.BMBoneTag;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ModelService {

    private static final ModelService INSTANCE = new ModelService();
    private final Map<String, BoneController> boneControllerCache = new ConcurrentHashMap<>();
    private final BoneMechanismHandler mechanismHandler = new BoneMechanismHandler();
    // Dummy (location-bound) trackers have no entity UUID, so BMModelTag identity for them
    // is 'bmmodel@dummy:<id>,<model>' backed by this registry. Cleared on BM reload/shutdown.
    private final Map<String, BmTracker> dummyTrackers = new ConcurrentHashMap<>();
    private final AtomicLong dummyIdCounter = new AtomicLong();
    private BmPlatform platform;
    private volatile JavaPlugin plugin;

    private ModelService() {}

    public static ModelService getInstance() {
        return INSTANCE;
    }

    public void initialize(JavaPlugin plugin, BmPlatform platform) {
        this.plugin = plugin;
        this.platform = platform;
        platform.registerEvents(new Sink());
    }

    public void shutdown() {
        boneControllerCache.clear();
        dummyTrackers.clear();
        platform = null;
        plugin = null;
    }

    /**
     * Denizen's ScriptEvent.fire() runs script queues on the calling thread with no guard,
     * and BetterModel delivers bus/reload/signal callbacks off-main (async reload, tracker
     * tick executor) — so every Denizen-facing dispatch is marshaled to the main thread.
     * The global region scheduler is Paper's main thread on non-Folia servers, and the
     * global tick thread on Folia (where isPrimaryThread() reports the global tick thread).
     */
    private void onMain(Runnable task) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        JavaPlugin plugin = this.plugin;
        if (plugin == null || !plugin.isEnabled()) return;
        org.bukkit.Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    // --- Dummy tracker registry ---

    /** Registry id of the dummy tracker handle, assigning one if new (used by BMModelTag identity). */
    public String registerDummy(BmTracker tracker) {
        // Wrappers are recreated per lookup, so compare by equals (underlying BM tracker).
        for (Map.Entry<String, BmTracker> entry : dummyTrackers.entrySet()) {
            if (entry.getValue().equals(tracker)) {
                return entry.getKey();
            }
        }
        String id = Long.toString(dummyIdCounter.incrementAndGet());
        dummyTrackers.put(id, tracker);
        return id;
    }

    public BmTracker dummyTracker(String id) {
        return dummyTrackers.get(id);
    }

    public void unregisterDummy(BmTracker tracker) {
        dummyTrackers.values().removeIf(t -> t.equals(tracker));
    }

    /** Core-side sink: cache invalidation + the full Denizen event surface. */
    private class Sink implements BmEventSink {
        @Override
        public void trackerCreated(BmTracker tracker) {
            onMain(() -> BMTrackerCreatedEvent.handle(tracker));
        }

        @Override
        public void trackerClosed(BmTracker tracker) {
            UUID uuid = tracker.entityUuid();
            if (uuid != null) {
                String prefix = uuid + "," + tracker.name() + ",";
                boneControllerCache.keySet().removeIf(key -> key.startsWith(prefix));
            } else {
                unregisterDummy(tracker);
            }
            onMain(() -> BMTrackerClosedEvent.handle(tracker));
        }

        @Override
        public void reloadStart() {
            // BM reload closes every tracker; dummy handles cannot survive it.
            dummyTrackers.clear();
            onMain(BMReloadStartEvent::handle);
        }

        @Override
        public void reloadEnd(BmReloadResult result) {
            onMain(() -> BMReloadEndEvent.handle(result));
        }

        @Override
        public void animationSignal(BmTracker tracker, String signal, Map<String, String> metadata, Player player) {
            onMain(() -> {
                BMAnimationSignalEvent.handle(tracker, signal, metadata, player);
                if (player != null) {
                    BMPlayerAnimationSignalEvent.handle(tracker, signal, metadata, player);
                }
            });
        }

        @Override
        public void animationStarted(BmTracker tracker, Player player) {
            onMain(() -> BMAnimationStartEvent.handle(tracker, player));
        }

        @Override
        public void animationEnded(BmTracker tracker, Player player) {
            onMain(() -> BMAnimationEndEvent.handle(tracker, player));
        }

        @Override
        public void modelSpawnAtPlayer(BmTracker tracker, Player player) {
            onMain(() -> BMModelSpawnEvent.handle(tracker, player));
        }

        @Override
        public void modelDespawnAtPlayer(BmTracker tracker, Player player) {
            onMain(() -> BMModelDespawnEvent.handle(tracker, player));
        }

        @Override
        public void hitboxInteracted(BmTracker tracker, Player player, String hand) {
            onMain(() -> BMHitboxInteractedEvent.handle(tracker, player, hand));
        }

        @Override
        public void hitboxDamaged(BmTracker tracker, double damage) {
            onMain(() -> BMHitboxDamagedEvent.handle(tracker, damage));
        }

        @Override
        public void mounted(BmTracker tracker, BmBone bone, Entity passenger) {
            onMain(() -> BMModelMountedEvent.handle(tracker, bone, passenger));
        }

        @Override
        public void dismounted(BmTracker tracker, BmBone bone, Entity passenger) {
            onMain(() -> BMModelDismountedEvent.handle(tracker, bone, passenger));
        }
    }

    private Optional<BoneController> getBoneController(UUID entityUUID, String modelId, String boneId) {
        if (platform == null) return Optional.empty();
        return platform.tracker(entityUUID, modelId)
                .flatMap(tracker -> tracker.bone(boneId)
                        .map(bone -> {
                            String key = entityUUID + "," + modelId + "," + boneId;
                            return boneControllerCache.computeIfAbsent(key, k -> new BoneController(tracker, bone));
                        }));
    }

    // --- Tag Getters ---
    public Location getBoneWorldLocation(UUID entityUUID, String modelId, String boneId) {
        return getBoneController(entityUUID, modelId, boneId)
                .map(BoneController::getWorldLocation)
                .orElse(null);
    }

    public Vector3f getBoneWorldRotationEuler(UUID entityUUID, String modelId, String boneId) {
        return getBoneController(entityUUID, modelId, boneId)
                .map(BoneController::getWorldRotationEuler)
                .orElse(null);
    }

    public boolean isBoneVisible(UUID entityUUID, String modelId, String boneId) {
        return getBoneController(entityUUID, modelId, boneId)
                .map(BoneController::isVisible)
                .orElse(false);
    }

    // --- Mechanism Handlers ---
    public void adjustBone(BMBoneTag boneTag, Mechanism mechanism) {
        getBoneController(boneTag.getEntityUUID(), boneTag.getModelName(), boneTag.getBoneName())
                .ifPresent(controller -> {
                    // fulfill exactly once, and only when actually handled (5.x tech-debt item 4)
                    if (mechanismHandler.handle(controller, mechanism)) {
                        mechanism.fulfill();
                    }
                });
    }

    public boolean isBoneMechanism(String name) {
        return mechanismHandler.isBoneMechanism(name);
    }

    /**
     * Applies a bone mechanism to every bone of the tracker WITHOUT fulfilling —
     * the caller (BMModelTag.adjust) fulfills once.
     *
     * @return true if the mechanism was applied to at least one bone
     */
    public boolean adjustAllBones(BmTracker tracker, Mechanism mechanism) {
        UUID uuid = tracker.entityUuid();
        if (uuid == null) return false;
        boolean any = false;
        for (var bone : tracker.bones()) {
            Optional<BoneController> controller = getBoneController(uuid, tracker.name(), bone.name());
            if (controller.isPresent() && mechanismHandler.handle(controller.get(), mechanism)) {
                any = true;
            }
        }
        return any;
    }

    // --- Command Logic ---
    public void mountEntity(Entity entityToMount, BMBoneTag boneTag) {
        getBoneController(boneTag.getEntityUUID(), boneTag.getModelName(), boneTag.getBoneName())
                .ifPresent(controller -> controller.mount(entityToMount));
    }

    public void dismountEntity(Entity entityToDismount, BMBoneTag boneTag) {
        getBoneController(boneTag.getEntityUUID(), boneTag.getModelName(), boneTag.getBoneName())
                .ifPresent(controller -> controller.dismount(entityToDismount));
    }

    public void dismountAll(BMBoneTag boneTag) {
        getBoneController(boneTag.getEntityUUID(), boneTag.getModelName(), boneTag.getBoneName())
                .ifPresent(BoneController::dismountAll);
    }

    public void playAnimationForPlayers(BmTracker tracker, String animation, BmAnimationOptions.Builder options, ListTag players) {
        if (players != null && !players.isEmpty()) {
            List<Player> resolved = players
                    .filter(PlayerTag.class, DenizenCore.implementation.getTagContext((com.denizenscript.denizencore.scripts.ScriptEntry) null))
                    .stream()
                    .map(PlayerTag::getPlayerEntity)
                    .toList();
            options.players(resolved);
        }
        tracker.animate(animation, options.build());
    }
}
