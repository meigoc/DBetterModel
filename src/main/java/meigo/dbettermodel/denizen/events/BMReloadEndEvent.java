/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import meigo.dbettermodel.compat.api.BmReloadResult;

public class BMReloadEndEvent extends BukkitScriptEvent {

    // <--[event]
    // @Events
    // bm finishes reload
    //
    // @Group DBetterModel
    //
    // @Cancellable false
    //
    // @Triggers when BetterModel finishes reloading its configuration and models.
    //
    // @Context
    // <context.result> returns whether the reload was a 'Success', 'Failure', or 'OnReload'.
    //
    // -->

    public static BMReloadEndEvent instance;

    public BMReloadEndEvent() {
        instance = this;
        registerCouldMatcher("bm finishes reload");
    }

    private BmReloadResult result = BmReloadResult.UNKNOWN;

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("result")) {
            // legacyName() preserves the exact 5.x strings (Success/Failure/OnReload).
            return new ElementTag(result.legacyName());
        }
        return super.getContext(name);
    }

    /** Fired by the compat event sink. */
    public static void handle(BmReloadResult result) {
        if (instance != null) {
            instance.result = result;
            instance.fire();
        }
    }
}
