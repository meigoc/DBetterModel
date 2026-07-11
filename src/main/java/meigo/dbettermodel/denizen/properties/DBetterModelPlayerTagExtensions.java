/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.properties;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.compat.api.BmRunningAnimation;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.denizen.objects.BMBoneTag;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DBetterModelPlayerTagExtensions {

    public static void register() {
        // <--[tag]
        // @attribute <PlayerTag.limb[(<model_name>)]>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the name of the player's currently active "limb" animation.
        // "Limb" animations are those controlled by the 'bmlimb' command and defined in the 'player-animations' folder.
        // If no model name is provided, this returns the animation from the first-found active limb animator.
        // This can be ambiguous if multiple limb animators are active (e.g., one for base movement, one for gestures).
        // It is highly recommended to specify the model name to ensure script stability.
        // Returns null if the player has no models, no limb animators, or no animation is playing.
        //
        // @example
        // # Narrate the animation from the first available limb model.
        // - narrate "Your current limb animation is: <player.limb>"
        //
        // @example
        // # Check for a specific animation on a specific limb model.
        // - if <player.limb[steve]> == roll:
        //   - narrate "You are rolling!"
        // -->
        PlayerTag.tagProcessor.registerTag(ObjectTag.class, "limb", (attribute, object) -> {
            Player player = object.getPlayerEntity();
            BmPlatform platform = DBetterModel.platform();
            if (player == null || platform == null) {
                return null;
            }
            Set<String> limbModelNames = new HashSet<>(platform.limbNames());
            if (limbModelNames.isEmpty()) {
                return null;
            }
            List<BmTracker> playerLimbTrackers = platform.trackers(player).stream()
                    .filter(tracker -> limbModelNames.contains(tracker.name()))
                    .toList();
            if (playerLimbTrackers.isEmpty()) {
                return null;
            }
            BmTracker targetTracker;
            if (attribute.hasContext(1)) {
                String modelName = attribute.getContext(1);
                targetTracker = playerLimbTrackers.stream()
                        .filter(tracker -> tracker.name().equalsIgnoreCase(modelName))
                        .findFirst()
                        .orElse(null);
            } else {
                targetTracker = playerLimbTrackers.get(0);
            }
            if (targetTracker == null) {
                return null;
            }
            Optional<BmRunningAnimation> running = targetTracker.runningAnimation();
            if (running.isPresent()) {
                BmRunningAnimation runningAnimation = running.get();
                MapTag map = new MapTag();
                map.putObject("animation_name", new ElementTag(runningAnimation.name()));
                map.putObject("loop_mode", new ElementTag(runningAnimation.rawTypeName().toLowerCase()));

                targetTracker.model().animationLoopMode(runningAnimation.name()).ifPresent(loop ->
                        map.putObject("default_loop_mode", new ElementTag(loop.toLowerCase()))
                );
                return map;
            }
            return null;
        });

        // <--[tag]
        // @attribute <PlayerTag.limb_bones[<model_name>]>
        // @returns MapTag
        // @plugin DBetterModel
        // @description
        // Returns a MapTag of all bones for a specific player limb model.
        // The keys are the bone names, and the values are BMBoneTag objects.
        // This requires the model name to be specified.
        //
        // @example
        // # List all bones of the 'steve' limb model for the current player.
        // - foreach <player.limb_bones[steve]> as:bone:
        //   - narrate "Found bone: <[bone].name>"
        // -->
        PlayerTag.tagProcessor.registerTag(MapTag.class, "limb_bones", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The limb_bones tag must have a model name specified.");
                return null;
            }
            String modelName = attribute.getContext(1);
            Player player = object.getPlayerEntity();
            BmPlatform platform = DBetterModel.platform();
            if (player == null || platform == null) {
                return null;
            }

            if (platform.limbNames().stream().noneMatch(limb -> limb.equalsIgnoreCase(modelName))) {
                attribute.echoError("Model '" + modelName + "' is not a valid player limb model.");
                return null;
            }

            return platform.tracker(player, modelName)
                    .map(tracker -> {
                        MapTag map = new MapTag();
                        UUID uuid = tracker.entityUuid();
                        for (BmBone bone : tracker.bones()) {
                            map.putObject(bone.name(), new BMBoneTag(uuid, modelName, bone.name()));
                        }
                        return map;
                    }).orElse(null);
        });
    }


}
