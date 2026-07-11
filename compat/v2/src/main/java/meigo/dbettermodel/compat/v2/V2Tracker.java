/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import meigo.dbettermodel.compat.api.BmAnimationOptions;
import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.compat.api.BmModel;
import meigo.dbettermodel.compat.api.BmRunningAnimation;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class V2Tracker implements BmTracker {

    private final V2Platform platform;
    private final Tracker tracker;

    V2Tracker(V2Platform platform, Tracker tracker) {
        this.platform = platform;
        this.tracker = tracker;
    }

    Tracker handle() {
        return tracker;
    }

    // Wrappers are recreated per lookup; identity follows the underlying BM tracker.
    @Override
    public boolean equals(Object o) {
        return o instanceof V2Tracker other && tracker == other.tracker;
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
        return new V2Model(tracker.renderer());
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
        boolean started = animateInternal(animation, options);
        if (started) {
            // The pipeline only reports the animation after its first async tick.
            platform.markStarted(tracker, animation, typeOf(options.loopMode()).name());
        }
        return started;
    }

    private boolean animateInternal(String animation, BmAnimationOptions options) {
        Set<String> bones = options.bones();
        List<Player> players = options.players();
        if (bones != null && !bones.isEmpty()) {
            // 2.2.0 dropped the bone-filter animate overloads; the modern path emulates them
            // per bone via addAnimation. Older 2.x sub-lines animate the whole model
            // (BONE_FILTER_ANIMATE capability is absent there, the core reports it).
            if (platform.hasModernAnimation()) {
                if (players == null || players.isEmpty()) {
                    return V2AnimationModern.animate(tracker, bones, animation, modifierOf(options).build());
                }
                boolean any = false;
                for (Player player : players) {
                    any |= V2AnimationModern.animate(tracker, bones, animation,
                            modifierOf(options).player(BukkitAdapter.adapt(player)).build());
                }
                return any;
            }
        }
        if (players == null || players.isEmpty()) {
            return tracker.animate(animation, modifierOf(options).build(), () -> {});
        }
        // Per-player playback: one modifier per viewer (the official 2.x per-player path).
        boolean any = false;
        for (Player player : players) {
            any |= tracker.animate(animation, modifierOf(options).player(BukkitAdapter.adapt(player)).build(), () -> {});
        }
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
        return tracker.getPipeline().viewedPlayer()
                .map(V2Adapters::bukkit)
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
                .map(bone -> (BmBone) new V2Bone(tracker, bone, platform.boneState(bone)))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BmBone> bone(String name) {
        return Optional.ofNullable(tracker.bone(name))
                .map(bone -> new V2Bone(tracker, bone, platform.boneState(bone)));
    }
}
