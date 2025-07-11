package net.openproject.dbettermodel.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Entity;

public class BMStateCommand extends AbstractCommand {

    public BMStateCommand() {
        setName("bmstate");
        setSyntax("bmstate entity:<entity> state:<animation> (loop:<once|loop|hold>) (speed:<#.#>) (remove)");
        autoCompile();
    }

    // <--[command]
    // @Name BMState
    // @Syntax bmstate [entity:<entity>] [state:<animation>] (loop:<once|loop|hold>) (speed:<#.#>) (remove)
    // @Required 2
    // @Short Plays a state on a bmentity.
    // @Group DBetterModel
    //
    // @Description
    // Plays a state on a bmentity. If multiple models are on the entity, it will affect the first one loaded.
    //
    // @Usage
    // Use to play an animation on an entity's model.
    // - bmstate entity:<context.entity> state:walk loop:loop speed:1.5
    //
    // Use to stop an animation.
    // - bmstate entity:<context.entity> state:walk remove
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("state") @ArgPrefixed ElementTag animName,
                                   @ArgName("loop") @ArgDefaultText("once") @ArgPrefixed ElementTag loopMode,
                                   @ArgName("speed") @ArgDefaultText("1.0") @ArgPrefixed ElementTag speedTag,
                                   @ArgName("remove") boolean remove
    ) {
        Entity entity = entityTag.getBukkitEntity();

        if (!EntityTrackerRegistry.hasModelData(entity)) {
            Debug.echoError("The entity does not have any BetterModel models attached.");
            return;
        }

        EntityTrackerRegistry registry = EntityTrackerRegistry.registry(entity.getUniqueId());

        EntityTracker tracker = registry.first();
        if (tracker == null) {
            Debug.echoError("The entity does not have a loaded model tracker.");
            return;
        }

        String animation = animName.asString();

        if (remove) {
            tracker.stopAnimation(animation);
            Debug.echoApproval("Stopped animation '" + animation + "' on entity.");
            return;
        }

        float speed = speedTag.asFloat();

        AnimationIterator.Type type = switch (loopMode.asString().toLowerCase().trim()) {
            case "loop" -> AnimationIterator.Type.LOOP;
            case "hold" -> AnimationIterator.Type.HOLD_ON_LAST;
            default -> AnimationIterator.Type.PLAY_ONCE;
        };

        // for devs: в будущей версии 1.8.2 заменить первый аргумент на BooleanConstantSupplier.TRUE
        AnimationModifier modifier = new AnimationModifier(
                () -> true,
                1, // start tick
                0, // end tick
                type,
                speed
        );

        if (tracker.animate(animation, modifier)) {
            Debug.echoApproval("Started animation '" + animation + "' on entity.");
        }
        else {
            Debug.echoError("Failed to start animation '" + animation + "'. It might not exist on model '" + tracker.name() + "'.");
        }
    }
}
