/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

/**
 * Feature flags declared by each compat layer. Anything a layer cannot do is simply
 * absent from {@link BmPlatform#capabilities()} — the Denizen surface reports a clean
 * error or never fires the related event instead of stack-tracing.
 */
public enum BmCapability {
    /** The BetterModel line ships its own event bus (2.x/3.x). 1.x uses Bukkit events. */
    EVENT_BUS,
    /** Blockbench instruction keyframes routed through the {@code denizen:} script builder. */
    ANIMATION_SIGNALS,
    /** Location-bound (entity-less) trackers — {@link BmPlatform#summon}. */
    DUMMY_TRACKERS,
    /** {@code AnimationModifier.priority} — 3.x only. */
    ANIMATION_PRIORITY,
    /** Per-player animation via the modifier player field. */
    PER_PLAYER_ANIMATION,
    /** Hitbox damage events reachable ({@link BmEventSink#hitboxDamaged}). */
    HITBOX_DAMAGE_EVENTS,
    /** Per-player model spawn/despawn events ({@link BmEventSink#modelSpawnAtPlayer}). */
    SPAWN_EVENTS,
    /** Player limb animation events incl. per-player {@code AnimationSignalEvent}. */
    PLAYER_ANIMATION_EVENTS,
    /** Skin/profile API ({@link BmPlayerLimbs#applySkinPart}). */
    SKIN_API,
    /** Mount/dismount model events ({@link BmEventSink#mounted}). */
    MOUNT_EVENTS,
    /** Non-positional hitbox interact event (v1/v2 only). */
    HITBOX_INTERACT_EVENT,
    /** Positional hitbox interact-at event (all lines). */
    HITBOX_INTERACT_AT_EVENT,
    /** {@code TrackerAnimation} API (BM 2.2+). */
    TRACKER_ANIMATION_API,
    /** Animate restricted to a bone subtree in a single tracker call (v1 only natively). */
    BONE_FILTER_ANIMATE,
    /** {@code NMS.createPlayerHead} (v1/v2 only). */
    PLAYER_HEAD_ITEM,
    /** Per-player animation start/end events ({@link BmEventSink#animationStarted}) — declared only by wiring layers. */
    ANIMATION_LIFECYCLE_EVENTS
}
