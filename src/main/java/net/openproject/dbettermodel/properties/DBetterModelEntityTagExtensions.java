package net.openproject.dbettermodel.properties;

import com.denizenscript.denizen.objects.EntityTag;
import net.openproject.dbettermodel.objects.BMEntityTag;

public class DBetterModelEntityTagExtensions {

    public static BMEntityTag getModeledEntity(EntityTag entity) {
        return new BMEntityTag(entity.getBukkitEntity());
    }

    public static void register() {
        // <--[tag]
        // @attribute <EntityTag.bm_entity>
        // @returns BMEntityTag
        // @plugin DBetterModel
        // @description
        // Returns the BMEntity of the entity, if any.
        // -->
        EntityTag.tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attribute, entity) -> {
            return getModeledEntity(entity);
        });
    }
}
