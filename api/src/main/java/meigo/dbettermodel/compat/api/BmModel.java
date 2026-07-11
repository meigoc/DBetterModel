/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

import java.util.OptionalDouble;
import java.util.Set;

/**
 * A model blueprint (BetterModel {@code ModelRenderer}) — static data, no live tracker.
 */
public interface BmModel {

    /** Model name as registered in BetterModel. */
    String name();

    /** Names of all animations defined on this model. */
    Set<String> animations();

    /**
     * Length of the named animation in seconds, empty if the animation does not exist.
     */
    OptionalDouble animationLength(String animation);

    /**
     * Raw default loop mode name of the named animation ({@code PLAY_ONCE}/{@code LOOP}/{@code HOLD_ON_LAST}),
     * empty if the animation does not exist.
     */
    java.util.Optional<String> animationLoopMode(String animation);

    /** Names of all bones (groups) defined on this model. */
    Set<String> boneNames();
}
