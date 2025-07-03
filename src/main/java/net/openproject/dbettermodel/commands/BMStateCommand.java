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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class BMStateCommand extends AbstractCommand {

    public BMStateCommand() {
        setName("bmstate");
        setSyntax("bmstate entity:<entity> state:<animation> (loop:<once|loop|hold>) (speed:<#.#>) (remove) (override)");
        autoCompile();
    }

    // <--[command]
    // @Name BMState
    // @Syntax bmstate [entity:<entity>] [state:<animation>] [loop:<once|loop|hold>] (speed:<#.#>) (remove) (override)
    // @Required 3
    // @Short Plays a state on a bmentity with optional override and smooth interpolation
    // @Group DBetterModel
    //
    // @Description
    // Plays a state on a bmentity with optional override and smooth interpolation.
    //
    // @Usage
    // Use to add a model to an entity with custom animation settings.
    // - bmstate entity:<context.entity> state:hi loop:loop speed:1.5 override
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgPrefixed EntityTag entityTag,
                                   @ArgName("state") @ArgPrefixed ElementTag animName,
                                   @ArgName("loop") @ArgDefaultText("once") @ArgPrefixed ElementTag loopMode,
                                   @ArgName("speed") @ArgDefaultText("1.0") @ArgPrefixed ElementTag speedTag,
                                   @ArgName("remove") boolean remove
    ) {
        Entity entity = entityTag.getBukkitEntity();
        if (!(entity instanceof LivingEntity)) {
            Debug.echoError("The entity must be a living entity");
            return;
        }
        // fixed: 'tracker(org.bukkit.entity.@org.jetbrains.annotations.NotNull Entity)' is deprecated and marked for removal
        EntityTrackerRegistry registry = EntityTrackerRegistry.registry(entity);
        EntityTracker tracker = registry.first()
        if (tracker == null) {
            Debug.echoError("The entity does not have a BetterModel attached");
            return;
        }

        String animation = animName.asString();
        String loop = loopMode.asString().toLowerCase().trim();
        float speed = (float) speedTag.asDouble();

        if (remove) {
            tracker.stopAnimation(animation);
            return;
        }

//        Supplier<Float> dynamicSpeed = () -> {
//            return 1.0f + (System.currentTimeMillis() % 10000) / 5000f;
//        };
//        AnimationModifier.SpeedModifier sm = new AnimationModifier.SpeedModifier(dynamicSpeed);

        AnimationIterator.Type type = switch (loop) {
            case "loop" -> AnimationIterator.Type.LOOP;
            case "hold" -> AnimationIterator.Type.HOLD_ON_LAST;
            default -> AnimationIterator.Type.PLAY_ONCE;
        };

        AnimationModifier modifier = new AnimationModifier(
                AnimationModifier.DEFAULT.predicate(),
                AnimationModifier.DEFAULT.start(),
                AnimationModifier.DEFAULT.end(),
                type,
                speed
        );

        tracker.animate(animation, modifier);
    }
}
