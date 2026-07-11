/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.event.AnimationSignalEvent;
import kr.toxicity.model.api.event.CloseTrackerEvent;
import kr.toxicity.model.api.event.CreateDummyTrackerEvent;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;
import kr.toxicity.model.api.event.DismountModelEvent;
import kr.toxicity.model.api.event.ModelDespawnAtPlayerEvent;
import kr.toxicity.model.api.event.ModelEvent;
import kr.toxicity.model.api.event.ModelEventApplication;
import kr.toxicity.model.api.event.ModelEventListener;
import kr.toxicity.model.api.event.ModelSpawnAtPlayerEvent;
import kr.toxicity.model.api.event.MountModelEvent;
import kr.toxicity.model.api.event.PlayerPerAnimationEndEvent;
import kr.toxicity.model.api.event.PlayerPerAnimationStartEvent;
import kr.toxicity.model.api.event.PluginEndReloadEvent;
import kr.toxicity.model.api.event.PluginStartReloadEvent;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import meigo.dbettermodel.compat.api.BmEventSink;
import meigo.dbettermodel.compat.api.BmReloadResult;
import meigo.dbettermodel.compat.api.BmTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bridges the 2.x event bus to the version-neutral {@link BmEventSink}. The hitbox event
 * family drifted inside the 2.x line, so it is wired by {@link V2HitboxModern} (>= 2.1.0)
 * or {@link V2HitboxLegacy} (2.0.x) — never here.
 */
final class V2Listener {

    private final V2Platform platform;
    private final BmEventSink sink;
    private final List<ModelEventListener> subscriptions = new ArrayList<>();

    V2Listener(V2Platform platform, BmEventSink sink) {
        this.platform = platform;
        this.sink = sink;
    }

    private BmTracker wrap(Tracker tracker) {
        return tracker == null ? null : new V2Tracker(platform, tracker);
    }

    private <T extends ModelEvent> void subscribe(ModelEventApplication application, Class<T> type, Consumer<T> consumer) {
        subscriptions.add(BetterModel.eventBus().subscribe(application, type, consumer));
    }

    void register(ModelEventApplication application, boolean hasHitboxEventPackage) {
        subscribe(application, PluginStartReloadEvent.class, event -> sink.reloadStart());
        subscribe(application, PluginEndReloadEvent.class, event -> sink.reloadEnd(mapReload(event.result())));
        subscribe(application, CreateEntityTrackerEvent.class, event -> sink.trackerCreated(wrap(event.tracker())));
        subscribe(application, CreateDummyTrackerEvent.class, event -> sink.trackerCreated(wrap(event.tracker())));
        subscribe(application, CloseTrackerEvent.class, event -> sink.trackerClosed(wrap(event.tracker())));
        // The bus record carries no tracker, so per-player signals have no model context.
        subscribe(application, AnimationSignalEvent.class, event ->
                sink.animationSignal(null, event.signal(), Map.of(), V2Adapters.bukkit(event.player())));
        subscribe(application, PlayerPerAnimationStartEvent.class, event ->
                sink.animationStarted(wrap(event.tracker()), V2Adapters.bukkit(event.player())));
        subscribe(application, PlayerPerAnimationEndEvent.class, event ->
                sink.animationEnded(wrap(event.tracker()), V2Adapters.bukkit(event.player())));
        subscribe(application, ModelSpawnAtPlayerEvent.class, event ->
                sink.modelSpawnAtPlayer(wrap(event.getTracker()), V2Adapters.bukkit(event.getPlayer())));
        subscribe(application, ModelDespawnAtPlayerEvent.class, event ->
                sink.modelDespawnAtPlayer(wrap(event.tracker()), V2Adapters.bukkit(event.player())));
        subscribe(application, MountModelEvent.class, event -> {
            EntityTracker tracker = event.tracker();
            sink.mounted(wrap(tracker), new V2Bone(tracker, event.bone(), platform.boneState(event.bone())),
                    V2Adapters.bukkit(event.entity()));
        });
        subscribe(application, DismountModelEvent.class, event -> {
            EntityTracker tracker = event.tracker();
            sink.dismounted(wrap(tracker), new V2Bone(tracker, event.bone(), platform.boneState(event.bone())),
                    V2Adapters.bukkit(event.entity()));
        });
        if (hasHitboxEventPackage) {
            V2HitboxModern.register(this, application);
        } else {
            V2HitboxLegacy.register(this, application);
        }
    }

    void unregister() {
        subscriptions.forEach(ModelEventListener::unregister);
        subscriptions.clear();
    }

    // --- shared plumbing for the hitbox sub-modules ---

    <T extends ModelEvent> void subscribeRaw(ModelEventApplication application, Class<T> type, Consumer<T> consumer) {
        subscribe(application, type, consumer);
    }

    void fireDamaged(HitBox hitBox, double damage) {
        sink.hitboxDamaged(trackerOf(hitBox), damage);
    }

    void fireInteracted(HitBox hitBox, PlatformPlayer player, String hand) {
        sink.hitboxInteracted(trackerOf(hitBox), V2Adapters.bukkit(player), hand);
    }

    private BmTracker trackerOf(HitBox hitBox) {
        var bone = hitBox.positionSource();
        return hitBox.registry()
                .map(registry -> {
                    for (EntityTracker tracker : registry.trackers()) {
                        if (tracker.bones().contains(bone)) return (BmTracker) new V2Tracker(platform, tracker);
                    }
                    return wrap(registry.first());
                })
                .orElse(null);
    }

    private static BmReloadResult mapReload(BetterModelPlatform.ReloadResult result) {
        if (result instanceof BetterModelPlatform.ReloadResult.Success) return BmReloadResult.SUCCESS;
        if (result instanceof BetterModelPlatform.ReloadResult.Failure) return BmReloadResult.FAILURE;
        if (result instanceof BetterModelPlatform.ReloadResult.OnReload) return BmReloadResult.ON_RELOAD;
        return BmReloadResult.UNKNOWN;
    }
}
