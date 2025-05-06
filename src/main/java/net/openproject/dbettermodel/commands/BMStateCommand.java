package net.openproject.dbettermodel.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class BMStateCommand extends AbstractCommand {

    public BMStateCommand() {
        setName("bmstate");
        setSyntax("bmstate entity:<entity> state:<animation> loop:<once|loop> (speed:<#.#>) (remove)");
        autoCompile();
    }

    // <--[command]
    // @Name BMState
    // @Syntax bmstate [entity:<entity>] [state:<animation>] [loop:<once|loop>] (speed:(#.#) (remove)
    // @Required 3
    // @Short Plays a state on a bmentity
    // @Group DBetterModel
    //
    // @Description
    // Plays a state on a bmentity.
    //
    //
    // @Usage
    // Use to add a model to an entity.
    // - bmstate entity:<context.entity> state:hi loop:loop
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("state")  @ArgPrefixed ElementTag animName,
                                   @ArgName("loop")   @ArgPrefixed ElementTag loopMode,
                                   @ArgName("speed")  @ArgDefaultText("1.0") @ArgPrefixed ElementTag speedTag,
                                   @ArgName("remove") boolean remove
    ) {
        Entity e = entityTag.getBukkitEntity();
        if (!(e instanceof LivingEntity)) {
            Debug.echoError("The entity must be a living entity");
            return;
        }
        EntityTracker tracker = EntityTracker.tracker(e);
        if (tracker == null) {
            Debug.echoError("The entity does not have a BetterModel attached");
            return;
        }

        String animation = animName.asString();
        String loop = loopMode.asString().toLowerCase().trim();
        float  speed = (float) speedTag.asDouble();

        if (remove) {
            tracker.stopAnimation(animation);
            return;
        }

        tracker.stopAnimation(animation);


        switch (loop) {
            case "once":
                tracker.animate(animation, AnimationModifier.DEFAULT);
                break;
            case "loop":
                AnimationModifier loopType =
                        new AnimationModifier(
                                AnimationModifier.DEFAULT.predicate(),
                                AnimationModifier.DEFAULT.start(),
                                AnimationModifier.DEFAULT.end(),
                                AnimationIterator.Type.LOOP,
                                AnimationModifier.DEFAULT.speed()
                        );

                tracker.animate(b -> true, animation, loopType, () -> {});
                break;
            default:
                Debug.echoError("The 'loop' parameter must be either 'once' or 'loop'");
        }
    }
}
