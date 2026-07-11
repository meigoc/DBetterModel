/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.properties;

import com.denizenscript.denizen.objects.EntityTag;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.denizen.objects.BMEntityTag;

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
        EntityTag.tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attribute, entity) -> {
            BmPlatform platform = DBetterModel.platform();
            if (platform == null || !platform.isModeled(entity.getBukkitEntity())) {
                return null;
            }
            return new BMEntityTag(entity.getBukkitEntity());
        });
    }
}
