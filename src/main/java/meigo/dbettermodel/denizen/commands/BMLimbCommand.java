package meigo.dbettermodel.denizen.commands;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
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
    // - bmlimb target:<player> model:player_base animation:roll
    //
    // @Usage
    // To make a player perform a repeating 'dance' animation.
    // - bmlimb target:<player> model:player_gestures animation:dance loop:loop
    //
    // @Usage
    // To make player_1 wave, but hide their model from player_2.
    // - bmlimb target:<[player_1]> model:player_base animation:wave hide:<[player_2]>
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addWithPrefix("model:", BetterModel.limbs().stream().map(m -> m.name()).toList());
        tab.addWithPrefix("loop:", List.of("once", "loop", "hold"));

        if (tab.arg.toLowerCase().startsWith("animation:")) {
            Set<String> allAnimations = new HashSet<>();
            BetterModel.limbs().forEach(model -> allAnimations.addAll(model.animations().keySet()));
            tab.add(allAnimations);
        }
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("target") @ArgPrefixed PlayerTag playerTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("animation") @ArgPrefixed ElementTag animationName,
                                   @ArgName("loop") @ArgDefaultText("once") @ArgPrefixed ElementTag loopMode,
                                   @ArgName("hide") @ArgPrefixed @ArgDefaultNull PlayerTag hideForPlayer) {

        Player player = playerTag.getPlayerEntity();
        if (player == null) {
            DBMDebug.error(scriptEntry, "Player not found.");
            return;
        }

        String model = modelName.asString();
        String animation = animationName.asString();
        if (BetterModel.limbOrNull(model) == null) {
            DBMDebug.error(scriptEntry, "Limb animator model '" + model + "' not found. Make sure it is configured under 'player-animations'.");
            return;
        }

        AnimationIterator.Type type = switch (loopMode.asString().toLowerCase().trim()) {
            case "loop" -> AnimationIterator.Type.LOOP;
            case "hold" -> AnimationIterator.Type.HOLD_ON_LAST;
            default -> AnimationIterator.Type.PLAY_ONCE;
        };

        AnimationModifier modifier = AnimationModifier.builder()
                .predicate(BooleanConstantSupplier.TRUE)
                .start(0)
                .end(0)
                .type(type)
                .speed(1.0f)
                .build();

        boolean success = BetterModel.plugin().modelManager().animate(player, model, animation, modifier);

        if (success) {
            DBMDebug.approval(scriptEntry, "Started player animation '" + animation + "' from model '" + model + "' on " + player.getName() + " with mode '" + type.name().toLowerCase() + "'.");
        } else {
            DBMDebug.error(scriptEntry, "Failed to start animation '" + animation + "'. It might not exist in the model '" + model + "'.");
        }

        if (hideForPlayer!= null) {
            Player observer = hideForPlayer.getPlayerEntity();
            if (observer == null) {
                DBMDebug.error(scriptEntry, "Observer player for 'hide' argument not found.");
            } else {
                BetterModel.registry(player).ifPresentOrElse(registry -> {
                    registry.trackers().forEach(tracker -> tracker.hide(observer));
                    DBMDebug.approval(scriptEntry, "Hid " + player.getName() + "'s models from " + observer.getName() + ".");
                }, () -> DBMDebug.error(scriptEntry, "Target player " + player.getName() + " has no models to hide."));
            }
        }
    }
}