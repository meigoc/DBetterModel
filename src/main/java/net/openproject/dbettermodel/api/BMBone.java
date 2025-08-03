package net.openproject.dbettermodel.api;

import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API wrapper for a model's bone, now with caching to preserve state across multiple Denizen script calls.
 */
public class BMBone {

    /**
     * Cache to store a single instance of BMBone for each unique model bone.
     * This solves the state-loss issue caused by Denizen re-creating tags on every command.
     */
    private static final Map<String, BMBone> boneApiCache = new ConcurrentHashMap<>();

    private final EntityTracker tracker;
    private final RenderedBone bone;
    private final Quaternionf customRotation = new Quaternionf();

    private TransformedItemStack currentTransformedItemStack;

    /**
     * Private constructor to enforce the use of the factory method.
     * @param tracker The entity tracker for the model.
     * @param bone The underlying RenderedBone object.
     */
    private BMBone(EntityTracker tracker, RenderedBone bone) {
        this.tracker = tracker;
        this.bone = bone;
        this.currentTransformedItemStack = bone.getGroup().getItemStack().copy();

        this.bone.addRotationModifier(BonePredicate.TRUE, animationRotation ->
                customRotation);
    }

    /**
     * Factory method to get or create a BMBone instance.
     * Ensures that only one BMBone object exists per actual bone, preserving its state.
     *
     * @param tracker The entity tracker.
     * @param bone The model bone.
     * @return The cached or newly created BMBone instance.
     */
    public static BMBone getOrCreate(EntityTracker tracker, RenderedBone bone) {
        String uniqueKey = tracker.registry().uuid().toString() + "," + tracker.name() + "," + bone.getName().name();
        return boneApiCache.computeIfAbsent(uniqueKey, k -> new BMBone(tracker, bone));
    }

    /**
     * IMPORTANT: Clears the cache for a specific model tracker.
     * This must be called when a model is removed from an entity to prevent memory leaks.
     *
     * @param tracker The tracker of the model being removed.
     */
    public static void clearCacheFor(EntityTracker tracker) {
        String prefix = tracker.registry().uuid().toString() + "," + tracker.name() + ",";
        boneApiCache.keySet().removeIf(key -> key.startsWith(prefix));
    }


    /**
     * Sets an additional rotation to be applied to the bone, post-animation.
     * Each call replaces the previous custom rotation.
     *
     * @param rotation The Quaternionf representing the desired additional rotation.
     */
    public void setRotation(Quaternionf rotation) {
        this.customRotation.set(rotation);
        tracker.forceUpdate(true);
    }

    /**
     * Applies a color tint to the bone's item.
     *
     * @param color The color as an integer (e.g., 0xFF0000 for red).
     */
    public void setTint(int color) {
        if (bone.tint(BonePredicate.TRUE, color)) {
            tracker.forceUpdate(true);
        }
    }

    /**
     * Sets the visibility of the bone for all players.
     *
     * @param visible True to make the bone visible, false to hide it.
     */
    public void setVisible(boolean visible) {
        if (bone.togglePart(BonePredicate.TRUE, visible)) {
            tracker.forceUpdate(true);
        }
    }

    /**
     * Sets the visibility of the bone for a specific list of players.
     * Note: This is a temporary, packet-based change.
     *
     * @param visible True to show the bone, false to hide it.
     * @param players The list of players for this visibility change.
     */
    public void setVisible(boolean visible, List<Player> players) {
        if (players == null || players.isEmpty()) {
            setVisible(visible);
            return;
        }
        ModelDisplay display = bone.getDisplay();
        if (display == null) {
            // Cannot apply per-player visibility to a dummy bone without a display.
            return;
        }
        PacketBundler bundler = tracker.getPipeline().createParallelBundler();
        if (visible) {
            display.spawn(true, bundler);
            display.sendTransformation(bundler);
        } else {
            display.remove(bundler);
        }
        for (Player player : players) {
            bundler.send(player);
        }
    }

    /**
     * Internal helper method to apply the current TransformedItemStack to the bone.
     */
    private void updateItemStack() {
        if (bone.itemStack(BonePredicate.TRUE, this.currentTransformedItemStack)) {
            tracker.forceUpdate(true);
        }
    }

    /**
     * Sets the item displayed by this bone, preserving the current offset and scale.
     *
     * @param itemStack The ItemStack to display.
     */
    public void setItem(@NotNull ItemStack itemStack) {
        this.currentTransformedItemStack = new TransformedItemStack(
                this.currentTransformedItemStack.position(),
                this.currentTransformedItemStack.offset(),
                this.currentTransformedItemStack.scale(),
                itemStack
        );
        updateItemStack();
    }

    /**
     * Sets the local offset of the bone's displayed item, preserving the current item and scale.
     *
     * @param localOffset A Vector3f representing the new local offset.
     */
    public void setOffset(@NotNull Vector3f localOffset) {
        this.currentTransformedItemStack = new TransformedItemStack(
                this.currentTransformedItemStack.position(),
                localOffset,
                this.currentTransformedItemStack.scale(),
                this.currentTransformedItemStack.itemStack()
        );
        updateItemStack();
    }

    /**
     * Sets the scale of the bone's displayed item, preserving the current item and offset.
     *
     * @param scale A Vector3f representing the new scale.
     */
    public void setScale(@NotNull Vector3f scale) {
        this.currentTransformedItemStack = new TransformedItemStack(
                this.currentTransformedItemStack.position(),
                this.currentTransformedItemStack.offset(),
                scale,
                this.currentTransformedItemStack.itemStack()
        );
        updateItemStack();
    }

    /**
     * Sets the view range for the bone's display entity.
     *
     * @param range The distance in blocks at which the bone will be visible.
     */
    public void setViewRange(float range) {
        ModelDisplay display = bone.getDisplay();
        if (display != null) {
            display.viewRange(range);
            tracker.forceUpdate(true);
        }
    }
}
