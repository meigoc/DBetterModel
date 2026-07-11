/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.compat.api.BmPlayerLimbs;
import meigo.dbettermodel.util.DBMDebug;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;

public class BMPartCommand extends AbstractCommand {

    public BMPartCommand() {
        setName("bmpart");
        setSyntax("bmpart entity:<entity> model:<model> bone:<bone> part:<part_name> from:<player>");
        setRequiredArguments(5, 5);
        autoCompile();
    }

    // <--[command]
    // @Name BMPart
    // @Syntax bmpart entity:<entity> model:<model> bone:<bone> part:<part_name> from:<player>
    // @Required 5
    // @Short Applies a player's skin part to a specific model bone.
    // @Group DBetterModel
    //
    // @Description
    // This command dynamically maps a bone of a model to a part of a player's skin.
    // The model's bone will then render using the texture and shape of the specified player's skin part.
    // This is the correct way to apply player skins to models, as it allows BetterModel's engine to handle skin fetching and caching.
    // See also the BMBoneTag 'skin' mechanism — the same operation as a bone adjust.
    //
    // @Usage
    // Use to make the 'head' bone of the demon_knight model display the head of the player 'Notch'.
    // - bmpart entity:<[knight_entity]> model:demon_knight bone:head part:head from:Notch
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        BmPlatform platform = DBetterModel.platform();
        if (platform != null && tab.arg.startsWith("part:")) {
            tab.add(platform.playerLimbs().skinPartNames());
        }
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("bone") @ArgPrefixed ElementTag boneName,
                                   @ArgName("part") @ArgPrefixed ElementTag partName,
                                   @ArgName("from") @ArgPrefixed PlayerTag fromPlayer) {

        BmPlatform platform = DBetterModel.platform();
        if (platform == null) {
            DBMDebug.error(scriptEntry, "DBetterModel compat layer is not available.");
            return;
        }
        Entity entity = entityTag.getBukkitEntity();
        Player sourcePlayer = fromPlayer.getPlayerEntity();
        if (entity == null || sourcePlayer == null) {
            DBMDebug.error(scriptEntry, "Target entity or source player not found.");
            return;
        }

        platform.playerLimbs().applySkinPart(
                () -> Optional.ofNullable(entityTag.getBukkitEntity()),
                modelName.asString(), boneName.asString(), partName.asString(), sourcePlayer
        ).thenAccept(result -> {
            switch (result) {
                case SUCCESS -> DBMDebug.approval(scriptEntry, "Successfully applied skin part '" + partName.asString()
                        + "' from " + fromPlayer.getName() + " to bone '" + boneName.asString() + "'.");
                case MODEL_NOT_FOUND -> DBMDebug.error(scriptEntry, "Model '" + modelName.asString() + "' not found on the entity.");
                case BONE_NOT_FOUND -> DBMDebug.error(scriptEntry, "Bone '" + boneName.asString() + "' not found or is a dummy bone.");
                case INVALID_PART -> DBMDebug.error(scriptEntry, "Invalid part name: '" + partName.asString() + "'.");
                case TARGET_GONE -> { /* target vanished mid-flight — 5.x was silent here */ }
            }
        }).exceptionally(e -> {
            DBMDebug.error(scriptEntry, "Failed to load skin for " + fromPlayer.getName() + ": " + e.getMessage());
            return null;
        });
    }
}
