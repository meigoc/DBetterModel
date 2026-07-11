/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v3;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.BoneRenderContext;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.manager.SkinManager;
import kr.toxicity.model.api.player.PlayerLimb;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.EntityTracker;
import meigo.dbettermodel.compat.api.BmAnimationOptions;
import meigo.dbettermodel.compat.api.BmPlayerLimbs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class V3PlayerLimbs implements BmPlayerLimbs {

    private final V3Platform platform;
    private final Plugin ownPlugin;

    V3PlayerLimbs(V3Platform platform, Plugin ownPlugin) {
        this.platform = platform;
        this.ownPlugin = ownPlugin;
    }

    @Override
    public boolean playLimbAnimation(Player player, String limbModel, String animation, BmAnimationOptions options) {
        boolean started = BetterModel.platform().modelManager().animate(BukkitAdapter.adapt(player), limbModel, animation,
                V3Tracker.modifierOf(options).build());
        if (started) {
            // The tracker registers synchronously but its pipeline only reports the animation
            // after the first async tick — record the start for same-tick <player.limb> reads.
            BetterModel.registry(player.getUniqueId())
                    .map(registry -> registry.tracker(limbModel))
                    .ifPresent(tracker -> platform.markStarted(tracker, animation,
                            V3Tracker.typeOf(options.loopMode()).name()));
        }
        return started;
    }

    @Override
    public Collection<String> skinPartNames() {
        return Arrays.stream(PlayerLimb.values()).map(limb -> limb.name().toLowerCase()).toList();
    }

    @Override
    public CompletableFuture<SkinPartResult> applySkinPart(Supplier<Optional<Entity>> target,
                                                           String modelName, String boneName,
                                                           String partName, Player source) {
        // Resolve via ModelProfile.of(player) — BetterModel's tracked player profile,
        // kept in sync with SkinsRestorer (SkinApplyEvent). Unlike of(uuid) this never
        // forces BM's HTTP profile supplier, which NPEs on offline/unknown UUIDs.
        ModelProfile.Uncompleted sourceProfile = ModelProfile.of(BukkitAdapter.adapt(source)).asUncompleted();
        SkinManager skinManager = BetterModel.platform().skinManager();
        CompletableFuture<SkinPartResult> future = new CompletableFuture<>();
        // Snapshot the entity before going async so the callback can land on its region
        // thread (Folia); the supplier is re-checked inside the scheduled task.
        Entity known = target.get().orElse(null);
        skinManager.complete(sourceProfile).thenAccept(skinData ->
                schedule(known, () -> future.complete(SkinPartResult.TARGET_GONE), () -> {
                    try {
                        Entity entity = target.get().orElse(null);
                        if (entity == null || !source.isOnline()) {
                            future.complete(SkinPartResult.TARGET_GONE);
                            return;
                        }
                        EntityTracker tracker = BetterModel.registry(entity.getUniqueId())
                                .map(registry -> registry.tracker(modelName))
                                .orElse(null);
                        if (tracker == null) {
                            future.complete(SkinPartResult.MODEL_NOT_FOUND);
                            return;
                        }
                        RenderedBone bone = tracker.bone(boneName);
                        if (bone == null || bone.getDisplay() == null) {
                            future.complete(SkinPartResult.BONE_NOT_FOUND);
                            return;
                        }
                        PlayerLimb limb;
                        try {
                            limb = PlayerLimb.valueOf(partName.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            future.complete(SkinPartResult.INVALID_PART);
                            return;
                        }
                        var adaptedSource = BetterModel.nms().adapt(BukkitAdapter.adapt(source));
                        BoneRenderContext context = new BoneRenderContext(RenderSource.of(adaptedSource), skinData);
                        bone.setItemMapper(limb.getItemMapper());
                        bone.updateItem(context);
                        tracker.forceUpdate(true);
                        future.complete(SkinPartResult.SUCCESS);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                })
        ).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });
        return future;
    }

    /**
     * Runs the task on the entity's region thread when the entity is known (with the retired
     * callback covering removal), or on the global region scheduler otherwise. On non-Folia
     * Paper both delegate to the main thread.
     */
    private void schedule(Entity known, Runnable retired, Runnable task) {
        if (known != null) {
            known.getScheduler().run(ownPlugin, scheduled -> task.run(), retired);
        } else {
            Bukkit.getGlobalRegionScheduler().execute(ownPlugin, task);
        }
    }
}
