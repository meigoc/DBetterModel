/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.commands;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmAnimationOptions;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.util.DBMDebug;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BMLimbCommand extends AbstractCommand {

    public BMLimbCommand() {
        setName("bmlimb");
        setSyntax("bmlimb target:<player> model:<model_animator> animation:<animation_name> (loop:<once|loop|hold>) (hide:<player>)");
        autoCompile();
    }

    // <--[command]
    // @Name BMLimb
    // @Syntax bmlimb target:<player> model:<model_animator> animation:<animation_name> (loop:<once|loop|hold>) (hide:<player>)
    // @Required 3
    // @Short Plays a player-specific animation.
    // @Group DBetterModel
    //
    // @Description
    // Plays a player animation from a model in the 'player-animations' folder.
    // The 'loop' argument controls the playback mode:
    // - once: Plays the animation a single time (default).
    // - loop: Repeats the animation indefinitely.
    // - hold: Plays the animation once and freezes on the final frame.
    //
    // The optional 'hide' argument allows you to make all the target player's models
    // invisible to a specific observer player. The animation will still play for everyone else.
    //
    // To stop a looping or held animation, play another animation over it.
    //
    // @Usage
    // To make a player perform a 'roll' animation once.
    // - bmlimb target:<player> model:steve animation:roll
    //
    // @Usage
    // To make a player perform a repeating 'roll' animation.
    // - bmlimb target:<player> model:steve animation:roll loop:loop
    //
    // @Usage
    // To animate player_1, but hide their model from player_2.
    // - bmlimb target:<[player_1]> model:steve animation:roll hide:<[player_2]>
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        BmPlatform platform = DBetterModel.platform();
        if (platform == null) {
            return;
        }
        tab.addWithPrefix("model:", platform.limbNames());
        tab.addWithPrefix("loop:", List.of("once", "loop", "hold"));

        if (tab.arg.toLowerCase().startsWith("animation:")) {
            Set<String> allAnimations = new HashSet<>();
            platform.limbNames().forEach(name ->
                    platform.limb(name).ifPresent(model -> allAnimations.addAll(model.animations())));
            tab.add(allAnimations);
        }
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("target") @ArgPrefixed PlayerTag playerTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("animation") @ArgPrefixed ElementTag animationName,
                                   @ArgName("loop") @ArgDefaultText("once") @ArgPrefixed ElementTag loopMode,
                                   @ArgName("hide") @ArgPrefixed @ArgDefaultNull PlayerTag hideForPlayer) {

        BmPlatform platform = DBetterModel.platform();
        if (platform == null) {
            DBMDebug.error(scriptEntry, "DBetterModel compat layer is not available.");
            return;
        }
        Player player = playerTag.getPlayerEntity();
        if (player == null) {
            DBMDebug.error(scriptEntry, "Player not found.");
            return;
        }

        String model = modelName.asString();
        String animation = animationName.asString();
        if (platform.limb(model).isEmpty()) {
            DBMDebug.error(scriptEntry, "Limb animator model '" + model + "' not found. Make sure it is configured under 'player-animations'.");
            return;
        }

        BmAnimationOptions.LoopMode type = switch (loopMode.asString().toLowerCase().trim()) {
            case "loop" -> BmAnimationOptions.LoopMode.LOOP;
            case "hold" -> BmAnimationOptions.LoopMode.HOLD;
            default -> BmAnimationOptions.LoopMode.ONCE;
        };

        BmAnimationOptions options = BmAnimationOptions.builder()
                .lerpTicks(0)
                .loopMode(type)
                .speed(1.0f)
                .build();

        boolean success = platform.playerLimbs().playLimbAnimation(player, model, animation, options);

        // 5.x printed the BetterModel iterator type name here (play_once/loop/hold_on_last).
        String modeName = switch (type) {
            case LOOP -> "loop";
            case HOLD -> "hold_on_last";
            default -> "play_once";
        };
        if (success) {
            DBMDebug.approval(scriptEntry, "Started player animation '" + animation + "' from model '" + model + "' on " + player.getName() + " with mode '" + modeName + "'.");
        } else {
            DBMDebug.error(scriptEntry, "Failed to start animation '" + animation + "'. It might not exist in the model '" + model + "'.");
        }

        if (hideForPlayer!= null) {
            Player observer = hideForPlayer.getPlayerEntity();
            if (observer == null) {
                DBMDebug.error(scriptEntry, "Observer player for 'hide' argument not found.");
            } else {
                List<BmTracker> trackers = platform.trackers(player);
                if (trackers.isEmpty()) {
                    DBMDebug.error(scriptEntry, "Target player " + player.getName() + " has no models to hide.");
                } else {
                    trackers.forEach(tracker -> tracker.hide(observer));
                    DBMDebug.approval(scriptEntry, "Hid " + player.getName() + "'s models from " + observer.getName() + ".");
                }
            }
        }
    }
}
