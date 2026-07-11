/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;

/**
 * Per-RenderedBone mutable state. BetterModel offers no rotation-modifier removal in any
 * version, so exactly one modifier is installed per bone, reading {@link #rotation} —
 * fixing the 5.x accumulation bug. The last-set {@link TransformedItemStack} is kept here
 * too so item/offset/scale writes compose.
 */
final class V2BoneState {

    final Quaternionf rotation = new Quaternionf();
    TransformedItemStack item;

    // Last values written through DBetterModel — BetterModel has no display-state reads.
    boolean glow;
    int glowColor = 0xFFFFFF;
    int tint = 0xFFFFFF;
    Display.Billboard billboard = Display.Billboard.FIXED;
    float viewRange = 1.0f;
    int brightnessBlock;
    int brightnessSky = 15;
    float shadowRadius;
    ItemStack lastItem;

    V2BoneState(RenderedBone bone) {
        this.item = bone.getGroup().getItemStack().copy();
        bone.addRotationModifier(BonePredicate.TRUE, animationRotation ->
                animationRotation.mul(rotation, new Quaternionf()));
    }
}
