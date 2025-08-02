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
import com.denizenscript.denizencore.objects.core.QuaternionTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.tracker.EntityTracker;
import net.openproject.dbettermodel.api.BMBone;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BMBoneTag implements ObjectTag, Adjustable {

    // <--
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
    private final BMBone boneApi;

    public BMBoneTag(EntityTracker tracker, RenderedBone bone) {
        this.tracker = tracker;
        this.bone = bone;
        this.boneApi = BMBone.getOrCreate(tracker, bone);
    }

    public RenderedBone getBone() {
        return bone;
    }

    private String prefix = PREFIX;

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ObjectTag setPrefix(String s) {
        this.prefix = s;
        return this;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String identify() {
        return PREFIX + "@" + tracker.registry().uuid() + "," + tracker.name() + "," + bone.getName().name();
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public Object getJavaObject() {
        return bone;
    }

    @Override
    public String toString() {
        return identify();
    }

    public static final ObjectTagProcessor<BMBoneTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMBoneTag.name>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the name of the bone.
        // -->
        tagProcessor.registerTag(ElementTag.class, "name", (attr, obj) -> new ElementTag(obj.getBone().getName().name())
        );

        // <--[tag]
        // @attribute <BMBoneTag.global_position>
        // @returns LocationTag
        // @plugin DBetterModel
        // @description
        // Returns the bone's current position in the world.
        // @deprecated Use world_location instead.
        // -->
        tagProcessor.registerTag(LocationTag.class, "global_position", (attr, obj) -> {
            Vector3f worldPos = obj.getBone().worldPosition();
            Vector pos = new Vector(worldPos.x(), worldPos.y(), worldPos.z());
            return new LocationTag(obj.tracker.sourceEntity().getWorld(), pos.getX(), pos.getY(), pos.getZ());
        });

        // <--[tag]
        // @attribute <BMBoneTag.world_location>
        // @returns LocationTag
        // @plugin DBetterModel
        // @description
        // Returns the bone's precise, real-time location in the world.
        // This position accounts for all entity and animation transformations.
        // -->
        tagProcessor.registerTag(LocationTag.class, "world_location", (attr, obj) -> {
            Vector3f worldPos = obj.getBone().worldPosition();
            return new LocationTag(obj.tracker.sourceEntity().getWorld(), worldPos.x(), worldPos.y(), worldPos.z());
        });

        // <--[tag]
        // @attribute <BMBoneTag.world_rotation>
        // @returns QuaternionTag
        // @plugin DBetterModel
        // @description
        // Returns the bone's precise, real-time rotation in world space as a Quaternion.
        // This rotation accounts for all entity and animation transformations.
        // -->
        tagProcessor.registerTag(QuaternionTag.class, "world_rotation", (attr, obj) -> {
            Quaternionf worldRot = obj.getBone().worldRotation();
            return new QuaternionTag(worldRot.x, worldRot.y, worldRot.z, worldRot.w);
        });

        // <--[tag]
        // @attribute <BMBoneTag.is_visible>
        // @returns ElementTag(Boolean)
        // @plugin DBetterModel
        // @description
        // Returns whether the bone is currently visible.
        // -->
        tagProcessor.registerTag(ElementTag.class, "is_visible", (attr, obj) -> new ElementTag(obj.getBone().isVisible())
        );

        // <--[tag]
        // @attribute <BMBoneTag.bm_model>
        // @returns BMModelTag
        // @plugin DBetterModel
        // @description
        // Returns the parent model of this bone.
        // -->
        tagProcessor.registerTag(BMModelTag.class, "bm_model", (attr, obj) -> new BMModelTag(obj.tracker)
        );
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void adjust(Mechanism mechanism) {
        // All mechanisms are handled by the BMBone API class for cleanliness.
        // The BMBone class abstracts away direct calls to the tracker and bone.

        // <--[mechanism]
        // @object BMBoneTag
        // @name tint
        // @input ElementTag(Integer)
        // @plugin DBetterModel
        // @description
        // Applies a color tint to the bone's item.
        // The color is specified as a single integer representing the RGB value.
        // For example, red is 16711680.
        // @tags
        // <BMBoneTag.tint>
        // -->
        if (mechanism.matches("tint") && mechanism.requireInteger()) {
            boneApi.setTint(mechanism.getValue().asInt());
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
                boneApi.setVisible(visible);
            } else {
                List<Player> players = targets.filter(PlayerTag.class, mechanism.context)
                        .stream()
                        .map(PlayerTag::getPlayerEntity)
                        .collect(Collectors.toList());
                if (!players.isEmpty()) {
                    boneApi.setVisible(visible, players);
                }
            }
        }

        // <--[mechanism]
        // @object BMBoneTag
        // @name item
        // @input ItemTag
        // @plugin DBetterModel
        // @description
        // Sets the item displayed by this bone, preserving its current offset and scale.
        // @example
        // # Set the bone to display a diamond sword
        // - adjust <[my_bone]> item:<item[diamond_sword]>
        // -->
        if (mechanism.matches("item") && mechanism.requireObject(ItemTag.class)) {
            ItemTag itemTag = mechanism.valueAsType(ItemTag.class);
            boneApi.setItem(itemTag.getItemStack());
        }

        // <--[mechanism]
        // @object BMBoneTag
        // @name offset
        // @input LocationTag
        // @plugin DBetterModel
        // @description
        // Sets the local offset for the bone's displayed item, preserving its current item and scale.
        // The input is a LocationTag interpreted as a vector.
        // @example
        // # Shift the displayed item up by 0.5 blocks
        // - adjust <[my_bone]> offset:<location[0,0.5,0]>
        // -->
        if (mechanism.matches("offset") && mechanism.requireObject(LocationTag.class)) {
            LocationTag loc = mechanism.valueAsType(LocationTag.class);
            Vector3f offsetVector = new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ());
            boneApi.setOffset(offsetVector);
        }

        // <--[mechanism]
        // @object BMBoneTag
        // @name scale
        // @input LocationTag
        // @plugin DBetterModel
        // @description
        // Adjusts the scale of the bone. Input is a LocationTag representing a vector.
        // For example, a scale of (2, 1, 1) will double the bone's width (X-axis).
        // This sets the base scale and will be multiplied by any animation scales.
        // @example
        // # Make a bone twice as wide
        // - adjust <[my_bone]> scale:<location[2,1,1]>
        // -->
        if (mechanism.matches("scale") && mechanism.requireObject(LocationTag.class)) {
            LocationTag loc = mechanism.valueAsType(LocationTag.class);
            Vector3f scaleVector = new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ());
            boneApi.setScale(scaleVector);
        }

        // <--[mechanism]
        // @object BMBoneTag
        // @name rotate
        // @input QuaternionTag
        // @plugin DBetterModel
        // @description
        // Sets an additional rotation for the bone, which is applied on top of its current animation.
        // Each use of this mechanism replaces the previous rotation value, it does not add to it.
        // This allows for dynamic, script-controlled rotation independent of predefined animations.
        // @example
        // # Rotate a bone 45 degrees around the world's Y (up/down) axis.
        // - adjust <[my_bone]> rotate:<quaternion[0,0.382,0,0.923]>
        // -->
        if (mechanism.matches("rotate") && mechanism.requireObject(QuaternionTag.class)) {
            QuaternionTag quatTag = mechanism.valueAsType(QuaternionTag.class);
            boneApi.setRotation(new Quaternionf((float) quatTag.x, (float) quatTag.y, (float) quatTag.z, (float) quatTag.w));
        }

        // <--[mechanism]
        // @object BMBoneTag
        // @name view_range
        // @input ElementTag(Decimal)
        // @plugin DBetterModel
        // @description
        // Sets the view range (render distance) for this specific bone in blocks.
        // This is useful for hiding parts of a model in F5 view by setting a small view range (e.g., 1.0 or 1.5).
        // @example
        // # Make the 'head' bone only visible from very close up
        // - adjust <[my_model].bone[head]> view_range:1.5
        // -->
        if (mechanism.matches("view_range") && mechanism.requireFloat()) {
            boneApi.setViewRange(mechanism.getValue().asFloat());
        }

        tagProcessor.processMechanism(this, mechanism);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMBoneTag!");
    }
}