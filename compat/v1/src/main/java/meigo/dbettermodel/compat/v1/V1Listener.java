/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v1;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.event.AnimationSignalEvent;
import kr.toxicity.model.api.event.CloseTrackerEvent;
import kr.toxicity.model.api.event.CreateDummyTrackerEvent;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;
import kr.toxicity.model.api.event.DismountModelEvent;
import kr.toxicity.model.api.event.ModelDamagedEvent;
import kr.toxicity.model.api.event.ModelDespawnAtPlayerEvent;
import kr.toxicity.model.api.event.ModelInteractEvent;
import kr.toxicity.model.api.event.ModelSpawnAtPlayerEvent;
import kr.toxicity.model.api.event.MountModelEvent;
import kr.toxicity.model.api.event.PlayerPerAnimationEndEvent;
import kr.toxicity.model.api.event.PlayerPerAnimationStartEvent;
import kr.toxicity.model.api.event.PluginEndReloadEvent;
import kr.toxicity.model.api.event.PluginStartReloadEvent;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import meigo.dbettermodel.compat.api.BmEventSink;
import meigo.dbettermodel.compat.api.BmReloadResult;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * Bridges 1.x Bukkit events to the version-neutral {@link BmEventSink}.
 */
final class V1Listener implements Listener {

    private final V1Platform platform;
    private final BmEventSink sink;

    V1Listener(V1Platform platform, BmEventSink sink) {
        this.platform = platform;
        this.sink = sink;
    }

    private BmTracker wrap(Tracker tracker) {
        return tracker == null ? null : new V1Tracker(platform, tracker);
    }

    private BmTracker trackerOf(HitBox hitBox) {
        var bone = hitBox.positionSource();
        return hitBox.registry()
                .map(registry -> {
                    for (EntityTracker tracker : registry.trackers()) {
                        if (tracker.bones().contains(bone)) return (BmTracker) new V1Tracker(platform, tracker);
                    }
                    return wrap(registry.first());
                })
                .orElse(null);
    }

    @EventHandler
    public void onReloadStart(PluginStartReloadEvent event) {
        sink.reloadStart();
    }

    @EventHandler
    public void onReloadEnd(PluginEndReloadEvent event) {
        var result = event.getResult();
        BmReloadResult mapped;
        if (result instanceof kr.toxicity.model.api.BetterModelPlugin.ReloadResult.Success) mapped = BmReloadResult.SUCCESS;
        else if (result instanceof kr.toxicity.model.api.BetterModelPlugin.ReloadResult.Failure) mapped = BmReloadResult.FAILURE;
        else if (result instanceof kr.toxicity.model.api.BetterModelPlugin.ReloadResult.OnReload) mapped = BmReloadResult.ON_RELOAD;
        else mapped = BmReloadResult.UNKNOWN;
        sink.reloadEnd(mapped);
    }

    @EventHandler
    public void onTrackerCreate(CreateEntityTrackerEvent event) {
        sink.trackerCreated(wrap(event.tracker()));
    }

    @EventHandler
    public void onDummyTrackerCreate(CreateDummyTrackerEvent event) {
        sink.trackerCreated(wrap(event.tracker()));
    }

    @EventHandler
    public void onTrackerClose(CloseTrackerEvent event) {
        sink.trackerClosed(wrap(event.getTracker()));
    }

    @EventHandler
    public void onAnimationSignal(AnimationSignalEvent event) {
        // The 1.x Bukkit event carries no tracker, so there is no model context here.
        sink.animationSignal(null, event.signal(), Map.of(), event.getPlayer());
    }

    @EventHandler
    public void onPerAnimationStart(PlayerPerAnimationStartEvent event) {
        sink.animationStarted(wrap(event.tracker()), event.getPlayer());
    }

    @EventHandler
    public void onPerAnimationEnd(PlayerPerAnimationEndEvent event) {
        sink.animationEnded(wrap(event.tracker()), event.getPlayer());
    }

    @EventHandler
    public void onModelSpawnAtPlayer(ModelSpawnAtPlayerEvent event) {
        sink.modelSpawnAtPlayer(wrap(event.getTracker()), event.getPlayer());
    }

    @EventHandler
    public void onModelDespawnAtPlayer(ModelDespawnAtPlayerEvent event) {
        sink.modelDespawnAtPlayer(wrap(event.getTracker()), event.getPlayer());
    }

    @EventHandler
    public void onModelDamaged(ModelDamagedEvent event) {
        sink.hitboxDamaged(trackerOf(event.getHitBox()), event.getDamage());
    }

    @EventHandler
    public void onModelInteract(ModelInteractEvent event) {
        sink.hitboxInteracted(trackerOf(event.getHitBox()), event.getPlayer(), event.getHand().name());
    }

    @EventHandler
    public void onMount(MountModelEvent event) {
        EntityTracker tracker = event.tracker();
        sink.mounted(wrap(tracker), new V1Bone(tracker, event.bone(), platform.boneState(event.bone())), event.entity());
    }

    @EventHandler
    public void onDismount(DismountModelEvent event) {
        EntityTracker tracker = event.tracker();
        sink.dismounted(wrap(tracker), new V1Bone(tracker, event.bone(), platform.boneState(event.bone())), event.entity());
    }
}
