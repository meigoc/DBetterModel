package net.openproject.dbettermodel;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import net.openproject.dbettermodel.commands.*;
import net.openproject.dbettermodel.events.BMReloadEndEvent;
import net.openproject.dbettermodel.events.BMReloadStartEvent;
import net.openproject.dbettermodel.objects.BMBoneTag;
import net.openproject.dbettermodel.objects.BMEntityTag;
import net.openproject.dbettermodel.objects.BMModelTag;
import net.openproject.dbettermodel.properties.DBetterModelEntityTagExtensions;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.event.SkinApplyEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DBetterModel extends JavaPlugin {

    public static DBetterModel instance;
    private SkinsRestorer skinsRestorerApi;

    public static final String BETTERMODEL_VERSION = "1.10.1";
    public static final String DBETTERMODEL_VERSION = "3.3.0";

    public static long skinApplyDelay;
    public static boolean enablePluginLogging;

    @Override
    public void onEnable() {
        Debug.log("DBetterModel " + DBETTERMODEL_VERSION + " loading...");
        saveDefaultConfig();
        instance = this;
        reloadConfig();
        enablePluginLogging = getConfig().getBoolean("options.enable-plugin-logging", true);
        skinApplyDelay = getConfig().getLong("options.skin-apply-delay-ticks", 3L);


        Debug.log("Targeting BetterModel API version " + BETTERMODEL_VERSION);

        registerCommands();
        registerObjects();
        registerEvents();
        registerExtensions();
        registerSkinsRestorerListener();

        Debug.log("DBetterModel loaded successfully!");
    }

    public static DBetterModel getInstance() {
        return instance;
    }

    private void registerSkinsRestorerListener() {
        if (getServer().getPluginManager().getPlugin("SkinsRestorer") == null) {
            Debug.log("SkinsRestorer not found, listener will not be registered.");
            return;
        }

        try {
            this.skinsRestorerApi = SkinsRestorerProvider.get();
            this.skinsRestorerApi.getEventBus().subscribe(this, SkinApplyEvent.class, event -> {
                Player player = event.getPlayer(Player.class);
                if (player == null) {
                    return;
                }

                GameProfile profile = BetterModel.plugin().nms().profile(player);
                BetterModel.plugin().skinManager().removeCache(profile);
            });

            Debug.log("Successfully hooked into SkinsRestorer event bus for SkinApplyEvent.");
        } catch (Throwable e) {
            Debug.echoError("Error registering SkinsRestorer listener.");
            Debug.echoError(e);
        }
    }


    private void registerCommands() {
        tryRegister("BMModelCommand", () -> DenizenCore.commandRegistry.registerCommand(BMModelCommand.class));
        tryRegister("BMStateCommand", () -> DenizenCore.commandRegistry.registerCommand(BMStateCommand.class));
        tryRegister("BMBillboardCommand", () -> DenizenCore.commandRegistry.registerCommand(BMBillboardCommand.class));
        tryRegister("BMLimbCommand", () -> DenizenCore.commandRegistry.registerCommand(BMLimbCommand.class));
        tryRegister("BMPartCommand", () -> DenizenCore.commandRegistry.registerCommand(BMPartCommand.class));
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
