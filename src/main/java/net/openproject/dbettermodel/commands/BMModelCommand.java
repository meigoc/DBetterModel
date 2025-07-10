package net.openproject.dbettermodel.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Entity;

public class BMModelCommand extends AbstractCommand {

    public BMModelCommand() {
        setName("bmmodel");
        setSyntax("bmmodel [entity:<entity>] [model:<model>] (remove)");
        autoCompile();
    }

    // <--[command]
    // @Name BMModel
    // @Syntax bmmodel [entity:<entity>] [model:<model>] (remove)
    // @Required 2
    // @Short Adds or removes a model from an entity
    // @Group DBetterModel
    //
    // @Description
    // Adds or removes a model from an entity.
    //
    // @Tags
    // <EntityTag.bm_entity>
    //
    // @Usage
    // Use to add a model to an entity.
    // - bmmodel entity:<context.entity> model:my_model
    //
    // Use to remove a model from an entity.
    // - bmmodel entity:<context.entity> model:my_model remove
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addWithPrefix("model:", BetterModel.plugin().modelManager().keys());
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag model,
                                   @ArgName("remove") boolean remove) {

        Entity entity = entityTag.getBukkitEntity();
        if (model == null) {
            Debug.echoError("Model is not specified");
            return;
        }
        String modelName = model.asString();
        
        if (remove) {
            EntityTrackerRegistry registry = EntityTrackerRegistry.registry(entity.getUniqueId());
            if (registry != null) {
                if (registry.remove(modelName)) {
                    Debug.echoApproval("Model '" + modelName + "' removed from entity.");
                } else {
                    Debug.echoError("Model '" + modelName + "' not found on entity.");
                }
            } else {
                Debug.echoError("Entity does not have any models.");
            }
            return;
        }

        BetterModel.plugin().modelManager().renderer(modelName).create(entity);
        Debug.echoApproval("Model '" + modelName + "' added to entity.");
    }
}
