package meigo.dbettermodel.denizen.commands;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.HitBox;
import meigo.dbettermodel.denizen.objects.BMBoneTag;
import meigo.dbettermodel.util.DBMDebug;
import org.bukkit.entity.Entity;

import java.util.Optional;

public class BMMountCommand extends AbstractCommand {

    public BMMountCommand() {
        setName("bmmount");
        setSyntax("bmmount [<entity_to_mount>] on:<bmbone> (dismount) (dismount_all)");
        autoCompile();
    }

    // <--[command]
    // @Name BMMount
    // @Syntax bmmount [<entity_to_mount>] on:<bmbone> (dismount) (dismount_all)
    // @Required 2
    // @Short Mounts or dismounts an entity from a model's bone.
    // @Group DBetterModel
    //
    // @Description
    // Allows entities to ride on specific bones of a BetterModel model.
    // The bone must be configured as a seat in the model file (e.g., with the 'p' tag).
    //
    // To mount an entity, provide the entity and the 'on' argument with a BMBoneTag.
    //
    // To dismount a specific entity, provide the entity, the 'on' argument, and the 'dismount' flag.
    //
    // To dismount all entities from a bone, provide the 'on' argument and the 'dismount_all' flag.
    //
    // @Tags
    // <BMEntityTag.model[<name>].bone[<name>]>
    //
    // @Usage
    // Use to make a player ride on the 'seat' bone of a model on an armor stand.
    // - bmmount <player> on:<[my_armorstand].bm_entity.model[car].bone[seat]>
    //
    // @Usage
    // Use to dismount a specific player.
    // - bmmount <player> on:<[my_armorstand].bm_entity.model[car].bone[seat]> dismount
    //
    // @Usage
    // Use to dismount all entities from the seat.
    // - bmmount on:<[my_armorstand].bm_entity.model[car].bone[seat]> dismount_all
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgDefaultNull @ArgLinear EntityTag entityToMount,
                                   @ArgName("on") @ArgPrefixed BMBoneTag onBone,
                                   @ArgName("dismount") boolean dismount,
                                   @ArgName("dismount_all") boolean dismountAll) {

        if (onBone == null) {
            DBMDebug.error(scriptEntry, "You must specify a bone to mount on or dismount from.");
            return;
        }
        RenderedBone bone = Optional.ofNullable(BetterModel.registryOrNull(onBone.getEntityUUID()))
                .flatMap(registry -> Optional.ofNullable(registry.tracker(onBone.getModelName())))
                .flatMap(tracker -> Optional.ofNullable(tracker.bone(onBone.getBoneName())))
                .orElse(null);
        if (bone == null) {
            DBMDebug.error(scriptEntry, "The specified bone tag is invalid or the model is not loaded.");
            return;
        }
        HitBox hitBox = bone.getHitBox();

        if (hitBox == null) {
            DBMDebug.error(scriptEntry, "The bone '" + onBone.getBoneName() + "' is not a seat or does not have a hitbox. Make sure the bone is tagged with 'p' (e.g., 'p_seat').");
            return;
        }

        if (dismountAll) {
            hitBox.dismountAll();
            DBMDebug.approval(scriptEntry, "Dismounted all entities from bone '" + onBone.getBoneName() + "'.");
            return;
        }

        if (entityToMount == null) {
            DBMDebug.error(scriptEntry, "You must specify an entity to mount or dismount.");
            return;
        }
        Entity entity = entityToMount.getBukkitEntity();

        if (dismount) {
            hitBox.dismount(entity);
            DBMDebug.approval(scriptEntry, "Dismounted " + entity.getName() + " from bone '" + onBone.getBoneName() + "'.");
        } else {
            hitBox.mount(entity);
            DBMDebug.approval(scriptEntry, "Mounted " + entity.getName() + " on bone '" + onBone.getBoneName() + "'.");
        }
    }
}

