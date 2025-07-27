package net.openproject.dbettermodel.api;

import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Represents a high-level API wrapper for a single bone of a BetterModel model.
 * This class simplifies common bone manipulations by abstracting away the underlying
 * RenderedBone and EntityTracker interactions.
 */
public class BMBone {

    private final EntityTracker tracker;
    private final RenderedBone bone;

    /**
     * Constructs a new BMBone API object.
     *
     * @param tracker The entity tracker for the model this bone belongs to.
     * @param bone    The underlying RenderedBone object from the BetterModel API.
     */
    public BMBone(EntityTracker tracker, RenderedBone bone) {
        this.tracker = tracker;
        this.bone = bone;
    }

    /**
     * Applies a color tint to the bone's item.
     *
     * @param color The color as a single integer representing the RGB value (e.g., 0xFF0000 for red).
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
     * Note: This is a temporary, packet-based change and may reset if the player reloads the model.
     *
     * @param visible True to show the bone to the players, false to hide it.
     * @param players The list of players to whom the visibility change should apply.
     */
    public void setVisible(boolean visible, List<Player> players) {
        if (players == null || players.isEmpty()) {
            setVisible(visible); // Delegate to global method
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
     * Sets the item displayed by this bone.
     *
     * @param itemStack   The ItemStack to display.
     * @param localOffset An optional local translation to apply to the item.
     */
    public void setItem(ItemStack itemStack, @Nullable Vector3f localOffset) {
        // Since RenderedBone#getItemStack is unavailable, we use the default properties from the bone's group
        // to avoid resetting other transformations. The offset is replaced if provided.
        TransformedItemStack defaultTis = bone.getGroup().getItemStack();
        Vector3f offset = (localOffset != null) ? localOffset : defaultTis.offset();

        TransformedItemStack newTis = new TransformedItemStack(
                defaultTis.position(),
                offset,
                defaultTis.scale(),
                itemStack
        );

        if (bone.itemStack(BonePredicate.TRUE, newTis)) {
            tracker.forceUpdate(true);
        }
    }

    /**
     * Sets the scale of the bone's displayed item.
     * This modifies the base scale of the item, which is then multiplied by animation scales.
     *
     * @param scale A Vector3f representing the new scale (x, y, z).
     */
    public void setScale(Vector3f scale) {
        // Since RenderedBone#getItemStack is unavailable, we use the default item and offset
        // from the bone's group to avoid losing the item entirely when scaling.
        TransformedItemStack defaultTis = bone.getGroup().getItemStack();
        TransformedItemStack newTis = new TransformedItemStack(
                defaultTis.position(),
                defaultTis.offset(),
                scale,
                defaultTis.itemStack()
        );
        if (bone.itemStack(BonePredicate.TRUE, newTis)) {
            tracker.forceUpdate(true);
        }
    }
}
