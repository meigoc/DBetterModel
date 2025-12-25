/*
 * Copyright 2025 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import kr.toxicity.model.api.event.PluginStartReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BMReloadStartEvent extends BukkitScriptEvent implements Listener {

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

    public BMReloadStartEvent() {
        registerCouldMatcher("bm starts reload");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    @EventHandler
    public void onBetterModelStartReload(PluginStartReloadEvent e) {
        fire(e);
    }
}
