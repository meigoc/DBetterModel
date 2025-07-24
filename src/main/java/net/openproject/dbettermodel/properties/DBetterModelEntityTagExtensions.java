package net.openproject.dbettermodel.properties;

import com.denizenscript.denizen.objects.EntityTag;
import kr.toxicity.model.api.BetterModel;
import net.openproject.dbettermodel.objects.BMEntityTag;

public class DBetterModelEntityTagExtensions {

    public static void register() {
        // <--[tag]
        // @attribute <EntityTag.bm_entity>
        // @returns BMEntityTag
        // @plugin DBetterModel
        // @description
        // Returns the BMEntityTag of the entity, if it has any BetterModel models.
        // This provides access to all models and their properties on the entity.
        // Returns null if the entity has no models.
        // -->
        EntityTag.tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attribute, entity) ->
                BetterModel.registry(entity.getBukkitEntity())
                        .map(BMEntityTag::new)
                        .orElse(null)
        );
    }
}
