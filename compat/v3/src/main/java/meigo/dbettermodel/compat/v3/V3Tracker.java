/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v3;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationOverrideState;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import meigo.dbettermodel.compat.api.BmAnimationOptions;
import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.compat.api.BmModel;
import meigo.dbettermodel.compat.api.BmRunningAnimation;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class V3Tracker implements BmTracker {

    private final V3Platform platform;
    private final Tracker tracker;

    V3Tracker(V3Platform platform, Tracker tracker) {
        this.platform = platform;
        this.tracker = tracker;
    }

    Tracker handle() {
        return tracker;
    }

    // Wrappers are recreated per lookup; identity follows the underlying BM tracker.
    @Override
    public boolean equals(Object o) {
        return o instanceof V3Tracker other && tracker == other.tracker;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(tracker);
    }

    @Override
    public String name() {
        return tracker.name();
    }

    @Override
    public UUID entityUuid() {
        return tracker instanceof EntityTracker entityTracker ? entityTracker.registry().uuid() : null;
    }

    @Override
    public BmModel model() {
        return new V3Model(tracker.renderer());
    }

    private static Predicate<RenderedBone> filterOf(Set<String> bones) {
        if (bones == null) return bone -> true;
        return bone -> bones.contains(bone.name().name());
    }

    static AnimationIterator.Type typeOf(BmAnimationOptions.LoopMode mode) {
        return switch (mode) {
            case LOOP -> AnimationIterator.Type.LOOP;
            case HOLD -> AnimationIterator.Type.HOLD_ON_LAST;
            default -> AnimationIterator.Type.PLAY_ONCE;
        };
    }

    static AnimationModifier.Builder modifierOf(BmAnimationOptions options) {
        AnimationModifier.Builder builder = AnimationModifier.builder()
                .start(options.lerpTicks())
                // 3.2.0 bug: toBuilder()/mergeNotDefault() lose priority — set it explicitly.
                .priority(0)
                .type(typeOf(options.loopMode()))
                .speed(options.speed());
        if (options.override() != null) {
            builder.override(options.override());
        }
        return builder;
    }

    @Override
    public boolean animate(String animation, BmAnimationOptions options) {
        List<Player> players = options.players();
        boolean started;
        if (players == null || players.isEmpty()) {
            started = animateOnce(animation, options.bones(), modifierOf(options).build());
        } else {
            // Per-player playback: one modifier per viewer (matrix §4.5).
            started = false;
            for (Player player : players) {
                started |= animateOnce(animation, options.bones(),
                        modifierOf(options).player(BukkitAdapter.adapt(player)).build());
            }
        }
        if (started) {
            // The pipeline only reports the animation after its first async tick.
            platform.markStarted(tracker, animation, typeOf(options.loopMode()).name());
        }
        return started;
    }

    private boolean animateOnce(String animation, Set<String> bones, AnimationModifier modifier) {
        if (bones == null) {
            return tracker.animate(animation, modifier);
        }
        // The 1.x bone-filter animate overloads were removed in 2.2.0; emulate via
        // per-bone addAnimation like the reference 3.x integrations do.
        BlueprintAnimation blueprint = tracker.renderer().animation(animation).orElse(null);
        if (blueprint == null) return false;
        boolean any = false;
        for (RenderedBone bone : tracker.bones()) {
            if (bones.contains(bone.name().name())) {
                any |= bone.addAnimation(AnimationOverrideState.MATCHED, blueprint, modifier, () -> {});
            }
        }
        if (any) tracker.forceUpdate(true);
        return any;
    }

    @Override
    public boolean stopAnimation(String animation, Set<String> bones) {
        platform.clearStarted(tracker, animation);
        return tracker.stopAnimation(filterOf(bones), animation);
    }

    @Override
    public Optional<BmRunningAnimation> runningAnimation() {
        var running = tracker.getPipeline().runningAnimation();
        if (running != null) {
            return Optional.of(new BmRunningAnimation(running.name(), running.type().name()));
        }
        // animate() only queues; the pipeline reports it after the first async tick.
        return platform.startedAnimation(tracker);
    }

    @Override
    public void forceUpdate() {
        tracker.forceUpdate(true);
    }

    @Override
    public void close() {
        tracker.close();
    }

    @Override
    public List<Player> viewers() {
        // Tracker.viewedPlayer() removed in 2.2+; pipeline yields channel handlers (matrix §9).
        return tracker.getPipeline().viewedPlayer()
                .map(handler -> Bukkit.getPlayer(handler.uuid()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void hide(Player player) {
        tracker.hide(BukkitAdapter.adapt(player));
    }

    @Override
    public void show(Player player) {
        tracker.show(BukkitAdapter.adapt(player));
    }

    @Override
    public List<BmBone> bones() {
        return tracker.bones().stream()
                .map(bone -> (BmBone) new V3Bone(tracker, bone, platform.boneState(bone)))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BmBone> bone(String name) {
        return Optional.ofNullable(tracker.bone(name))
                .map(bone -> new V3Bone(tracker, bone, platform.boneState(bone)));
    }
}
