/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
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
import meigo.dbettermodel.services.ModelService;
import meigo.dbettermodel.util.DBMDebug;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BMStateCommand extends AbstractCommand {

    public BMStateCommand() {
        setName("bmstate");
        setSyntax("bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_duration:<duration>) (lerp_frames:<#>) (for_players:<list_of_players>) (remove)");
        autoCompile();
    }

    // <--[command]
    // @Name BMState
    // @Syntax bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_duration:<duration>) (lerp_frames:<#>) (for_players:<list_of_players>) (remove)
    // @Required 3
    // @Short Plays or stops a layered animation state on a model, with per-player and per-bone control.
    // @Group DBetterModel
    //
    // @Description
    // Plays or stops an animation state on a specific model attached to an entity.
    // This command supports multiple concurrent animations by allowing you to apply animations to specific parts of the model.
    //
    // The 'bones' argument is an optional list of bone names to which this animation should be applied.
    // If not provided, the animation applies to the entire model. This is the key to layering animations.
    //
    // The 'for_players' argument is an optional list of players. If provided, the animation will only be visible to those players. This uses the official BetterModel API for per-player animations.
    //
    // The 'lerp_duration' argument (previously lerp_frames) now accepts a DurationTag for smoother transitions.
    //
    // The 'lerp_frames' argument is a deprecated pre-4.0.0 alias, interpreted as a tick count.
    // When both are given, 'lerp_duration' wins.
    //
    // The 'remove' argument stops the specified animation on the specified bones/players.
    //
    // @Usage
    // # Play a looping 'walk' animation only on the leg bones for everyone.
    // - bmstate entity:<context.entity> model:demon_knight state:walk bones:left_leg|right_leg loop:loop
    //
    // @Usage
    // # Make the knight guard, but only player_1 and player_2 can see it.
    // - bmstate entity:<context.entity> model:demon_knight state:guard for_players:<[player_1]>|<[player_2]>
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        BmPlatform platform = DBetterModel.platform();
        if (platform != null) {
            tab.addWithPrefix("model:", platform.modelNames());
        }
        tab.addWithPrefix("loop:", List.of("once", "loop", "hold"));
        tab.add("remove");
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("state") @ArgPrefixed ElementTag animName,
                                   @ArgName("bones") @ArgPrefixed @ArgDefaultNull ListTag bones,
                                   @ArgName("loop") @ArgDefaultText("once") @ArgPrefixed ElementTag loopMode,
                                   @ArgName("speed") @ArgDefaultText("1.0") @ArgPrefixed ElementTag speedTag,
                                   @ArgName("lerp_duration") @ArgPrefixed @ArgDefaultNull DurationTag lerpDuration,
                                   @ArgName("lerp_frames") @ArgPrefixed @ArgDefaultNull ElementTag lerpFrames,
                                   @ArgName("for_players") @ArgPrefixed @ArgDefaultNull ListTag forPlayers,
                                   @ArgName("remove") boolean remove) {
        BmPlatform platform = DBetterModel.platform();
        if (platform == null) {
            DBMDebug.error(scriptEntry, "DBetterModel compat layer is not available.");
            return;
        }
        Entity entity = entityTag.getBukkitEntity();
        if (!platform.isModeled(entity)) {
            DBMDebug.error(scriptEntry, "The entity does not have any BetterModel models attached.");
            return;
        }
        BmTracker tracker = platform.tracker(entity, modelName.asString()).orElse(null);
        if (tracker == null) {
            DBMDebug.error(scriptEntry, "Model '" + modelName.asString() + "' not found on entity " + entity.getUniqueId() + ".");
            return;
        }
        String animation = animName.asString();
        Set<String> boneNames = bones != null ? new HashSet<>(bones) : null;

        if (remove) {
            if (tracker.stopAnimation(animation, boneNames)) {
                DBMDebug.approval(scriptEntry, "Stopped animation '" + animation + "' on model '" + modelName.asString() + "'.");
            } else {
                DBMDebug.error(scriptEntry, "Animation '" + animation + "' was not running on the specified parts of model '" + modelName.asString() + "'.");
            }
            return;
        }

        BmAnimationOptions.LoopMode type = switch (loopMode.asString().toLowerCase().trim()) {
            case "loop" -> BmAnimationOptions.LoopMode.LOOP;
            case "hold" -> BmAnimationOptions.LoopMode.HOLD;
            default -> BmAnimationOptions.LoopMode.ONCE;
        };

        // Legacy alias: lerp_frames (pre-4.0.0) counts as ticks; explicit lerp_duration wins.
        int lerpTicks = 1;
        if (lerpDuration != null) {
            lerpTicks = lerpDuration.getTicksAsInt();
        }
        else if (lerpFrames != null && lerpFrames.isInt()) {
            lerpTicks = lerpFrames.asInt();
        }

        BmAnimationOptions.Builder options = BmAnimationOptions.builder()
                .lerpTicks(lerpTicks)
                .loopMode(type)
                .speed(speedTag.asFloat())
                .override(false)
                .bones(boneNames);

        ModelService.getInstance().playAnimationForPlayers(tracker, animation, options, forPlayers);

        DBMDebug.approval(scriptEntry, "Started animation '" + animation + "' on model '" + modelName.asString() + "'.");
    }
}
