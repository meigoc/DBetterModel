/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * Version-neutral animation playback options. Built once by the Denizen surface,
 * translated into the line-specific {@code AnimationModifier} by each layer.
 *
 * <ul>
 *   <li>{@code bones} — restrict the animation to these bone names (null = whole model).
 *       Guarded by {@link BmCapability#BONE_FILTER_ANIMATE}; layers without it emulate per bone.</li>
 *   <li>{@code players} — play only for these viewers (null/empty = everyone).
 *       Guarded by {@link BmCapability#PER_PLAYER_ANIMATION}.</li>
 * </ul>
 */
public final class BmAnimationOptions {

    /** Loop mode, mapping to BetterModel's {@code AnimationIterator.Type}. */
    public enum LoopMode { ONCE, LOOP, HOLD }

    private final LoopMode loopMode;
    private final float speed;
    private final int lerpTicks;
    private final Boolean override;
    private final Set<String> bones;
    private final List<Player> players;

    private BmAnimationOptions(Builder b) {
        this.loopMode = b.loopMode;
        this.speed = b.speed;
        this.lerpTicks = b.lerpTicks;
        this.override = b.override;
        this.bones = b.bones;
        this.players = b.players;
    }

    public LoopMode loopMode() { return loopMode; }
    public float speed() { return speed; }
    /** Lerp/blend-in duration in ticks. */
    public int lerpTicks() { return lerpTicks; }
    /** Override flag; null = layer default. */
    public Boolean override() { return override; }
    /** Bone-name filter; null = all bones. */
    public Set<String> bones() { return bones; }
    /** Per-player viewer list; null = all viewers. */
    public List<Player> players() { return players; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private LoopMode loopMode = LoopMode.ONCE;
        private float speed = 1.0f;
        private int lerpTicks = 0;
        private Boolean override;
        private Set<String> bones;
        private List<Player> players;

        public Builder loopMode(LoopMode loopMode) { this.loopMode = loopMode; return this; }
        public Builder speed(float speed) { this.speed = speed; return this; }
        public Builder lerpTicks(int lerpTicks) { this.lerpTicks = lerpTicks; return this; }
        public Builder override(Boolean override) { this.override = override; return this; }
        public Builder bones(Set<String> bones) { this.bones = bones; return this; }
        public Builder players(List<Player> players) { this.players = players; return this; }

        public BmAnimationOptions build() {
            return new BmAnimationOptions(this);
        }
    }
}
