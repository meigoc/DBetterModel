/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.objects;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.DurationTag;
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
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.services.ModelService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;

public class BMModelTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMModelTag
    // @prefix bmmodel
    // @base ElementTag
    // @format
    // The identity format for a BMModelTag is the UUID of the base entity, a comma, and the model name.
    // For example: 'bmmodel@dfc67056-b15d-45dd-b239-482d92e482e5,demon_knight'.
    // Models summoned with bmsummon have no entity and identify as 'bmmodel@dummy:<id>,<model>' (non-persistent).
    //
    // @plugin DBetterModel
    // @description
    // Represents a specific model instance (tracker) attached to an entity, or bound to a location via bmsummon.
    //
    // -->

    public static final String PREFIX = "bmmodel";

    @Fetchable("bmmodel")
    public static BMModelTag valueOf(String string, TagContext context) {
        if (string == null) return null;
        String lower = CoreUtilities.toLowerCase(string);
        if (!lower.startsWith(PREFIX + "@")) return null;
        String body = string.substring(PREFIX.length() + 1);

        String[] parts = body.split(",", 2);
        if (parts.length < 2) return null;

        // Dummy (location-bound) form: bmmodel@dummy:<id>,<model> — resolved via the core registry.
        if (CoreUtilities.toLowerCase(parts[0]).startsWith("dummy:")) {
            String id = parts[0].substring("dummy:".length());
            BmTracker dummy = ModelService.getInstance().dummyTracker(id);
            if (dummy == null || !CoreUtilities.equalsIgnoreCase(dummy.name(), parts[1])) return null;
            return new BMModelTag(dummy);
        }

        EntityTag entityTag = EntityTag.valueOf(parts[0], context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        BmPlatform platform = DBetterModel.platform();
        if (platform == null) return null;
        return platform.tracker(entityTag.getBukkitEntity(), parts[1])
                .map(BMModelTag::new)
                .orElse(null);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    private final BmTracker tracker;

    public BMModelTag(BmTracker tracker) {
        this.tracker = tracker;
    }

    public BmTracker getTracker() { return tracker; }

    private String prefix = PREFIX;
    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { this.prefix = s;
        return this; }
    @Override public boolean isUnique() { return true; }
    @Override public String identify() {
        UUID uuid = tracker.entityUuid();
        if (uuid == null) {
            return PREFIX + "@dummy:" + ModelService.getInstance().registerDummy(tracker) + "," + tracker.name();
        }
        return PREFIX + "@" + uuid + "," + tracker.name();
    }
    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return tracker; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMModelTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {

        EntityTag.tagProcessor.registerTag(BMModelTag.class, "bm_model", (attr, obj) -> {
            if (!attr.hasContext(1)) {
                attr.echoError("The bm_model tag must specify a model name!");
                return null;
            }
            String modelName = attr.getContext(1);
            BmPlatform platform = DBetterModel.platform();
            if (platform == null) return null;
            return platform.tracker(obj.getBukkitEntity(), modelName)
                    .map(BMModelTag::new)
                    .orElse(null);
        });

        // <--[tag]
        // @attribute <BMModelTag.bm_entity>
        // @returns BMEntityTag
        // @plugin DBetterModel
        // @description
        // Returns the parent BMEntityTag of this model.
        // -->
        tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attr, obj) -> {
            UUID uuid = obj.getTracker().entityUuid();
            Entity entity = uuid == null ? null : Bukkit.getEntity(uuid);
            return entity == null ? null : new BMEntityTag(entity);
        });

        // <--[tag]
        // @attribute <BMModelTag.name>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the name of the model.
        // -->
        tagProcessor.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.getTracker().name())
        );

        tagProcessor.registerTag(MapTag.class, "bones", (attr, obj) -> {
            MapTag map = new MapTag();
            UUID uuid = obj.getTracker().entityUuid();
            String modelName = obj.getTracker().name();
            for (BmBone bone : obj.getTracker().bones()) {
                map.putObject(bone.name(), new BMBoneTag(uuid, modelName, bone.name()));
            }
            return map;
        });

        tagProcessor.registerTag(BMBoneTag.class, "bone", (attr, obj) -> {
            if (!attr.hasContext(1)) return null;
            String boneName = attr.getContext(1);
            return obj.getTracker().bone(boneName)
                    .map(bone -> new BMBoneTag(obj.getTracker().entityUuid(), obj.getTracker().name(), bone.name()))
                    .orElse(null);
        });

        tagProcessor.registerTag(DurationTag.class, "get_animation_duration", (attr, obj) -> {
            if (!attr.hasContext(1)) {
                attr.echoError("The get_animation_duration tag must have an animation name specified.");
                return null;
            }
            String animationName = attr.getContext(1);
            OptionalDouble length = obj.getTracker().model().animationLength(animationName);
            return length.isPresent() ? new DurationTag(length.getAsDouble()) : null;
        });

        tagProcessor.registerTag(ListTag.class, "animations", (attr, obj) -> {
            Set<String> animationNames = obj.getTracker().model().animations();
            return new ListTag(animationNames);
        });

        // <--[tag]
        // @attribute <BMModelTag.viewers>
        // @returns ListTag(PlayerTag)
        // @plugin DBetterModel
        // @description
        // Returns the list of players currently seeing this model.
        // -->
        tagProcessor.registerTag(ListTag.class, "viewers", (attr, obj) -> {
            ListTag list = new ListTag();
            for (var player : obj.getTracker().viewers()) {
                list.addObject(new com.denizenscript.denizen.objects.PlayerTag(player));
            }
            return list;
        });

        // <--[tag]
        // @attribute <BMModelTag.running_animations>
        // @returns ListTag
        // @plugin DBetterModel
        // @description
        // Returns the names of animations currently playing on this model's tracker pipeline.
        // The compat api exposes at most one pipeline animation, so the list has zero or one entries.
        // -->
        tagProcessor.registerTag(ListTag.class, "running_animations", (attr, obj) -> {
            ListTag list = new ListTag();
            obj.getTracker().runningAnimation().ifPresent(anim -> list.addObject(new ElementTag(anim.name())));
            return list;
        });
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void adjust(Mechanism mechanism) {
        if (mechanism.matches("force_update")) {
            if (!mechanism.hasValue() || mechanism.requireBoolean()) {
                tracker.forceUpdate();
            }
            return;
        }
        // Bone mechanisms fan out over every bone but fulfill exactly once (tech-debt item 4).
        if (ModelService.getInstance().isBoneMechanism(mechanism.getName())) {
            if (ModelService.getInstance().adjustAllBones(tracker, mechanism)) {
                mechanism.fulfill();
            }
            return;
        }
        tagProcessor.processMechanism(this, mechanism);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMModelTag!");
    }
}
