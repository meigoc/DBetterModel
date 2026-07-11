/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * An active model instance (BetterModel {@code Tracker}). Entity-bound trackers report
 * their source entity UUID; dummy (location-bound) trackers return null there.
 */
public interface BmTracker {

    /** Model name of this tracker. */
    String name();

    /** UUID of the source entity, or null for dummy trackers. */
    UUID entityUuid();

    /** Blueprint of this tracker's model. */
    BmModel model();

    /**
     * Plays an animation with the given options (bones filter, per-player list, loop, speed, lerp).
     * Per-player playback is guarded by {@link BmCapability#PER_PLAYER_ANIMATION}.
     *
     * @return true if the animation started for at least one target
     */
    boolean animate(String animation, BmAnimationOptions options);

    /**
     * Stops an animation, optionally restricted to the named bones (null = all).
     *
     * @return true if the animation was running on the targeted bones
     */
    boolean stopAnimation(String animation, Set<String> bones);

    /** The animation currently playing on the tracker pipeline, if any. */
    Optional<BmRunningAnimation> runningAnimation();

    /** Forces a full update packet to viewers. */
    void forceUpdate();

    /** Closes (removes) this tracker. */
    void close();

    /** Players currently seeing this tracker. */
    List<Player> viewers();

    /** Hides the whole tracker from one player. */
    void hide(Player player);

    /** Shows the tracker to one player again. */
    void show(Player player);

    /** All bones of this tracker. */
    List<BmBone> bones();

    /** Looks up a bone by name. */
    Optional<BmBone> bone(String name);
}
