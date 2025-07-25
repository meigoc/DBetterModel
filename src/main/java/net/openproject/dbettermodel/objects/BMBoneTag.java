package net.openproject.dbettermodel.objects;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BMBoneTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMBoneTag
    // @prefix bmbone
    // @base ElementTag
    // @format
    // The identity format for a BMBoneTag is <uuid>,<model_name>,<bone_name>
    // For example: 'bmbone@dfc67056-b15d-45dd-b239-482d92e482e5,dummy,head'.
    //
    // @plugin DBetterModel
    // @description
    // Represents a single bone within a specific model instance on an entity.
    //
    // -->

    public static final String PREFIX = "bmbone";

    public static BMBoneTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("bmbone")
    public static BMBoneTag valueOf(String string, TagContext context) {
        if (string == null) return null;
        String lower = CoreUtilities.toLowerCase(string);

        if (!lower.startsWith(PREFIX + "@")) return null;
        String body = string.substring(PREFIX.length() + 1);
        
        String[] parts = body.split(",", 3);
        if (parts.length < 3) return null;

        String entityId = parts[0];
        String modelName = parts[1];
        String boneName = parts[2];

        EntityTag entityTag = EntityTag.valueOf(entityId, context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        return BetterModel.registry(entityTag.getBukkitEntity())
                .map(registry -> registry.tracker(modelName))
                .map(tracker -> Optional.ofNullable(tracker.bone(boneName))
                        .map(bone -> new BMBoneTag(tracker, bone))
                        .orElse(null))
                .orElse(null);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    private final EntityTracker tracker;
    private final RenderedBone bone;

    public BMBoneTag(EntityTracker tracker, RenderedBone bone) {
        this.tracker = tracker;
        this.bone = bone;
    }

    public RenderedBone getBone() { return bone; }

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { this.prefix = s; return this; }
    @Override public boolean isUnique() { return true; }

    @Override
    public String identify() {
        return PREFIX + "@"
                + tracker.registry().uuid()
                + "," + tracker.name()
                + "," + bone.getName().name();
    }

    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return bone; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMBoneTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMBoneTag.name>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the name of the bone.
        // -->
        tagProcessor.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.getBone().getName().name())
        );

        // <--[tag]
        // @attribute <BMBoneTag.global_position>
        // @returns LocationTag
        // @plugin DBetterModel
        // @description
        // Returns the bone's current position in the world.
        // -->
        tagProcessor.registerTag(LocationTag.class, "global_position", (attr, obj) -> {
            Vector3f worldPos = obj.getBone().worldPosition();
            Vector pos = new Vector(worldPos.x(), worldPos.y(), worldPos.z());
            return new LocationTag(obj.tracker.sourceEntity().getWorld(), pos.getX(), pos.getY(), pos.getZ());
        });

        // <--[tag]
        // @attribute <BMBoneTag.is_visible>
        // @returns ElementTag(Boolean)
        // @plugin DBetterModel
        // @description
        // Returns whether the bone is currently visible.
        // -->
        tagProcessor.registerTag(ElementTag.class, "is_visible", (attr, obj) ->
                new ElementTag(obj.getBone().isVisible())
        );

        // <--[tag]
        // @attribute <BMBoneTag.bm_model>
        // @returns BMModelTag
        // @plugin DBetterModel
        // @description
        // Returns the parent model of this bone.
        // -->
        tagProcessor.registerTag(BMModelTag.class, "bm_model", (attr, obj) ->
                new BMModelTag(obj.tracker)
        );
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override public void adjust(Mechanism mechanism) {
        // <--[mechanism]
        // @object BMBoneTag
        // @name tint
        // @input ElementTag(Integer)
        // @plugin DBetterModel
        // @description
        // Applies a color tint to the bone's item. The color is specified as a single integer representing the RGB value.
        // For example, red is 16711680.
        // @tags
        // <BMBoneTag.tint>
        // -->
        if (mechanism.matches("tint") && mechanism.requireInteger()) {
            int color = mechanism.getValue().asInt();
            if (bone.tint(BonePredicate.TRUE, color)) {
                tracker.forceUpdate(true);
            }
        }

        // <--[mechanism]
        // @object BMBoneTag
        // @name visible
        // @input ElementTag(Boolean) or ListTag
        // @plugin DBetterModel
        // @description
        // Sets the visibility of the bone.
        // To change visibility for specific players, provide a ListTag where the first element is the boolean (true/false)
        // and the following elements are the PlayerTags.
        // Note: Per-player visibility is temporary and may reset if the player reloads the model.
        // @tags
        // <BMBoneTag.is_visible>
        // @example
        // # Hide the 'helmet' bone for everyone
        // - adjust <[my_model].bone[helmet]> visible:false
        // @example
        // # Show the 'sword' bone only to a specific player
        // - adjust <[my_model].bone[sword]> visible:<list[true|<player>]>
        // @example
        // # Hide the 'cape' bone for a list of players
        // - adjust <[my_model].bone[cape]> visible:<list[false|<server.online_players.exclude[<player>]>]>
        // -->
        if (mechanism.matches("visible")) {
            boolean visible;
            ListTag targets = null;

            if (mechanism.value.canBeType(ListTag.class)) {
                ListTag list = mechanism.valueAsType(ListTag.class);
                if (list.isEmpty() || !list.getObject(0).canBeType(ElementTag.class) || !list.getObject(0).asElement().isBoolean()) {
                    mechanism.echoError("If using a ListTag for 'visible', the first element must be a boolean (true/false).");
                    return;
                }
                visible = list.getObject(0).asElement().asBoolean();
                targets = new ListTag(list.subList(1, list.size()));
            } else if (mechanism.requireBoolean()) {
                visible = mechanism.getValue().asBoolean();
            } else {
                return;
            }

            if (targets == null || targets.isEmpty()) {
                if (bone.togglePart(BonePredicate.TRUE, visible)) {
                    tracker.forceUpdate(true);
                }
            } else {
                ModelDisplay display = bone.getDisplay();
                if (display == null) {
                    Debug.echoError("Cannot get ModelDisplay for this bone. Per-player visibility is not possible.");
                    return;
                }

                List<Player> players = targets.filter(PlayerTag.class, mechanism.context)
                        .stream()
                        .map(PlayerTag::getPlayerEntity)
                        .collect(Collectors.toList());

                if (players.isEmpty()) return;

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
        }

        // <--[mechanism]
        // @object BMBoneTag
        // @name item
        // @input ListTag
        // @plugin DBetterModel
        // @description
        // Sets the item displayed by this bone.
        // The input must be a ListTag. The first element must be a valid ItemTag.
        // Optionally, a second element can be a LocationTag to specify a local offset.
        // @example
        // # Set the bone to display a diamond sword
        // - adjust <[my_bone]> item:<list[<item[diamond_sword]>]>
        // @example
        // # Set the bone to display a stone block, shifted up by 0.5 blocks
        // - adjust <[my_bone]> item:<list[<item[stone]>|<location[0,0.5,0]>]>
        // -->
        if (mechanism.matches("item") && mechanism.requireObject("Mechanism 'item' must have a ListTag value.", ListTag.class)) {
            ListTag list = mechanism.valueAsType(ListTag.class);
            if (list.isEmpty()) {
                Debug.echoError("The ListTag for 'item' mechanism must not be empty.");
                return;
            }
            ItemTag itemTag = list.getObject(0).asType(ItemTag.class, mechanism.context);
            if (itemTag == null) {
                Debug.echoError("The first element of the list must be a valid ItemTag.");
                return;
            }

            Vector3f localOffset = new Vector3f(0f, 0f, 0f);
            if (list.size() > 1) {
                LocationTag loc = list.getObject(1).asType(LocationTag.class, mechanism.context);
                if (loc == null) {
                    Debug.echoError("The second element of the list, if present, must be a LocationTag for the offset.");
                    return;
                }
                localOffset.set((float) loc.getX(), (float) loc.getY(), (float) loc.getZ());
            }

            TransformedItemStack tis = new TransformedItemStack(
                    new Vector3f(0f, 0f, 0f),
                    localOffset,
                    new Vector3f(1f, 1f, 1f),
                    itemTag.getItemStack()
            );

            if (bone.itemStack(BonePredicate.TRUE, tis)) {
                tracker.forceUpdate(true);
            }
        }

        tagProcessor.processMechanism(this, mechanism);
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMBoneTag!");
    }
}
