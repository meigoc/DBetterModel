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
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.RenderedBone;
import meigo.dbettermodel.services.ModelService;
import meigo.dbettermodel.util.DBMDebug;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class BMStateCommand extends AbstractCommand {

    public BMStateCommand() {
        setName("bmstate");
        setSyntax("bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_duration:<duration>) (for_players:<list_of_players>) (remove)");
        autoCompile();
    }

    // <--[command]
    // @Name BMState
    // @Syntax bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_duration:<duration>) (for_players:<list_of_players>) (remove)
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
    // The 'remove' argument stops the specified animation on the specified bones/players.
    //
    // @Usage
    // # Play a looping 'walk' animation only on the leg bones for everyone.
    // - bmstate entity:<context.entity> model:robot state:walk bones:left_leg|right_leg loop:loop
    //
    // @Usage
    // # Make the robot wave, but only player_1 and player_2 can see it.
    // - bmstate entity:<context.entity> model:robot state:wave for_players:<[player_1]>|<[player_2]>
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("state") @ArgPrefixed ElementTag animName,
                                   @ArgName("bones") @ArgPrefixed @ArgDefaultNull ListTag bones,
                                   @ArgName("loop") @ArgDefaultText("once") @ArgPrefixed ElementTag loopMode,
                                   @ArgName("speed") @ArgDefaultText("1.0") @ArgPrefixed ElementTag speedTag,
                                   @ArgName("lerp_duration") @ArgDefaultText("1t") @ArgPrefixed DurationTag lerpDuration,
                                   @ArgName("for_players") @ArgPrefixed @ArgDefaultNull ListTag forPlayers,
                                   @ArgName("remove") boolean remove) {
        Entity entity = entityTag.getBukkitEntity();
        BetterModel.registry(entity).ifPresentOrElse(registry -> {
            var tracker = registry.tracker(modelName.asString());
            if (tracker == null) {
                DBMDebug.error(scriptEntry, "Model '" + modelName.asString() + "' not found on entity " + entity.getUniqueId() + ".");
                return;
            }
            String animation = animName.asString();
            Predicate<RenderedBone> boneFilter = (bone) -> true;
            if (bones!= null) {
                final Set<String> boneNames = new HashSet<>(bones);
                boneFilter = (bone) -> boneNames.contains(bone.name().name());
            }

            if (remove) {
                if (tracker.stopAnimation(boneFilter, animation)) {
                    DBMDebug.approval(scriptEntry, "Stopped animation '" + animation + "' on model '" + modelName.asString() + "'.");
                } else {
                    DBMDebug.error(scriptEntry, "Animation '" + animation + "' was not running on the specified parts of model '" + modelName.asString() + "'.");
                }
                return;
            }

            AnimationIterator.Type type = switch (loopMode.asString().toLowerCase().trim()) {
                case "loop" -> AnimationIterator.Type.LOOP;
                case "hold" -> AnimationIterator.Type.HOLD_ON_LAST;
                default -> AnimationIterator.Type.PLAY_ONCE;
            };

            AnimationModifier.Builder builder = AnimationModifier.builder()
                    .start(lerpDuration.getTicksAsInt())
                    .type(type)
                    .speed(speedTag::asFloat)
                    .override(false);

            ModelService.getInstance().playAnimationForPlayers(tracker, animation, builder.build(), forPlayers);

            DBMDebug.approval(scriptEntry, "Started animation '" + animation + "' on model '" + modelName.asString() + "'.");

        }, () -> DBMDebug.error(scriptEntry, "The entity does not have any BetterModel models attached."));
    }
}