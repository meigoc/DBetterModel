package net.openproject.dbettermodel.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import org.bukkit.entity.Entity;

import java.util.Optional;

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
    // - bmmodel entity:<context.entity> model:my_model
    //
    // @Usage
    // Use to remove a model from an entity.
    // - bmmodel entity:<context.entity> model:my_model remove
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addWithPrefix("model:", BetterModel.models().stream().map(m -> m.name()).toList());
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag model,
                                   @ArgName("remove") boolean remove) {

        Entity entity = entityTag.getBukkitEntity();
        if (model == null) {
            Debug.echoError("Model is not specified.");
            return;
        }
        String modelName = model.asString();

        if (remove) {
            BetterModel.registry(entity).ifPresentOrElse(registry -> {
                if (registry.remove(modelName)) {
                    Debug.echoApproval("Model '" + modelName + "' removed from entity.");
                } else {
                    Debug.echoError("Model '" + modelName + "' not found on entity.");
                }
            }, () -> {
                Debug.echoError("Entity does not have any models.");
            });
            return;
        }

        Optional.ofNullable(BetterModel.plugin().modelManager().renderer(modelName)).ifPresentOrElse(
                renderer -> {
                    renderer.create(entity);
                    Debug.echoApproval("Model '" + modelName + "' added to entity.");
                },
                () -> Debug.echoError("Model renderer '" + modelName + "' not found.")
        );
    }
}
