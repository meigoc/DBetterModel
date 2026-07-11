/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v1;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import meigo.dbettermodel.compat.api.BmAnimationOptions;
import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.compat.api.BmModel;
import meigo.dbettermodel.compat.api.BmRunningAnimation;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class V1Tracker implements BmTracker {

    private final V1Platform platform;
    private final Tracker tracker;

    V1Tracker(V1Platform platform, Tracker tracker) {
        this.platform = platform;
        this.tracker = tracker;
    }

    Tracker handle() {
        return tracker;
    }

    // Wrappers are recreated per lookup; identity follows the underlying BM tracker.
    @Override
    public boolean equals(Object o) {
        return o instanceof V1Tracker other && tracker == other.tracker;
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
        return new V1Model(tracker.renderer());
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
                .type(typeOf(options.loopMode()))
                .speed(options.speed());
        if (options.override() != null) {
            builder.override(options.override());
        }
        return builder;
    }

    @Override
    public boolean animate(String animation, BmAnimationOptions options) {
        Predicate<RenderedBone> filter = filterOf(options.bones());
        List<Player> players = options.players();
        boolean started;
        if (players == null || players.isEmpty()) {
            started = tracker.animate(filter, animation, modifierOf(options).build(), () -> {});
        } else {
            // Per-player playback: one modifier per viewer (the official 1.x per-player path).
            started = false;
            for (Player player : players) {
                started |= tracker.animate(filter, animation, modifierOf(options).player(player).build(), () -> {});
            }
        }
        if (started) {
            // The pipeline only reports the animation after its first async tick.
            platform.markStarted(tracker, animation, typeOf(options.loopMode()).name());
        }
        return started;
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
        return tracker.viewedPlayer().collect(Collectors.toList());
    }

    @Override
    public void hide(Player player) {
        tracker.hide(player);
    }

    @Override
    public void show(Player player) {
        tracker.show(player);
    }

    @Override
    public List<BmBone> bones() {
        return tracker.bones().stream()
                .map(bone -> (BmBone) new V1Bone(tracker, bone, platform.boneState(bone)))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BmBone> bone(String name) {
        return Optional.ofNullable(tracker.bone(name))
                .map(bone -> new V1Bone(tracker, bone, platform.boneState(bone)));
    }
}
