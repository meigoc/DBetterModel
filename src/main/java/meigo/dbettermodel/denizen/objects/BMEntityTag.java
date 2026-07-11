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
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.entity.Entity;

import java.util.Optional;

public class BMEntityTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMEntityTag
    // @prefix bmentity
    // @base ElementTag
    // @format
    // The identity format for a BMEntityTag is the UUID of the base entity.
    // For example: 'bmentity@dfc67056-b15d-45dd-b239-482d92e482e5'.
    //
    // @plugin DBetterModel
    // @description
    // Represents an entity that has one or more BetterModel models attached to it.
    //
    // -->

    public static final String PREFIX = "bmentity";

    public static BMEntityTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("bmentity")
    public static BMEntityTag valueOf(String string, TagContext context) {
        if (string == null) return null;

        String lower = CoreUtilities.toLowerCase(string);
        if (!lower.startsWith(PREFIX + "@")) return null;

        String uuidPart = lower.substring(PREFIX.length() + 1);
        EntityTag entity = EntityTag.valueOf(uuidPart, context);
        if (entity == null || entity.getBukkitEntity() == null) return null;

        BmPlatform platform = DBetterModel.platform();
        if (platform == null || !platform.isModeled(entity.getBukkitEntity())) return null;
        return new BMEntityTag(entity.getBukkitEntity());
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    private final Entity entity;

    public BMEntityTag(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { this.prefix = s;
                                                    return this; }
    @Override public boolean isUnique() { return true; }

    @Override
    public String identify() {
        return PREFIX + "@" + entity.getUniqueId();
    }

    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return entity; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMEntityTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMEntityTag.base_entity>
        // @returns EntityTag
        // @plugin DBetterModel
        // @description
        // Returns the base Bukkit entity.
        // -->
        tagProcessor.registerTag(EntityTag.class, "base_entity", (attr, obj) ->
                new EntityTag(obj.getEntity())
        );

        // <--[tag]
        // @attribute <BMEntityTag.model[(<model_name>)]>
        // @returns BMModelTag
        // @plugin DBetterModel
        // @description
        // Returns the model with the specified name on the entity.
        // If no name is provided, returns the first model loaded on the entity.
        // -->
        tagProcessor.registerTag(BMModelTag.class, "model", (attr, obj) -> {
            BmPlatform platform = DBetterModel.platform();
            if (platform == null) return null;
            BmTracker tracker;
            if (attr.hasContext(1)) {
                String modelName = attr.getContext(1);
                tracker = platform.tracker(obj.getEntity(), modelName).orElse(null);
                if (tracker == null) {
                    attr.echoError("The entity does not have a model named '" + modelName + "'.");
                    return null;
                }
            } else {
                Optional<BmTracker> first = platform.trackers(obj.getEntity()).stream().findFirst();
                if (first.isEmpty()) {
                    attr.echoError("The entity has a model registry but no models are currently loaded.");
                    return null;
                }
                tracker = first.get();
            }
            return new BMModelTag(tracker);
        });
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override public void adjust(Mechanism mech) {
        tagProcessor.processMechanism(this, mech);
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMEntityTag!");
    }
}
