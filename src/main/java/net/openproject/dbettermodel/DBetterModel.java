package net.openproject.dbettermodel;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.openproject.dbettermodel.commands.BMBillboardCommand;
import net.openproject.dbettermodel.commands.BMLimbCommand;
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

    public static final String BETTERMODEL_VERSION = "1.10.1";
    public static final String DBETTERMODEL_VERSION = "3.0.0";

    @Override
    public void onEnable() {
        Debug.log("DBetterModel " + DBETTERMODEL_VERSION + " loading...");
        saveDefaultConfig();
        instance = this;

        Debug.log("Targeting BetterModel API version " + BETTERMODEL_VERSION);

        registerCommands();
        registerObjects();
        registerEvents();
        registerExtensions();

        Debug.log("DBetterModel loaded successfully!");
    }

    private void registerCommands() {
        tryRegister("BMModelCommand", () -> DenizenCore.commandRegistry.registerCommand(BMModelCommand.class));
        tryRegister("BMStateCommand", () -> DenizenCore.commandRegistry.registerCommand(BMStateCommand.class));
        tryRegister("BMBillboardCommand", () -> DenizenCore.commandRegistry.registerCommand(BMBillboardCommand.class));
        tryRegister("BMLimbCommand", () -> DenizenCore.commandRegistry.registerCommand(BMLimbCommand.class));
    }

    private void registerObjects() {
        tryRegister("BMEntityTag", () -> ObjectFetcher.registerWithObjectFetcher(BMEntityTag.class, BMEntityTag.tagProcessor));
        tryRegister("BMModelTag", () -> ObjectFetcher.registerWithObjectFetcher(BMModelTag.class, BMModelTag.tagProcessor));
        tryRegister("BMBoneTag", () -> ObjectFetcher.registerWithObjectFetcher(BMBoneTag.class, BMBoneTag.tagProcessor));
    }

    private void registerEvents() {
        tryRegister("BMReloadStartEvent", () -> ScriptEvent.registerScriptEvent(BMReloadStartEvent.class));
        tryRegister("BMReloadEndEvent", () -> ScriptEvent.registerScriptEvent(BMReloadEndEvent.class));
    }



    private void registerExtensions() {
        tryRegister("DBetterModelEntityTagExtensions", DBetterModelEntityTagExtensions::register);
    }

    private void tryRegister(String featureName, Runnable registrationLogic) {
        try {
            registrationLogic.run();
        } catch (Throwable e) {
            Debug.echoError("Error registering feature '" + featureName + "'.");
            Debug.echoError(e);
        }
    }

    @Override
    public void onDisable() {
        Debug.log("DBetterModel disabled.");
    }
}
