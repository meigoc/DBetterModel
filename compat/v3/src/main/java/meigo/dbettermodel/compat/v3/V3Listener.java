/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v3;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.bukkit.event.BukkitEventApplication;
import kr.toxicity.model.api.bukkit.platform.BukkitEntity;
import kr.toxicity.model.api.bukkit.platform.BukkitPlayer;
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
import kr.toxicity.model.api.event.hitbox.HitBoxDamagedEvent;
import kr.toxicity.model.api.event.hitbox.HitBoxInteractAtEvent;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import meigo.dbettermodel.compat.api.BmEventSink;
import meigo.dbettermodel.compat.api.BmReloadResult;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bridges the 3.x BetterModel event bus to the version-neutral {@link BmEventSink}.
 * Every subscription is unregistered in {@link #unsubscribeAll()}.
 */
final class V3Listener {

    private final V3Platform platform;
    private final BmEventSink sink;
    private final List<ModelEventListener> subscriptions = new ArrayList<>();

    V3Listener(V3Platform platform, BmEventSink sink) {
        this.platform = platform;
        this.sink = sink;
    }

    void subscribeAll(Plugin ownPlugin) {
        ModelEventApplication application = BukkitEventApplication.of(ownPlugin);

        subscribe(application, PluginStartReloadEvent.class, event -> sink.reloadStart());
        subscribe(application, PluginEndReloadEvent.class, event -> sink.reloadEnd(mapReload(event.result())));
        subscribe(application, CreateEntityTrackerEvent.class, event -> sink.trackerCreated(wrap(event.tracker())));
        subscribe(application, CreateDummyTrackerEvent.class, event -> sink.trackerCreated(wrap(event.tracker())));
        subscribe(application, CloseTrackerEvent.class, event -> sink.trackerClosed(wrap(event.tracker())));
        // Per-player limb signal; keyframe "denizen:..." signals come via the script builder.
        // The bus record carries no tracker, so per-player signals have no model context.
        subscribe(application, AnimationSignalEvent.class, event ->
                sink.animationSignal(null, event.signal(), Map.of(), bukkit(event.player())));
        subscribe(application, PlayerPerAnimationStartEvent.class, event ->
                sink.animationStarted(wrap(event.tracker()), bukkit(event.player())));
        subscribe(application, PlayerPerAnimationEndEvent.class, event ->
                sink.animationEnded(wrap(event.tracker()), bukkit(event.player())));
        subscribe(application, ModelSpawnAtPlayerEvent.class, event ->
                sink.modelSpawnAtPlayer(wrap(event.getTracker()), bukkit(event.getPlayer())));
        subscribe(application, ModelDespawnAtPlayerEvent.class, event ->
                sink.modelDespawnAtPlayer(wrap(event.tracker()), bukkit(event.player())));
        subscribe(application, HitBoxDamagedEvent.class, event ->
                sink.hitboxDamaged(trackerOf(event.getHitBox()), event.getDamage()));
        // HitBoxInteractEvent was removed in 3.0.2 — only the -At variant exists on 3.2.0.
        subscribe(application, HitBoxInteractAtEvent.class, event ->
                sink.hitboxInteracted(trackerOf(event.getHitBox()), bukkit(event.getWho()), event.getHand().name()));
        subscribe(application, MountModelEvent.class, event -> {
            EntityTracker tracker = event.tracker();
            sink.mounted(wrap(tracker),
                    new V3Bone(tracker, event.bone(), platform.boneState(event.bone())),
                    bukkitEntity(event.entity()));
        });
        subscribe(application, DismountModelEvent.class, event -> {
            EntityTracker tracker = event.tracker();
            sink.dismounted(wrap(tracker),
                    new V3Bone(tracker, event.bone(), platform.boneState(event.bone())),
                    bukkitEntity(event.entity()));
        });
    }

    private <T extends ModelEvent> void subscribe(ModelEventApplication application, Class<T> eventClass, Consumer<T> consumer) {
        subscriptions.add(BetterModel.eventBus().subscribe(application, eventClass, consumer));
    }

    void unsubscribeAll() {
        subscriptions.forEach(ModelEventListener::unregister);
        subscriptions.clear();
    }

    static Player bukkit(PlatformPlayer player) {
        return player instanceof BukkitPlayer bukkitPlayer ? bukkitPlayer.source() : Bukkit.getPlayer(player.uuid());
    }

    private static Entity bukkitEntity(PlatformEntity entity) {
        return entity instanceof BukkitEntity bukkitEntity ? bukkitEntity.source() : Bukkit.getEntity(entity.uuid());
    }

    private static BmReloadResult mapReload(BetterModelPlatform.ReloadResult result) {
        if (result instanceof BetterModelPlatform.ReloadResult.Success) return BmReloadResult.SUCCESS;
        if (result instanceof BetterModelPlatform.ReloadResult.Failure) return BmReloadResult.FAILURE;
        if (result instanceof BetterModelPlatform.ReloadResult.OnReload) return BmReloadResult.ON_RELOAD;
        return BmReloadResult.UNKNOWN;
    }

    private BmTracker wrap(Tracker tracker) {
        return tracker == null ? null : new V3Tracker(platform, tracker);
    }

    private BmTracker trackerOf(HitBox hitBox) {
        var bone = hitBox.positionSource();
        return hitBox.registry()
                .map(registry -> {
                    for (EntityTracker tracker : registry.trackers()) {
                        if (tracker.bones().contains(bone)) return (BmTracker) new V3Tracker(platform, tracker);
                    }
                    return wrap(registry.first());
                })
                .orElse(null);
    }
}
