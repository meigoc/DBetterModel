/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */
package meigo.dbettermodel.services;

import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Thin core-side facade over a compat-layer bone. Rotation-modifier lifecycle (5.x
 * accumulation bug) is owned by the layer — see BmBone.setRotation.
 */
public class BoneController {

    private final BmTracker tracker;
    private final BmBone bone;

    public BoneController(BmTracker tracker, BmBone bone) {
        this.tracker = tracker;
        this.bone = bone;
    }

    // --- Getters for Tags ---
    public Location getWorldLocation() {
        return bone.worldLocation();
    }

    public Vector3f getWorldRotationEuler() {
        return bone.worldRotationEuler();
    }

    public boolean isVisible() {
        return bone.isVisible();
    }

    // --- Handlers for Mechanisms ---
    public void setRotation(Quaternionf rotation) {
        bone.setRotation(rotation);
    }

    public void setTint(int color) {
        bone.setTint(color);
    }

    public void setVisible(boolean visible) {
        bone.setVisible(visible);
    }

    public void setVisible(boolean visible, List<Player> players) {
        bone.setVisible(visible, players);
    }

    public void setViewRange(float range) {
        bone.setViewRange(range);
    }

    public void setItem(ItemStack itemStack) {
        bone.setItem(itemStack);
    }

    public void setOffset(Vector3f localOffset) {
        bone.setOffset(localOffset);
    }

    public void setScale(Vector3f scale) {
        bone.setScale(scale);
    }

    public void setInterpolationDuration(int ticks) {
        bone.setInterpolationDuration(ticks);
    }

    public void setGlow(boolean glow) {
        bone.setGlow(glow);
    }

    public void setGlowColor(int color) {
        bone.setGlowColor(color);
    }

    public void setBrightness(int blockLight, int skyLight) {
        bone.setBrightness(blockLight, skyLight);
    }

    public void setShadowRadius(float radius) {
        bone.setShadowRadius(radius);
    }

    public void setBillboard(Display.Billboard type) {
        bone.setBillboard(type);
    }

    // --- Command Logic ---
    public void mount(Entity entity) {
        if (bone.canMount()) {
            bone.mount(entity);
        }
    }

    public void dismount(Entity entity) {
        bone.dismount(entity);
    }

    public void dismountAll() {
        bone.dismountAll();
    }
}
