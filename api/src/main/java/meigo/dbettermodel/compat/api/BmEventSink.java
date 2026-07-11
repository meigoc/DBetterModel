/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Callback surface the core registers via {@link BmPlatform#registerEvents}. Each layer
 * fires only what its BetterModel line can deliver — a callback that never fires means the
 * matching {@link BmCapability} is absent. All methods default to no-ops so future phases
 * (P4) can override selectively without api changes.
 */
public interface BmEventSink {

    /** BetterModel starts reloading. */
    default void reloadStart() {}

    /** BetterModel finished reloading. */
    default void reloadEnd(BmReloadResult result) {}

    /** A tracker (entity or dummy) was created. */
    default void trackerCreated(BmTracker tracker) {}

    /** A tracker was closed/removed. */
    default void trackerClosed(BmTracker tracker) {}

    /**
     * An animation signal fired — either a {@code denizen:} instruction keyframe
     * (script builder; player is null) or a per-player {@code AnimationSignalEvent}
     * (metadata empty). Guarded by {@link BmCapability#ANIMATION_SIGNALS} /
     * {@link BmCapability#PLAYER_ANIMATION_EVENTS}.
     *
     * <p>The tracker is the model instance whose animation emitted the signal. Builder-path
     * signals carry it on every line (the compiled script receives the BM tracker); per-player
     * {@code AnimationSignalEvent} carries no tracker in any BetterModel line, so it is null there.</p>
     */
    default void animationSignal(BmTracker tracker, String signal, Map<String, String> metadata, Player player) {}

    /**
     * A per-player animation sequence started on a tracker (BetterModel
     * {@code PlayerPerAnimationStartEvent}). Player is the viewer the animation plays for;
     * nullable to leave room for future global-animation delivery.
     * Guarded by {@link BmCapability#ANIMATION_LIFECYCLE_EVENTS}.
     */
    default void animationStarted(BmTracker tracker, Player player) {}

    /**
     * A per-player animation sequence ended on a tracker (BetterModel
     * {@code PlayerPerAnimationEndEvent}). Guarded by {@link BmCapability#ANIMATION_LIFECYCLE_EVENTS}.
     */
    default void animationEnded(BmTracker tracker, Player player) {}

    /** Model spawned at (became visible to) a player. Guarded by {@link BmCapability#SPAWN_EVENTS}. */
    default void modelSpawnAtPlayer(BmTracker tracker, Player player) {}

    /** Model despawned at (became invisible to) a player. Guarded by {@link BmCapability#SPAWN_EVENTS}. */
    default void modelDespawnAtPlayer(BmTracker tracker, Player player) {}

    /**
     * A player interacted with a model hitbox. Tracker may be null when the layer cannot
     * resolve it. Guarded by {@link BmCapability#HITBOX_INTERACT_EVENT} / {@code _AT_EVENT}.
     */
    default void hitboxInteracted(BmTracker tracker, Player player, String hand) {}

    /** A model hitbox took damage. Guarded by {@link BmCapability#HITBOX_DAMAGE_EVENTS}. */
    default void hitboxDamaged(BmTracker tracker, double damage) {}

    /** An entity mounted a model bone hitbox. Guarded by {@link BmCapability#MOUNT_EVENTS}. */
    default void mounted(BmTracker tracker, BmBone bone, Entity passenger) {}

    /** An entity dismounted a model bone hitbox. Guarded by {@link BmCapability#MOUNT_EVENTS}. */
    default void dismounted(BmTracker tracker, BmBone bone, Entity passenger) {}
}
