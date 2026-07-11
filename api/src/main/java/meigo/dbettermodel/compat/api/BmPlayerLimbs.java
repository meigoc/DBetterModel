/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Player limb ("player-animations") operations. Guarded by
 * {@link BmCapability#PLAYER_ANIMATION_EVENTS} / {@link BmCapability#SKIN_API}.
 */
public interface BmPlayerLimbs {

    /** Outcome of {@link #applySkinPart}. */
    enum SkinPartResult { SUCCESS, TARGET_GONE, MODEL_NOT_FOUND, BONE_NOT_FOUND, INVALID_PART }

    /**
     * Plays a limb animation on a player from a model in the player-animations folder.
     *
     * @return true if the animation started
     */
    boolean playLimbAnimation(Player player, String limbModel, String animation, BmAnimationOptions options);

    /** Names of all skin parts (BetterModel {@code PlayerLimb} constants), lower-cased. */
    java.util.Collection<String> skinPartNames();

    /**
     * Applies a skin part of {@code source}'s player profile to a bone of a model on
     * {@code target}. Skin completion is async; the layer re-resolves the target on the
     * main thread before applying (matching 5.x bmpart semantics). The future completes
     * exceptionally when the skin lookup fails.
     */
    CompletableFuture<SkinPartResult> applySkinPart(java.util.function.Supplier<Optional<Entity>> target,
                                                    String modelName, String boneName,
                                                    String partName, Player source);
}
