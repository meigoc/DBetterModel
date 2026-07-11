/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.objects;

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
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.services.ModelService;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public class BMBoneTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMBoneTag
    // @prefix bmbone
    // @base ElementTag
    // @format
    // The identity format for a BMBoneTag is the UUID of the base entity, the model name, and the bone name, comma-separated.
    // For example: 'bmbone@dfc67056-b15d-45dd-b239-482d92e482e5,demon_knight,head'.
    //
    // @plugin DBetterModel
    // @description
    // Represents a single bone within a specific model instance.
    //
    // -->

    public static final String PREFIX = "bmbone";

    private final UUID entityUUID;
    private final String modelName;
    private final String boneName;

    public BMBoneTag(UUID entityUUID, String modelName, String boneName) {
        this.entityUUID = entityUUID;
        this.modelName = modelName;
        this.boneName = boneName;
    }

    @Fetchable("bmbone")
    public static BMBoneTag valueOf(String string, TagContext context) {
        if (string == null) return null;
        String lower = CoreUtilities.toLowerCase(string);
        if (!lower.startsWith(PREFIX + "@")) return null;
        String body = string.substring(PREFIX.length() + 1);

        String[] parts = body.split(",", 3);
        if (parts.length < 3) return null;

        EntityTag entityTag = EntityTag.valueOf(parts[0], context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        return new BMBoneTag(entityTag.getUUID(), parts[1], parts[2]);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    public UUID getEntityUUID() { return entityUUID; }
    public String getModelName() { return modelName; }
    public String getBoneName() { return boneName; }

    private String prefix = PREFIX;
    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { this.prefix = s;
        return this; }
    @Override public boolean isUnique() { return true; }
    @Override public String identify() { return PREFIX + "@" + entityUUID + "," + modelName + "," + boneName; }
    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return this; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMBoneTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        tagProcessor.registerTag(ElementTag.class, "name", (attr, obj) -> new ElementTag(obj.boneName));

        tagProcessor.registerTag(LocationTag.class, "world_location", (attr, obj) -> {
            Location loc = obj.getAbsoluteLocation();
            return loc != null ? new LocationTag(loc) : null;
        });

        tagProcessor.registerTag(LocationTag.class, "world_rotation_euler", (attr, obj) -> {
            Vector3f euler = ModelService.getInstance().getBoneWorldRotationEuler(obj.entityUUID, obj.modelName, obj.boneName);
            return euler != null ? new LocationTag(null, euler.x, euler.y, euler.z) : null;
        });

        // <--[tag]
        // @attribute <BMBoneTag.world_rotation>
        // @returns LocationTag
        // @plugin DBetterModel
        // @deprecated Use 'world_rotation_euler' — pre-4.0.0 alias, kept for legacy scripts.
        // @description
        // Deprecated alias of <@link tag BMBoneTag.world_rotation_euler>.
        // -->
        tagProcessor.registerTag(LocationTag.class, "world_rotation", (attr, obj) -> {
            Vector3f euler = ModelService.getInstance().getBoneWorldRotationEuler(obj.entityUUID, obj.modelName, obj.boneName);
            return euler != null ? new LocationTag(null, euler.x, euler.y, euler.z) : null;
        });

        // <--[tag]
        // @attribute <BMBoneTag.bm_entity>
        // @returns BMEntityTag
        // @plugin DBetterModel
        // @deprecated Use 'bm_model.bm_entity' — 2.x alias dropped in 4.x, restored for legacy scripts.
        // @description
        // Deprecated alias: returns the parent BMEntityTag of this bone's model.
        // Returns null for bones of dummy (location-bound) trackers.
        // -->
        tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attr, obj) -> {
            org.bukkit.entity.Entity entity = obj.entityUUID == null ? null : org.bukkit.Bukkit.getEntity(obj.entityUUID);
            return entity == null ? null : new BMEntityTag(entity);
        });

        tagProcessor.registerTag(ElementTag.class, "is_visible", (attr, obj) ->
                new ElementTag(ModelService.getInstance().isBoneVisible(obj.entityUUID, obj.modelName, obj.boneName))
        );

        tagProcessor.registerTag(BMModelTag.class, "bm_model", (attr, obj) -> {
            BmPlatform platform = DBetterModel.platform();
            if (platform == null) return null;
            return platform.tracker(obj.entityUUID, obj.modelName)
                    .map(BMModelTag::new)
                    .orElse(null);
        });

        tagProcessor.registerTag(LocationTag.class, "local_position", (attr, obj) -> {
            Location offset = ModelService.getInstance().getBoneWorldLocation(obj.entityUUID, obj.modelName, obj.boneName);
            return offset != null ? new LocationTag(offset) : null;
        });

        tagProcessor.registerTag(LocationTag.class, "global_position", (attr, obj) -> {
            Location loc = obj.getAbsoluteLocation();
            return loc != null ? new LocationTag(loc) : null;
        });

        tagProcessor.registerTag(LocationTag.class, "real_position", (attr, obj) -> {
            Location loc = obj.getRealWorldLocation();
            return loc != null ? new LocationTag(loc) : null;
        });

        // BetterModel exposes no display-state reads, so the tags below return the last
        // value set through DBetterModel (or the documented default before the first write).

        // <--[tag]
        // @attribute <BMBoneTag.glow>
        // @returns ElementTag(Boolean)
        // @mechanism BMBoneTag.glow
        // @plugin DBetterModel
        // @description
        // Whether the bone glows. Reflects the last value set through DBetterModel; false before the first write.
        // -->
        tagProcessor.registerTag(ElementTag.class, "glow", (attr, obj) ->
                fromBone(obj, bone -> new ElementTag(bone.getGlow())));

        // <--[tag]
        // @attribute <BMBoneTag.glow_color>
        // @returns ElementTag(Number)
        // @mechanism BMBoneTag.glow_color
        // @plugin DBetterModel
        // @description
        // The bone's glow color as an RGB integer. Reflects the last value set through DBetterModel; white (16777215) before the first write.
        // -->
        tagProcessor.registerTag(ElementTag.class, "glow_color", (attr, obj) ->
                fromBone(obj, bone -> new ElementTag(bone.getGlowColor())));

        // <--[tag]
        // @attribute <BMBoneTag.tint>
        // @returns ElementTag(Number)
        // @mechanism BMBoneTag.tint
        // @plugin DBetterModel
        // @description
        // The bone's tint as an RGB integer. Reflects the last value set through DBetterModel; neutral white (16777215) before the first write.
        // -->
        tagProcessor.registerTag(ElementTag.class, "tint", (attr, obj) ->
                fromBone(obj, bone -> new ElementTag(bone.getTint())));

        // <--[tag]
        // @attribute <BMBoneTag.billboard>
        // @returns ElementTag
        // @mechanism BMBoneTag.billboard
        // @plugin DBetterModel
        // @description
        // The bone's billboard mode (FIXED/VERTICAL/HORIZONTAL/CENTER). Reflects the last value set through DBetterModel; FIXED before the first write.
        // -->
        tagProcessor.registerTag(ElementTag.class, "billboard", (attr, obj) ->
                fromBone(obj, bone -> new ElementTag(bone.getBillboard().name())));

        // <--[tag]
        // @attribute <BMBoneTag.view_range>
        // @returns ElementTag(Decimal)
        // @mechanism BMBoneTag.view_range
        // @plugin DBetterModel
        // @description
        // The bone display's view range. Reflects the last value set through DBetterModel; the display default 1.0 before the first write.
        // -->
        tagProcessor.registerTag(ElementTag.class, "view_range", (attr, obj) ->
                fromBone(obj, bone -> new ElementTag(bone.getViewRange())));

        // <--[tag]
        // @attribute <BMBoneTag.brightness>
        // @returns ListTag
        // @mechanism BMBoneTag.brightness
        // @plugin DBetterModel
        // @description
        // The bone's brightness override as a two-element list of block|sky light. Reflects the last value set through DBetterModel; 0|15 before the first write.
        // -->
        tagProcessor.registerTag(ListTag.class, "brightness", (attr, obj) ->
                fromBone(obj, bone -> {
                    ListTag list = new ListTag();
                    list.addObject(new ElementTag(bone.getBrightnessBlock()));
                    list.addObject(new ElementTag(bone.getBrightnessSky()));
                    return list;
                }));

        // <--[tag]
        // @attribute <BMBoneTag.shadow_radius>
        // @returns ElementTag(Decimal)
        // @mechanism BMBoneTag.shadow_radius
        // @plugin DBetterModel
        // @description
        // The bone display's shadow radius. Reflects the last value set through DBetterModel; 0 before the first write.
        // -->
        tagProcessor.registerTag(ElementTag.class, "shadow_radius", (attr, obj) ->
                fromBone(obj, bone -> new ElementTag(bone.getShadowRadius())));

        // <--[tag]
        // @attribute <BMBoneTag.scale>
        // @returns LocationTag
        // @mechanism BMBoneTag.scale
        // @plugin DBetterModel
        // @description
        // The bone item's scale as a vector. Reflects the last value set through DBetterModel; the model's own item scale before the first write.
        // -->
        tagProcessor.registerTag(LocationTag.class, "scale", (attr, obj) ->
                fromBone(obj, bone -> {
                    Vector3f scale = bone.getScale();
                    return new LocationTag(null, scale.x, scale.y, scale.z);
                }));

        // <--[tag]
        // @attribute <BMBoneTag.offset>
        // @returns LocationTag
        // @mechanism BMBoneTag.offset
        // @plugin DBetterModel
        // @description
        // The bone item's local offset as a vector. Reflects the last value set through DBetterModel; the model's own item offset before the first write.
        // -->
        tagProcessor.registerTag(LocationTag.class, "offset", (attr, obj) ->
                fromBone(obj, bone -> {
                    Vector3f offset = bone.getOffset();
                    return new LocationTag(null, offset.x, offset.y, offset.z);
                }));

        // <--[tag]
        // @attribute <BMBoneTag.item>
        // @returns ItemTag
        // @mechanism BMBoneTag.item
        // @plugin DBetterModel
        // @description
        // The item displayed by the bone. Reflects the last item set through DBetterModel; null before the first write.
        // -->
        tagProcessor.registerTag(ItemTag.class, "item", (attr, obj) ->
                fromBone(obj, bone -> {
                    ItemStack item = bone.getItem();
                    return item == null ? null : new ItemTag(item);
                }));

        // <--[tag]
        // @attribute <BMBoneTag.skin_parts>
        // @returns ListTag
        // @mechanism BMBoneTag.skin
        // @plugin DBetterModel
        // @description
        // The skin part names accepted by the skin mechanism (head, body, left_arm, ...).
        // -->
        tagProcessor.registerTag(ListTag.class, "skin_parts", (attr, obj) -> {
            BmPlatform platform = DBetterModel.platform();
            if (platform == null) return null;
            ListTag list = new ListTag();
            platform.playerLimbs().skinPartNames().forEach(list::add);
            return list;
        });
    }

    private static <T extends ObjectTag> T fromBone(BMBoneTag obj, java.util.function.Function<BmBone, T> reader) {
        BmPlatform platform = DBetterModel.platform();
        if (platform == null) return null;
        return platform.tracker(obj.entityUUID, obj.modelName)
                .flatMap(tracker -> tracker.bone(obj.boneName))
                .map(reader)
                .orElse(null);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    // <--[mechanism]
    // @object BMBoneTag
    // @name skin
    // @input MapTag
    // @plugin DBetterModel
    // @description
    // Applies a player's skin part to this bone, replacing its displayed texture.
    // Input is a MapTag with keys 'part' (a skin part name, see <@link tag BMBoneTag.skin_parts>)
    // and 'from' (the player whose skin provides the texture).
    // The skin is fetched asynchronously, so the bone updates a moment later.
    // Modern replacement for the bmpart command.
    // @tags
    // <BMBoneTag.skin_parts>
    // @example
    // # Put the linked player's head skin onto the knight's head bone.
    // - adjust <[knight].bm_entity.model[demon_knight].bone[head]> skin:[part=head;from=<player>]
    // -->
    private void adjustSkin(Mechanism mechanism) {
        BmPlatform platform = DBetterModel.platform();
        if (platform == null) {
            mechanism.echoError("DBetterModel compat layer is not available.");
            return;
        }
        MapTag map = mechanism.valueAsType(MapTag.class);
        if (map == null) {
            mechanism.echoError("The skin mechanism needs a MapTag input like [part=head;from=<player>].");
            return;
        }
        ObjectTag partObj = map.getObject("part");
        ObjectTag fromObj = map.getObject("from");
        PlayerTag from = fromObj == null ? null : fromObj.asType(PlayerTag.class, mechanism.context);
        if (partObj == null || from == null || from.getPlayerEntity() == null) {
            mechanism.echoError("The skin mechanism needs a 'part' name and an online 'from' player.");
            return;
        }
        UUID uuid = entityUUID;
        String part = partObj.toString();
        platform.playerLimbs().applySkinPart(
                () -> Optional.ofNullable(org.bukkit.Bukkit.getEntity(uuid)),
                modelName, boneName, part, from.getPlayerEntity()
        ).thenAccept(result -> {
            switch (result) {
                case SUCCESS, TARGET_GONE -> { }
                case MODEL_NOT_FOUND -> Debug.echoError("skin mechanism: model '" + modelName + "' not found on the entity.");
                case BONE_NOT_FOUND -> Debug.echoError("skin mechanism: bone '" + boneName + "' not found or is a dummy bone.");
                case INVALID_PART -> Debug.echoError("skin mechanism: invalid part name '" + part + "'.");
            }
        }).exceptionally(e -> {
            Debug.echoError("skin mechanism: failed to load skin for " + from.getName() + ": " + e.getMessage());
            return null;
        });
    }

    @Override
    public void adjust(Mechanism mechanism) {
        if (mechanism.matches("skin")) {
            adjustSkin(mechanism);
            mechanism.fulfill();
            return;
        }
        ModelService.getInstance().adjustBone(this, mechanism);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMBoneTag!");
    }

    public Location getRealWorldLocation() {
        org.bukkit.entity.Entity bukkitEntity = org.bukkit.Bukkit.getEntity(this.entityUUID);
        if (bukkitEntity == null) return null;

        BmPlatform platform = DBetterModel.platform();
        if (platform == null) return null;
        BmBone bone = platform.tracker(this.entityUUID, this.modelName)
                .flatMap(tracker -> tracker.bone(this.boneName))
                .orElse(null);
        if (bone == null) return null;

        Vector3f pos = bone.worldPosition();
        Quaternionf rot = bone.hitboxViewRotation();

        Vector3f direction = new Vector3f(0, 0, 1);
        direction.rotate(rot);
        Location loc = bukkitEntity.getLocation().add(pos.x, pos.y, pos.z);

        loc.setDirection(new Vector(direction.x, direction.y, direction.z));

        return loc;
    }

    private Location getAbsoluteLocation() {
        Location offset = ModelService.getInstance().getBoneWorldLocation(this.entityUUID, this.modelName, this.boneName);
        if (offset == null) return null;
        org.bukkit.entity.Entity bukkitEntity = org.bukkit.Bukkit.getEntity(this.entityUUID);
        if (bukkitEntity == null) return null;

        Location entityLoc = bukkitEntity.getLocation();
        Vector vec = new Vector(offset.getX(), offset.getY(), offset.getZ());
        vec.rotateAroundY(Math.toRadians(-entityLoc.getYaw()));
        return entityLoc.add(vec);
    }
}
