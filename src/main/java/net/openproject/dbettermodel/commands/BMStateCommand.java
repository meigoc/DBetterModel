package net.openproject.dbettermodel.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
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
                                   @ArgName("remove") @ArgPrefixed @ArgDefaultText("false") ElementTag removeTag
    ) {
        Entity e = entityTag.getBukkitEntity();
        if (!(e instanceof LivingEntity)) {
            Debug.echoError("Сущность должна быть живым существом");
            return;
        }
        EntityTracker tracker = EntityTracker.tracker(e);
        if (tracker == null) {
            Debug.echoError("R сущности не прикреплена модель BetterModel");
            return;
        }

        String animation = animName.asString();
        String loop  = loopMode.asString().toLowerCase();
        float  speed = (float) speedTag.asDouble();
        boolean remove = removeTag.asBoolean();

        if (remove) {
            tracker.stopAnimation(animation);
            return;
        }

        tracker.stopAnimation(animation);

        switch (loop) {
            case "once":
                tracker.animateSingle(animation, new AnimationModifier(0, 0, speed));
                break;
            case "loop":
                tracker.animateLoop(animation, new AnimationModifier(6, 0, speed));
                break;
            default:
                Debug.echoError("Параметр loop должен быть 'once' или 'loop'");
        }
    }
}
