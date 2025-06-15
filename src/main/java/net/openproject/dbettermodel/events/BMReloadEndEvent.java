package net.openproject.dbettermodel.events;

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
    // @Triggers when a BetterModel finishes reloading
    //
    // @Context
    // <context.result> returns the result
    //
    // -->

    public BMReloadEndEvent() {
        registerCouldMatcher("bm finishes reload");
    }

    private PluginEndReloadEvent event;

    @Override
    public boolean matches(ScriptPath path) {
        return true;
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "result" -> new ElementTag(event.getResult().toString());
            default -> super.getContext(name);
        };
    }

    @EventHandler
    public void onBetterModelEndReload(PluginEndReloadEvent e) {
        this.event = e;
        fire(e);
    }
}
