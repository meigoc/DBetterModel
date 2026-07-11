/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * A rendered bone of an active tracker. Write methods force an update packet themselves
 * where BetterModel requires one — callers never touch the pipeline.
 *
 * <p>Rotation via {@link #setRotation} is idempotent: the layer installs exactly one
 * rotation modifier per underlying bone that reads a mutable holder (fixes the 5.x
 * modifier-accumulation bug — BetterModel offers no modifier removal in any version).</p>
 */
public interface BmBone {

    /** Bone name (without tags). */
    String name();

    // --- reads ---

    /** Bone position relative to the model origin (BetterModel world position). */
    Vector3f worldPosition();

    /** Bone world rotation as Euler angles (degrees). */
    Vector3f worldRotationEuler();

    /** Hitbox view rotation quaternion; identity when the bone has no hitbox rotation data. */
    Quaternionf hitboxViewRotation();

    /** Whether the bone display is currently visible. False for dummy bones. */
    boolean isVisible();

    /** World location of the bone, absolute (source entity world + position). May be null for dummies. */
    Location worldLocation();

    // --- cached reads ---
    // BetterModel exposes no display-state reads, so each getter below reflects the last
    // value set through DBetterModel (cached per bone), or a sensible default before the
    // first write. They do NOT observe changes made by BetterModel itself or other plugins.

    /** Last glow flag set through DBetterModel; false before the first write. */
    boolean getGlow();

    /** Last glow color (RGB) set through DBetterModel; white (0xFFFFFF) before the first write. */
    int getGlowColor();

    /** Last tint (RGB) set through DBetterModel; neutral white (0xFFFFFF) before the first write. */
    int getTint();

    /** Last billboard mode set through DBetterModel; FIXED before the first write. */
    Display.Billboard getBillboard();

    /** Last view range set through DBetterModel; the display default 1.0 before the first write. */
    float getViewRange();

    /** Last block-light override set through DBetterModel; 0 before the first write. */
    int getBrightnessBlock();

    /** Last sky-light override set through DBetterModel; 15 before the first write. */
    int getBrightnessSky();

    /** Last shadow radius set through DBetterModel; 0 before the first write. */
    float getShadowRadius();

    /** Last scale set through DBetterModel; the model's own item scale before the first write. */
    Vector3f getScale();

    /** Last item offset set through DBetterModel; the model's own item offset before the first write. */
    Vector3f getOffset();

    /** Last item set through DBetterModel; null before the first write. */
    ItemStack getItem();

    // --- writes (each triggers its own update) ---

    /** Sets the custom rotation applied on top of animation rotation. Idempotent installation. */
    void setRotation(Quaternionf rotation);

    void setTint(int rgb);

    void setVisible(boolean visible);

    /** Per-player visibility toggle; falls back to global when players is null/empty. */
    void setVisible(boolean visible, List<Player> players);

    void setItem(ItemStack item);

    /** Local offset of the displayed item. */
    void setOffset(Vector3f offset);

    void setScale(Vector3f scale);

    /** Display interpolation (move) duration in ticks. */
    void setInterpolationDuration(int ticks);

    void setGlow(boolean glow);

    void setGlowColor(int rgb);

    void setBrightness(int blockLight, int skyLight);

    void setViewRange(float range);

    void setShadowRadius(float radius);

    /**
     * Billboard mode of the bone display (the billboard controller hook).
     *
     * @return true if the display accepted the change (false for dummy bones)
     */
    boolean setBillboard(Display.Billboard billboard);

    // --- hitbox / mounting ---

    /** Whether this bone carries a hitbox (seat) — required for mounting. */
    boolean hasHitBox();

    /** Whether the hitbox mount controller allows mounting. False when no hitbox. */
    boolean canMount();

    /** Mounts the entity on this bone's hitbox. No-op without a hitbox. */
    void mount(Entity entity);

    /** Dismounts the entity from this bone's hitbox. No-op without a hitbox. */
    void dismount(Entity entity);

    /** Dismounts every passenger from this bone's hitbox. No-op without a hitbox. */
    void dismountAll();
}
