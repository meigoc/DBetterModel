/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.events.BukkitScriptEvent;

public class BMReloadStartEvent extends BukkitScriptEvent {

    // <--[event]
    // @Events
    // bm starts reload
    //
    // @Group DBetterModel
    //
    // @Cancellable false
    //
    // @Triggers when BetterModel starts reloading its configuration and models.
    //
    // -->

    public static BMReloadStartEvent instance;

    public BMReloadStartEvent() {
        instance = this;
        registerCouldMatcher("bm starts reload");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    /** Fired by the compat event sink. */
    public static void handle() {
        if (instance != null) {
            instance.fire();
        }
    }
}
