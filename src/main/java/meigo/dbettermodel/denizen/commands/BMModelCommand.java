/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.util.DBMDebug;
import org.bukkit.entity.Entity;

public class BMModelCommand extends AbstractCommand {

    public BMModelCommand() {
        setName("bmmodel");
        setSyntax("bmmodel entity:<entity> model:<model> (remove)");
        autoCompile();
    }

    // <--[command]
    // @Name BMModel
    // @Syntax bmmodel entity:<entity> model:<model> (remove)
    // @Required 2
    // @Short Adds or removes a model from an entity.
    // @Group DBetterModel
    //
    // @Description
    // Adds or removes a specific model from an entity. This is necessary for entities that can have multiple models.
    //
    // @Tags
    // <EntityTag.bm_entity>
    // <BMEntityTag.model[<name>]>
    //
    // @Usage
    // Use to add a model to an entity.
    // - bmmodel entity:<context.entity> model:demon_knight
    //
    // @Usage
    // Use to remove a model from an entity.
    // - bmmodel entity:<context.entity> model:demon_knight remove
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        BmPlatform platform = DBetterModel.platform();
        if (platform != null) {
            tab.addWithPrefix("model:", platform.modelNames());
        }
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag model,
                                   @ArgName("remove") boolean remove) {

        BmPlatform platform = DBetterModel.platform();
        if (platform == null) {
            DBMDebug.error(scriptEntry, "DBetterModel compat layer is not available.");
            return;
        }
        Entity entity = entityTag.getBukkitEntity();
        if (model == null) {
            DBMDebug.error(scriptEntry, "Model is not specified.");
            return;
        }
        String modelName = model.asString();
        if (remove) {
            if (!platform.isModeled(entity)) {
                DBMDebug.error(scriptEntry, "Entity does not have any models.");
                return;
            }
            if (platform.removeModel(entity, modelName)) {
                DBMDebug.approval(scriptEntry, "Model '" + modelName + "' removed from entity.");
            } else {
                DBMDebug.error(scriptEntry, "Model '" + modelName + "' not found on entity.");
            }
            return;
        }

        platform.attach(entity, modelName).ifPresentOrElse(
                tracker -> DBMDebug.approval(scriptEntry, "Model '" + modelName + "' added to entity."),
                () -> DBMDebug.error(scriptEntry, "Model renderer '" + modelName + "' not found.")
        );
    }
}
