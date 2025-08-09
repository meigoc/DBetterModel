package meigo.dbettermodel;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import meigo.dbettermodel.denizen.commands.*;
import meigo.dbettermodel.denizen.events.BMReloadEndEvent;
import meigo.dbettermodel.denizen.events.BMReloadStartEvent;
import meigo.dbettermodel.denizen.objects.BMBoneTag;
import meigo.dbettermodel.denizen.objects.BMEntityTag;
import meigo.dbettermodel.denizen.objects.BMModelTag;
import meigo.dbettermodel.denizen.properties.DBetterModelEntityTagExtensions;
import meigo.dbettermodel.services.ModelService;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.event.SkinApplyEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for DBetterModel.
 * Initializes all components, including services, Denizen integrations, and event listeners.
 */
public class DBetterModel extends JavaPlugin {

    public static DBetterModel instance;
    public static final String BETTERMODEL_VERSION = "1.11.0";
    public static final String DBETTERMODEL_VERSION = "4.0.0";

    public static long skinApplyDelay;
    public static boolean enablePluginLogging = true;

    @Override
    public void onEnable() {
        Debug.log("DBetterModel " + DBETTERMODEL_VERSION + " loading...");
        saveDefaultConfig();
        instance = this;
        reloadConfig();
        skinApplyDelay = getConfig().getLong("options.skin-apply-delay-ticks", 3L);
        ModelService.getInstance().initialize(this);

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
            SkinsRestorer skinsRestorerApi = SkinsRestorerProvider.get();
            skinsRestorerApi.getEventBus().subscribe(this, SkinApplyEvent.class, event -> {
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
        tryRegister("BMMountCommand", () -> DenizenCore.commandRegistry.registerCommand(BMMountCommand.class));
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
        ModelService.getInstance().shutdown();
        Debug.log("DBetterModel disabled.");
    }
}