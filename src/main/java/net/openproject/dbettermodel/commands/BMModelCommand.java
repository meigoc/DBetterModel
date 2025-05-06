package net.openproject.dbettermodel.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

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
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addWithPrefix("model:", BetterModel.inst().modelManager().keys());
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag model,
                                   @ArgName("remove") boolean remove) throws Exception {

        Entity entity = entityTag.entity;
        if (!(entity instanceof LivingEntity)) {
            Debug.echoError("Entity must be a living entity");
            return;
        }
        if (model == null) {
            Debug.echoError("Model is not specified");
            return;
        }
        if (remove) {
            EntityTracker tr = EntityTracker.tracker(entity);
            if (tr != null) tr.close();
            return;
        }

        BetterModel.inst()
                .modelManager()
                .renderer(model.toString())
                .create(entity);

        EntityTracker tracker = EntityTracker.tracker(entity);
        if (tracker == null) {
            Debug.echoError("Failed to create model tracker");
            return;
        }
        RenderInstance inst = tracker.getInstance();
        if (inst == null) {
            Debug.echoError("Failed to get model instance");
            return;
        }

        tracker.spawnNearby(entity.getLocation());
    }
}
