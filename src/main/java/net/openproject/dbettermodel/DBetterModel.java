package net.openproject.dbettermodel;

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
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DBetterModel extends JavaPlugin {

    public static DBetterModel instance;

    public static final String CRAFTBUKKIT_VERSION = "1.20.4-R0.1-SNAPSHOT";
    public static final String DENIZEN_VERSION = "1.3.0-SNAPSHOT";
    public static final String BETTERMODEL_VERSION = "1.9.0";
    public static final String DBETTERMODEL_VERSION = "2.1.0";

    @Override
    public void onEnable() {
        Debug.log("DBetterModel loading...");
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        instance = this;

        // Display build information
        Debug.log("DBetterModel Build Information:");
        Debug.log("  - craftbukkit.version: " + CRAFTBUKKIT_VERSION);
        Debug.log("  - denizen.version: " + DENIZEN_VERSION);
        Debug.log("  - bettermodel.version: " + BETTERMODEL_VERSION);
        Debug.log("Plugin created by gleb_petrovich (Meigo™ Corporation) for Better Models API version " + BETTERMODEL_VERSION + " and Denizen " + DENIZEN_VERSION);

        // Safely register all components
        registerCommands();
        registerObjects();
        registerEvents();
        registerExtensions();

        Debug.log("DBetterModel loaded!");

        if (getConfig().getBoolean("checkUpdates", true)) {
            checkPluginVersions();
        }
    }

    private void registerCommands() {
        tryRegister("BMModelCommand",
                () -> DenizenCore.commandRegistry.registerCommand(BMModelCommand.class),
                "com.denizenscript.denizen.objects.EntityTag", "kr.toxicity.model.api.BetterModel", "kr.toxicity.model.api.tracker.EntityTrackerRegistry");
        tryRegister("BMStateCommand",
                () -> DenizenCore.commandRegistry.registerCommand(BMStateCommand.class),
                "com.denizenscript.denizen.objects.EntityTag", "kr.toxicity.model.api.animation.AnimationIterator", "kr.toxicity.model.api.tracker.EntityTrackerRegistry", "kr.toxicity.model.api.util.function.BooleanConstantSupplier");
    }

    private void registerObjects() {
        tryRegister("BMEntityTag",
                () -> ObjectFetcher.registerWithObjectFetcher(BMEntityTag.class, BMEntityTag.tagProcessor),
                "com.denizenscript.denizen.objects.EntityTag", "kr.toxicity.model.api.tracker.EntityTracker", "kr.toxicity.model.api.tracker.EntityTrackerRegistry");
        tryRegister("BMModelTag",
                () -> ObjectFetcher.registerWithObjectFetcher(BMModelTag.class, BMModelTag.tagProcessor),
                "com.denizenscript.denizen.objects.EntityTag", "kr.toxicity.model.api.bone.RenderedBone", "kr.toxicity.model.api.tracker.EntityTrackerRegistry");
        tryRegister("BMBoneTag",
                () -> ObjectFetcher.registerWithObjectFetcher(BMBoneTag.class, BMBoneTag.tagProcessor),
                "com.denizenscript.denizen.objects.EntityTag", "kr.toxicity.model.api.bone.RenderedBone", "kr.toxicity.model.api.util.TransformedItemStack", "kr.toxicity.model.api.util.function.BonePredicate");
    }

    private void registerEvents() {
        tryRegister("BMReloadStartEvent",
                () -> ScriptEvent.registerScriptEvent(BMReloadStartEvent.class),
                "com.denizenscript.denizen.events.BukkitScriptEvent", "kr.toxicity.model.api.event.PluginStartReloadEvent");
        tryRegister("BMReloadEndEvent",
                () -> ScriptEvent.registerScriptEvent(BMReloadEndEvent.class),
                "com.denizenscript.denizen.events.BukkitScriptEvent", "kr.toxicity.model.api.event.PluginEndReloadEvent");
    }

    private void registerExtensions() {
        tryRegister("DBetterModelEntityTagExtensions",
                DBetterModelEntityTagExtensions::register,
                "com.denizenscript.denizen.objects.EntityTag", "net.openproject.dbettermodel.objects.BMEntityTag");
    }

    private void tryRegister(String featureName, Runnable registrationLogic, String... requiredClasses) {
        for (String className : requiredClasses) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                Debug.log("Cannot register feature '" + featureName + "'. Required class not found: " + className);
                return;
            }
        }
        try {
            registrationLogic.run();
        } catch (Throwable e) {
            Debug.echoError("Error registering feature '" + featureName + "'.");
            Debug.echoError(e);
        }
    }

    private void checkPluginVersions() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // Check BetterModel version
            checkVersion(
                    "BetterModel",
                    "https://api.github.com/repos/toxicity188/BetterModel/releases/latest",
                    BETTERMODEL_VERSION,
                    "A new version of BetterModel is available: %s. Please update."
            );

            // Check DBetterModel version
            checkVersion(
                    "DBetterModel",
                    "https://api.github.com/repos/meigoc/DBetterModel/releases/latest",
                    DBETTERMODEL_VERSION,
                    "A new version of DBetterModel is available: %s. An update is recommended."
            );
        });
    }

    private void checkVersion(String pluginName, String apiUrl, String currentVersion, String updateMessage) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                connection.disconnect();

                String response = content.toString();
                String latestVersion = parseTag(response);

                if (latestVersion != null && !latestVersion.equals(currentVersion)) {
                    getLogger().warning(String.format(updateMessage, latestVersion));
                }
            } else {
                getLogger().warning("Could not check version for " + pluginName + ". Response code: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            getLogger().warning("Error while checking version for " + pluginName + ": " + e.getMessage());
        }
    }

    private String parseTag(String jsonResponse) {
        String key = "\"tag_name\":\"";
        int keyIndex = jsonResponse.indexOf(key);
        if (keyIndex == -1) {
            return null;
        }
        int startIndex = keyIndex + key.length();
        int endIndex = jsonResponse.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return null;
        }
        // Remove 'v' if it's at the beginning of the tag
        String tag = jsonResponse.substring(startIndex, endIndex);
        return tag.startsWith("v") ? tag.substring(1) : tag;
    }

    @Override
    public void onDisable() {
        Debug.log("DBetterModel disabled.");
    }
}
