/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v1;

import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import meigo.dbettermodel.compat.api.BmBone;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

final class V1Bone implements BmBone {

    private final Tracker tracker;
    private final RenderedBone bone;
    private final V1BoneState state;

    V1Bone(Tracker tracker, RenderedBone bone, V1BoneState state) {
        this.tracker = tracker;
        this.bone = bone;
        this.state = state;
    }

    RenderedBone handle() {
        return bone;
    }

    private void forceUpdate() {
        tracker.forceUpdate(true);
    }

    @Override
    public String name() {
        return bone.name().name();
    }

    @Override
    public Vector3f worldPosition() {
        return bone.worldPosition();
    }

    @Override
    public Vector3f worldRotationEuler() {
        return bone.worldRotation();
    }

    @Override
    public Quaternionf hitboxViewRotation() {
        return bone.hitBoxViewRotation();
    }

    @Override
    public boolean isVisible() {
        ModelDisplay display = bone.getDisplay();
        return display != null && !display.invisible();
    }

    @Override
    public Location worldLocation() {
        Location base = tracker.location();
        if (base == null) return null;
        Vector3f pos = bone.worldPosition();
        return new Location(base.getWorld(), pos.x(), pos.y(), pos.z());
    }


    @Override
    public boolean getGlow() {
        return state.glow;
    }

    @Override
    public int getGlowColor() {
        return state.glowColor;
    }

    @Override
    public int getTint() {
        return state.tint;
    }

    @Override
    public Display.Billboard getBillboard() {
        return state.billboard;
    }

    @Override
    public float getViewRange() {
        return state.viewRange;
    }

    @Override
    public int getBrightnessBlock() {
        return state.brightnessBlock;
    }

    @Override
    public int getBrightnessSky() {
        return state.brightnessSky;
    }

    @Override
    public float getShadowRadius() {
        return state.shadowRadius;
    }

    @Override
    public Vector3f getScale() {
        return new Vector3f(state.item.scale());
    }

    @Override
    public Vector3f getOffset() {
        return new Vector3f(state.item.offset());
    }

    @Override
    public ItemStack getItem() {
        return state.lastItem;
    }

    @Override
    public void setRotation(Quaternionf rotation) {
        state.rotation.set(rotation);
        forceUpdate();
    }

    @Override
    public void setTint(int rgb) {
        state.tint = rgb;
        if (bone.tint(BonePredicate.TRUE, rgb)) forceUpdate();
    }

    @Override
    public void setVisible(boolean visible) {
        if (bone.applyAtDisplay(BonePredicate.TRUE, display -> display.invisible(!visible))) forceUpdate();
    }

    @Override
    public void setVisible(boolean visible, List<Player> players) {
        if (players == null || players.isEmpty()) {
            setVisible(visible);
            return;
        }
        ModelDisplay display = bone.getDisplay();
        if (display == null) return;
        PacketBundler bundler = tracker.getPipeline().createParallelBundler();
        if (visible) {
            display.spawn(true, bundler);
        } else {
            display.remove(bundler);
        }
        for (Player player : players) {
            bundler.send(player);
        }
    }

    @Override
    public void setItem(ItemStack item) {
        state.lastItem = item;
        state.item = new TransformedItemStack(state.item.position(), state.item.offset(), state.item.scale(), item);
        updateItemStack();
    }

    @Override
    public void setOffset(Vector3f offset) {
        state.item = new TransformedItemStack(state.item.position(), offset, state.item.scale(), state.item.itemStack());
        updateItemStack();
    }

    @Override
    public void setScale(Vector3f scale) {
        state.item = new TransformedItemStack(state.item.position(), state.item.offset(), scale, state.item.itemStack());
        updateItemStack();
    }

    private void updateItemStack() {
        if (bone.itemStack(BonePredicate.TRUE, state.item)) forceUpdate();
    }

    @Override
    public void setInterpolationDuration(int ticks) {
        bone.applyAtDisplay(BonePredicate.TRUE, display -> display.moveDuration(ticks));
        forceUpdate();
    }

    @Override
    public void setGlow(boolean glow) {
        state.glow = glow;
        if (bone.applyAtDisplay(BonePredicate.TRUE, display -> display.glow(glow))) forceUpdate();
    }

    @Override
    public void setGlowColor(int rgb) {
        state.glowColor = rgb;
        if (bone.applyAtDisplay(BonePredicate.TRUE, display -> display.glowColor(rgb))) forceUpdate();
    }

    @Override
    public void setBrightness(int blockLight, int skyLight) {
        state.brightnessBlock = blockLight;
        state.brightnessSky = skyLight;
        if (bone.applyAtDisplay(BonePredicate.TRUE, display -> display.brightness(blockLight, skyLight))) forceUpdate();
    }

    @Override
    public void setViewRange(float range) {
        state.viewRange = range;
        ModelDisplay display = bone.getDisplay();
        if (display != null) {
            display.viewRange(range);
            PacketBundler bundler = tracker.getPipeline().createParallelBundler();
            display.sendDirtyEntityData(bundler);
            tracker.getPipeline().viewedPlayer().forEach(bundler::send);
        }
    }

    @Override
    public void setShadowRadius(float radius) {
        state.shadowRadius = radius;
        if (bone.applyAtDisplay(BonePredicate.TRUE, display -> display.shadowRadius(radius))) forceUpdate();
    }

    @Override
    public boolean setBillboard(Display.Billboard billboard) {
        state.billboard = billboard;
        if (bone.applyAtDisplay(BonePredicate.TRUE, display -> display.billboard(billboard))) {
            forceUpdate();
            return true;
        }
        return false;
    }

    @Override
    public boolean hasHitBox() {
        return bone.getHitBox() != null;
    }

    @Override
    public boolean canMount() {
        HitBox hitBox = bone.getHitBox();
        return hitBox != null && hitBox.mountController().canMount();
    }

    @Override
    public void mount(Entity entity) {
        HitBox hitBox = bone.getHitBox();
        if (hitBox != null) hitBox.mount(entity);
    }

    @Override
    public void dismount(Entity entity) {
        HitBox hitBox = bone.getHitBox();
        if (hitBox != null) hitBox.dismount(entity);
    }

    @Override
    public void dismountAll() {
        HitBox hitBox = bone.getHitBox();
        if (hitBox != null) hitBox.dismountAll();
    }
}
