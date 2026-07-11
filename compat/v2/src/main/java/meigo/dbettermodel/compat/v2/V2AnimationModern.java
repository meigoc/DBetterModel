/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationOverrideState;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.tracker.Tracker;

import java.util.Set;

/**
 * 2.2.0-only animation paths. {@link AnimationOverrideState} does not exist before 2.2.0,
 * so this class must never be loaded on 2.0.x/2.1.x — callers gate on
 * {@code V2Platform.hasModernAnimation()} (JVM lazy loading keeps it safe).
 */
final class V2AnimationModern {

    private V2AnimationModern() {
    }

    /** Emulates the removed bone-filter animate overloads via per-bone addAnimation. */
    static boolean animate(Tracker tracker, Set<String> bones, String animation, AnimationModifier modifier) {
        BlueprintAnimation blueprint = tracker.renderer().animation(animation).orElse(null);
        if (blueprint == null) return false;
        boolean any = false;
        for (RenderedBone bone : tracker.bones()) {
            if (!bones.contains(bone.name().name())) continue;
            any |= bone.addAnimation(AnimationOverrideState.MATCHED, blueprint, modifier, () -> {});
        }
        return any;
    }
}
