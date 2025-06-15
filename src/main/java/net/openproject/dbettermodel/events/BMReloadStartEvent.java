package net.openproject.dbettermodel.events;

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
    // @Triggers when a BetterModel starts reloading
    //
    // -->

    public BMReloadStartEvent() {
        registerCouldMatcher("bm starts reload");
    }

    private PluginStartReloadEvent event;

    @Override
    public boolean matches(ScriptPath path) {
        return true;
    }

    @EventHandler
    public void onBetterModelStartReload(PluginStartReloadEvent e) {
        this.event = e;
        fire(e);
    }
}