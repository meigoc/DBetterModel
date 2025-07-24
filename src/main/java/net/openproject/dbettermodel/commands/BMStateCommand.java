package net.openproject.dbettermodel.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.RenderedBone;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class BMStateCommand extends AbstractCommand {

    public BMStateCommand() {
        setName("bmstate");
        setSyntax("bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_frames:<#>) (remove)");
        autoCompile();
    }
    
    // <--[command]
    // @Name BMState
    // @Syntax bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_frames:<#>) (remove)
    // @Required 3
    // @Short Plays or stops a layered animation state on a specific model on an entity, optionally limited to specific bones.
    // @Group DBetterModel
    //
    // @Description
    // Plays or stops an animation state on a specific model attached to an entity.
    // This command supports multiple concurrent animations by allowing you to apply animations to specific parts of the model.
    // The 'bones' argument is an optional list of bone names to which this animation should be applied.
    // If not provided, the animation applies to the entire model. This is the key to layering animations.
    //
    // @Usage
    // Use to play a looping 'walk' animation only on the bones named 'left_leg' and 'right_leg'.
    // - bmstate entity:<context.entity> model:robot state:walk bones:left_leg|right_leg loop:loop
    //
    // @Usage
    // Use to play a one-shot 'shoot' animation on the 'right_arm' bone, without stopping the walk animation.
    // - bmstate entity:<context.entity> model:robot state:shoot bones:right_arm
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("model") @ArgPrefixed ElementTag modelName,
                                   @ArgName("state") @ArgPrefixed ElementTag animName,
                                   @ArgName("bones") @ArgPrefixed @ArgDefaultNull ListTag bones,
                                   @ArgName("loop") @ArgDefaultText("once") @ArgPrefixed ElementTag loopMode,
                                   @ArgName("speed") @ArgDefaultText("1.0") @ArgPrefixed ElementTag speedTag,
                                   @ArgName("lerp_frames") @ArgDefaultText("1") @ArgPrefixed ElementTag lerpFrames,
                                   @ArgName("remove") boolean remove) {
        Entity entity = entityTag.getBukkitEntity();

        BetterModel.registry(entity).ifPresentOrElse(registry -> {
            var tracker = registry.tracker(modelName.asString());
            if (tracker == null) {
                Debug.echoError("Model '" + modelName.asString() + "' not found on entity " + entity.getUniqueId() + ".");
                return;
            }

            String animation = animName.asString();

            Predicate<RenderedBone> boneFilter = (bone) -> true;
            if (bones != null) {
                final Set<String> boneNames = new HashSet<>(bones);
                boneFilter = (bone) -> boneNames.contains(bone.getName().name());
            }

            if (remove) {
                if (tracker.stopAnimation(boneFilter, animation)) {
                    Debug.echoApproval("Stopped animation '" + animation + "' on model '" + modelName.asString() + "'.");
                } else {
                    Debug.echoError("Animation '" + animation + "' was not running on the specified parts of model '" + modelName.asString() + "'.");
                }
                return;
            }

            int lerp = lerpFrames.asInt();

            AnimationIterator.Type type = switch (loopMode.asString().toLowerCase().trim()) {
                case "loop" -> AnimationIterator.Type.LOOP;
                case "hold" -> AnimationIterator.Type.HOLD_ON_LAST;
                default -> AnimationIterator.Type.PLAY_ONCE;
            };

            AnimationModifier modifier = AnimationModifier.builder()
                    .start(lerp)
                    .type(type)
                    .speed(() -> speedTag.asFloat())
                    .override(false)
                    .build();

            if (tracker.animate(boneFilter, animation, modifier, () -> {})) {
                Debug.echoApproval("Started animation '" + animation + "' on model '" + modelName.asString() + "'.");
            } else {
                Debug.echoError("Failed to start animation '" + animation + "'. It might not exist on model '" + tracker.name() + "'.");
            }
        }, () -> {
            Debug.echoError("The entity does not have any BetterModel models attached.");
        });
    }
}
