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
import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.util.DBMDebug;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;

import java.util.List;

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
    // - bmboard entity:<context.entity> model:demon_knight bone:head type:center
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        BmPlatform platform = DBetterModel.platform();
        if (platform != null) {
            tab.addWithPrefix("model:", platform.modelNames());
        }
        tab.addWithPrefix("type:", List.of("fixed", "vertical", "horizontal", "center"));
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("bone") @ArgPrefixed ElementTag boneName,
                                   @ArgName("type") @ArgPrefixed ElementTag type) {

        BmPlatform platform = DBetterModel.platform();
        if (platform == null) {
            DBMDebug.error(scriptEntry, "DBetterModel compat layer is not available.");
            return;
        }
        Entity entity = entityTag.getBukkitEntity();
        Display.Billboard billboardType;
        try {
            billboardType = Display.Billboard.valueOf(type.asString().toUpperCase());
        } catch (IllegalArgumentException e) {
            DBMDebug.error(scriptEntry, "Invalid billboard type specified: " + type.asString());
            return;
        }

        if (!platform.isModeled(entity)) {
            DBMDebug.error(scriptEntry, "Entity does not have any models.");
            return;
        }
        BmTracker tracker = platform.tracker(entity, modelName.asString()).orElse(null);
        if (tracker == null) {
            DBMDebug.error(scriptEntry, "Model '" + modelName.asString() + "' not found on entity.");
            return;
        }

        BmBone bone = tracker.bone(boneName.asString()).orElse(null);
        if (bone == null) {
            DBMDebug.error(scriptEntry, "Bone '" + boneName.asString() + "' not found on model '" + modelName.asString() + "'.");
            return;
        }

        if (bone.setBillboard(billboardType)) {
            DBMDebug.approval(scriptEntry, "Set billboard type of bone '" + boneName.asString() + "' to '" + type.asString() + "'.");
        } else {
            DBMDebug.error(scriptEntry, "Failed to set billboard for bone '" + boneName.asString() + "'. It might be a dummy bone without a display.");
        }
    }
}
