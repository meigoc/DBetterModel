/*
 * Copyright 2025 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import kr.toxicity.model.api.event.PluginEndReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BMReloadEndEvent extends BukkitScriptEvent implements Listener {

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

    public BMReloadEndEvent() {
        registerCouldMatcher("bm finishes reload");
    }

    private PluginEndReloadEvent event;

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("result")) {
            return new ElementTag(event.getResult().getClass().getSimpleName());
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onBetterModelEndReload(PluginEndReloadEvent e) {
        this.event = e;
        fire(e);
    }
}
