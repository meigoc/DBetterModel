/*
 * Copyright 2025 Meigo™ Corporation
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
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.BoneRenderContext;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.manager.SkinManager;
import kr.toxicity.model.api.player.PlayerLimb;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.EntityTracker;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.util.DBMDebug;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
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
    //
    // @Usage
    // Use to make the 'head' bone of a statue model display the head of the player 'Notch'.
    // - bmpart entity:<[statue_entity]> model:statue_model bone:head part:head from:Notch
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        if (tab.arg.startsWith("part:")) {
            tab.add(Arrays.stream(PlayerLimb.values())
                    .map(limb -> limb.name().toLowerCase())
                    .toList());
        }
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("bone") @ArgPrefixed ElementTag boneName,
                                   @ArgName("part") @ArgPrefixed ElementTag partName,
                                   @ArgName("from") @ArgPrefixed PlayerTag fromPlayer) {

        Entity entity = entityTag.getBukkitEntity();
        Player sourcePlayer = fromPlayer.getPlayerEntity();
        if (entity == null || sourcePlayer == null) {
            DBMDebug.error(scriptEntry, "Target entity or source player not found.");
            return;
        }
        ModelProfile sourceProfile = BetterModel.plugin().nms().profile(sourcePlayer);
        SkinManager skinManager = BetterModel.plugin().skinManager();
        skinManager.complete(sourceProfile.asUncompleted()).thenAccept(skinData -> {

            Bukkit.getScheduler().runTask(DBetterModel.getInstance(), () -> {

                Entity currentEntity = entityTag.getBukkitEntity();
                Player currentPlayer = fromPlayer.getPlayerEntity();
                if (currentEntity == null || currentPlayer == null) return;

                Optional<EntityTracker> trackerOpt = BetterModel.registry(currentEntity)
                        .flatMap(registry -> Optional.ofNullable(registry.tracker(modelName.asString())));

                if (trackerOpt.isEmpty()) {
                    DBMDebug.error(scriptEntry, "Model '" + modelName.asString() + "' not found on the entity.");
                    return;
                }
                EntityTracker tracker = trackerOpt.get();

                RenderedBone bone = tracker.bone(boneName.asString());
                if (bone == null || bone.getDisplay() == null) {
                    DBMDebug.error(scriptEntry, "Bone '" + boneName.asString() + "' not found or is a dummy bone.");
                    return;
                }

                PlayerLimb targetLimb;
                try {
                    targetLimb = PlayerLimb.valueOf(partName.asString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    DBMDebug.error(scriptEntry, "Invalid part name: '" + partName.asString() + "'.");
                    return;
                }

                var adaptedSource = BetterModel.plugin().nms().adapt(currentPlayer);
                BoneRenderContext playerContext = new BoneRenderContext(RenderSource.of(adaptedSource), skinData);

                bone.setItemMapper(targetLimb.getItemMapper());
                bone.updateItem(playerContext);

                tracker.forceUpdate(true);

                DBMDebug.approval(scriptEntry, "Successfully applied skin part '" + partName.asString() + "' from " + fromPlayer.getName() + " to bone '" + boneName.asString() + "'.");
            });

        }).exceptionally(e -> {
            DBMDebug.error(scriptEntry, "Failed to load skin for " + fromPlayer.getName() + ": " + e.getMessage());
            return null;
        });
    }
}