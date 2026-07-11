/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.commands;

import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmCapability;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.denizen.objects.BMModelTag;
import meigo.dbettermodel.services.ModelService;
import meigo.dbettermodel.util.Caps;
import meigo.dbettermodel.util.DBMDebug;

public class BMSummonCommand extends AbstractCommand {

    public BMSummonCommand() {
        setName("bmsummon");
        setSyntax("bmsummon [model:<model> location:<location>] / [handle:<bmmodel> remove]");
        autoCompile();
    }

    // <--[command]
    // @Name BMSummon
    // @Syntax bmsummon [model:<model> location:<location>] / [handle:<bmmodel> remove]
    // @Required 1
    // @Short Summons a location-bound model (dummy tracker), or removes one.
    // @Group DBetterModel
    //
    // @Description
    // Summons a model bound to a location instead of an entity (a BetterModel dummy tracker),
    // useful for props and cutscenes. Requires the DUMMY_TRACKERS capability of the active compat layer.
    //
    // Dummy trackers have no source entity, so their BMModelTag identity uses the form
    // 'bmmodel@dummy:<id>,<model>', where <id> is an internal registry id valid until the
    // tracker is removed, BetterModel reloads, or the server restarts. Do not persist it.
    //
    // To remove a summoned model, pass the saved handle with 'remove'.
    //
    // @Tags
    // <entry[saveName].summoned_model> returns the BMModelTag of the summoned dummy tracker.
    //
    // @Usage
    // # Summon a model and save its handle.
    // - bmsummon model:blue_wizard location:<player.location> save:prop
    // - define prop <entry[prop].summoned_model>
    //
    // @Usage
    // # Remove the summoned model later.
    // - bmsummon handle:<[prop]> remove
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        BmPlatform platform = DBetterModel.platform();
        if (platform != null) {
            tab.addWithPrefix("model:", platform.modelNames());
        }
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("model") @ArgPrefixed @ArgDefaultNull ElementTag model,
                                   @ArgName("location") @ArgPrefixed @ArgDefaultNull LocationTag location,
                                   @ArgName("handle") @ArgPrefixed @ArgDefaultNull BMModelTag handle,
                                   @ArgName("remove") boolean remove) {
        if (remove) {
            if (handle == null) {
                DBMDebug.error(scriptEntry, "The 'remove' form requires 'handle:<bmmodel>' (the saved summoned model).");
                return;
            }
            BmTracker tracker = handle.getTracker();
            ModelService.getInstance().unregisterDummy(tracker);
            tracker.close();
            DBMDebug.approval(scriptEntry, "Removed summoned model '" + tracker.name() + "'.");
            return;
        }
        if (!Caps.require(scriptEntry, BmCapability.DUMMY_TRACKERS)) {
            return;
        }
        if (model == null || location == null) {
            DBMDebug.error(scriptEntry, "Summoning requires both 'model:' and 'location:'.");
            return;
        }
        BmPlatform platform = DBetterModel.platform();
        BmTracker tracker = platform.summon(location, model.asString()).orElse(null);
        if (tracker == null) {
            DBMDebug.error(scriptEntry, "Model '" + model.asString() + "' not found.");
            return;
        }
        BMModelTag result = new BMModelTag(tracker);
        // Registers the dummy id eagerly so the saved handle is stable.
        result.identify();
        scriptEntry.saveObject("summoned_model", result);
        DBMDebug.approval(scriptEntry, "Summoned model '" + model.asString() + "' as " + result.identify() + ".");
    }
}
