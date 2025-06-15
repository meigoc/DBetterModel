package net.openproject.dbettermodel;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.openproject.dbettermodel.commands.BMModelCommand;
import net.openproject.dbettermodel.commands.BMStateCommand;
import net.openproject.dbettermodel.events.BMReloadEndEvent;
import net.openproject.dbettermodel.events.BMReloadStartEvent;
import net.openproject.dbettermodel.objects.BMBoneTag;
import net.openproject.dbettermodel.objects.BMEntityTag;
import net.openproject.dbettermodel.objects.BMModelTag;
import net.openproject.dbettermodel.properties.DBetterModelEntityTagExtensions;
import org.bukkit.plugin.java.JavaPlugin;

public class DBetterModel extends JavaPlugin {

    public static DBetterModel instance;

    @Override
    public void onEnable() {
        Debug.log("DBetterModel loading...");
        saveDefaultConfig();
        instance = this;

        DenizenCore.commandRegistry.registerCommand(BMModelCommand.class);
        DenizenCore.commandRegistry.registerCommand(BMStateCommand.class);

        ObjectFetcher.registerWithObjectFetcher(BMEntityTag.class, BMEntityTag.tagProcessor);
        ObjectFetcher.registerWithObjectFetcher(BMModelTag.class, BMModelTag.tagProcessor);
        ObjectFetcher.registerWithObjectFetcher(BMBoneTag.class, BMBoneTag.tagProcessor);

        ScriptEvent.registerScriptEvent(BMReloadStartEvent.class);
        ScriptEvent.registerScriptEvent(BMReloadEndEvent.class);


        DBetterModelEntityTagExtensions.register();

        Debug.log("DBetterModel loaded!");
    }

    @Override
    public void onDisable() {
        Denizen.getInstance().onDisable();
    }
}
