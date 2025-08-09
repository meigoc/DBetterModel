package meigo.dbettermodel.services;

import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BoneController {

    private final EntityTracker tracker;
    private final RenderedBone bone;
    private final Quaternionf customRotation = new Quaternionf();
    private TransformedItemStack currentTransformedItemStack;

    public BoneController(EntityTracker tracker, RenderedBone bone) {
        this.tracker = tracker;
        this.bone = bone;
        this.currentTransformedItemStack = bone.getGroup().getItemStack().copy();

        this.bone.addRotationModifier(BonePredicate.TRUE, animationRotation ->
                animationRotation.mul(customRotation, new Quaternionf()));
    }

    private void forceUpdate() {
        tracker.forceUpdate(true);
    }

    // --- Getters for Tags ---
    public Location getWorldLocation() {
        Vector3f worldPos = bone.worldPosition();
        return tracker.sourceEntity().getLocation().add(worldPos.x(), worldPos.y(), worldPos.z());
    }

    public Vector3f getWorldRotationEuler() {
        return bone.worldRotation();
    }

    public boolean isVisible() {
        return !bone.getDisplay().invisible();
    }

    // --- Handlers for Mechanisms ---
    public void setRotation(Quaternionf rotation) {
        this.customRotation.set(rotation);
        forceUpdate();
    }

    public void setTint(int color) {
        if (bone.tint(BonePredicate.TRUE, color)) {
            forceUpdate();
        }
    }

    public void setVisible(boolean visible) {
        if (bone.togglePart(BonePredicate.TRUE, visible)) {
            forceUpdate();
        }
    }

    public void setItem(ItemStack itemStack) {
        this.currentTransformedItemStack = new TransformedItemStack(
                this.currentTransformedItemStack.position(),
                this.currentTransformedItemStack.offset(),
                this.currentTransformedItemStack.scale(),
                itemStack
        );
        updateItemStack();
    }

    public void setOffset(Vector3f localOffset) {
        this.currentTransformedItemStack = new TransformedItemStack(
                this.currentTransformedItemStack.position(),
                localOffset,
                this.currentTransformedItemStack.scale(),
                this.currentTransformedItemStack.itemStack()
        );
        updateItemStack();
    }

    public void setScale(Vector3f scale) {
        this.currentTransformedItemStack = new TransformedItemStack(
                this.currentTransformedItemStack.position(),
                this.currentTransformedItemStack.offset(),
                scale,
                this.currentTransformedItemStack.itemStack()
        );
        updateItemStack();
    }

    public void setInterpolationDuration(int ticks) {
        bone.moveDuration(ticks);
        forceUpdate();
    }

    public void setGlow(boolean glow) {
        if (bone.glow(BonePredicate.TRUE, glow)) {
            forceUpdate();
        }
    }

    public void setGlowColor(int color) {
        if (bone.glowColor(BonePredicate.TRUE, color)) {
            forceUpdate();
        }
    }

    public void setBrightness(int blockLight, int skyLight) {
        if (bone.brightness(BonePredicate.TRUE, blockLight, skyLight)) {
            forceUpdate();
        }
    }

    private void updateItemStack() {
        if (bone.itemStack(BonePredicate.TRUE, this.currentTransformedItemStack)) {
            forceUpdate();
        }
    }

    // --- Command Logic ---
    public void mount(Entity entity) {
        HitBox hitbox = bone.getHitBox();
        if (hitbox != null && hitbox.mountController().canMount()) {
            hitbox.mount(entity);
        }
    }

    public void dismount(Entity entity) {
        HitBox hitbox = bone.getHitBox();
        if (hitbox != null) {
            hitbox.dismount(entity);
        }
    }

    public void dismountAll() {
        HitBox hitbox = bone.getHitBox();
        if (hitbox != null) {
            hitbox.dismountAll();
        }
    }
}