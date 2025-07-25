package net.openproject.dbettermodel.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.function.BonePredicate;
import net.openproject.dbettermodel.util.DBMDebug;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;

public class BMBillboardCommand extends AbstractCommand {

    public BMBillboardCommand() {
        setName("bmboard");
        setSyntax("bmboard entity:<entity> model:<model> bone:<bone> type:<fixed|vertical|horizontal|center>");
        autoCompile();
    }

    // <--[command]
    // @Name BMBillboard
    // @Syntax bmboard entity:<entity> model:<model> bone:<bone> type:<fixed|vertical|horizontal|center>
    // @Required 4
    // @Short Applies a billboard effect to a specific bone of a model.
    // @Group DBetterModel
    //
    // @Description
    // Makes a specific bone on a model always face the player.
    // 'type' can be:
    // - fixed: Disables billboard effect.
    // - vertical: The bone rotates on the Y-axis only.
    // - horizontal: The bone rotates on the X and Z axes.
    // - center: The bone rotates on all axes to face the player.
    // This command now automatically sends an update packet, so the change is visible instantly.
    //
    // @Usage
    // Use to make a 'head' bone always face the player.
    // - bmboard entity:<context.entity> model:my_mob bone:head type:center
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("bone") @ArgPrefixed ElementTag boneName,
                                   @ArgName("type") @ArgPrefixed ElementTag type) {

        Entity entity = entityTag.getBukkitEntity();
        Display.Billboard billboardType;
        try {
            billboardType = Display.Billboard.valueOf(type.asString().toUpperCase());
        } catch (IllegalArgumentException e) {
            DBMDebug.error(scriptEntry, "Invalid billboard type specified: " + type.asString());
            return;
        }

        BetterModel.registry(entity).ifPresentOrElse(registry -> {
            var tracker = registry.tracker(modelName.asString());
            if (tracker == null) {
                DBMDebug.error(scriptEntry, "Model '" + modelName.asString() + "' not found on entity.");
                return;
            }

            var bone = tracker.bone(boneName.asString());
            if (bone == null) {
                DBMDebug.error(scriptEntry, "Bone '" + boneName.asString() + "' not found on model '" + modelName.asString() + "'.");
                return;
            }

            if (bone.billboard(BonePredicate.TRUE, billboardType)) {
                tracker.forceUpdate(true);
                DBMDebug.approval(scriptEntry, "Set billboard type of bone '" + boneName.asString() + "' to '" + type.asString() + "'.");
            } else {
                DBMDebug.error(scriptEntry, "Failed to set billboard for bone '" + boneName.asString() + "'. It might be a dummy bone without a display.");
            }
        }, () -> DBMDebug.error(scriptEntry, "Entity does not have any models."));
    }
}