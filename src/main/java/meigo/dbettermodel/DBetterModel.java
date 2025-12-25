/*
 * Copyright 2025 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import meigo.dbettermodel.denizen.commands.*;
import meigo.dbettermodel.denizen.events.BMReloadEndEvent;
import meigo.dbettermodel.denizen.events.BMReloadStartEvent;
import meigo.dbettermodel.denizen.objects.BMBoneTag;
import meigo.dbettermodel.denizen.objects.BMEntityTag;
import meigo.dbettermodel.denizen.objects.BMModelTag;
import meigo.dbettermodel.denizen.properties.DBetterModelEntityTagExtensions;
import meigo.dbettermodel.denizen.properties.DBetterModelPlayerTagExtensions;
import meigo.dbettermodel.services.ModelService;
import meigo.dbettermodel.util.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main plugin class for DBetterModel.
 * Initializes all components, including services and Denizen integration.
 */
public class DBetterModel extends JavaPlugin {

    public static DBetterModel instance;
    public static final String DBETTERMODEL_VERSION = "5.0.0";
    private static final int BSTATS_ID = 28477;

    public static boolean checkForUpdates;
    public static boolean enablePluginLogging = true;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";

    @Override
    public void onEnable() {
        if (!isBetterModelCompatible()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Debug.log("DBetterModel " + DBETTERMODEL_VERSION + " loading...");
        saveDefaultConfig();
        instance = this;
        reloadConfig();
        checkForUpdates = getConfig().getBoolean("options.check-for-updates", true);
        enablePluginLogging = getConfig().getBoolean("options.enable-plugin-logging", true);
        ModelService.getInstance().initialize(this);

        registerCommands();
        registerObjects();
        registerEvents();
        registerExtensions();

        initMetrics();
        if (checkForUpdates) {
            runUpdateChecker();
        }

        Debug.log("DBetterModel loaded successfully!");
    }

    @Override
    public void onDisable() {
        ModelService.getInstance().shutdown();
        Debug.log("DBetterModel disabled.");
    }

    public static DBetterModel getInstance() {
        return instance;
    }

    private boolean isBetterModelCompatible() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BetterModel");
        if (plugin == null) {
            getLogger().severe("BetterModel not found!");
            return false;
        }

        String version = plugin.getDescription().getVersion();
        int major = 0;
        int minor = 0;

        try {
            String cleanVersion = version.split("-")[0];
            String[] parts = cleanVersion.split("\\.");
            if (parts.length >= 2) {
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "✖ Could not parse BetterModel version: " + version + ANSI_RESET);
            return false;
        }

        if (major == 1 && minor == 15) {
            System.out.println(ANSI_GREEN + "✔ Detected BetterModel version is fully supported." + ANSI_RESET);
            return true;
        } else if (major >= 2 || (major == 1 && minor >= 16)) {
            System.out.println(ANSI_RED + "✖ This version of BetterModel is not supported due to API changes." + ANSI_RESET);
            System.out.println(ANSI_RED + "For more info, contact the author or visit https://github.com/meigoc/DBetterModel" + ANSI_RESET);
            return false;
        } else {
            System.out.println(ANSI_RED + "✖ This version is not supported by the current addon version." + ANSI_RESET);
            System.out.println(ANSI_RED + "Please check our GitHub Readme for a suitable version. GitHub: https://github.com/meigoc/DBetterModel" + ANSI_RESET);
            return false;
        }
    }

    private void initMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_ID);
        metrics.addCustomChart(new Metrics.SimplePie("Denizen", () ->
                Bukkit.getPluginManager().getPlugin("Denizen").getDescription().getVersion()));
        metrics.addCustomChart(new Metrics.SimplePie("BetterModel", () ->
                Bukkit.getPluginManager().getPlugin("BetterModel").getDescription().getVersion()));
    }

    private void runUpdateChecker() {
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            try {
                URL url = new URL("https://api.github.com/repos/meigoc/DBetterModel/releases");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "DBetterModel-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher matcher = pattern.matcher(response.toString());

                    if (matcher.find()) {
                        String latestTag = matcher.group(1);
                        String cleanLatest = latestTag.toLowerCase().startsWith("v") ? latestTag.substring(1) : latestTag;

                        if (!cleanLatest.equalsIgnoreCase(DBETTERMODEL_VERSION)) {
                            Debug.log("Found a new version: " + latestTag);
                            Debug.log("Download it on Modrinth: https://modrinth.com/plugin/dbettermodel");
                        }
                    }
                }
            } catch (Exception ignored) {}
        }, 1200L);
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
        tryRegister("DBetterModelPlayerTagExtensions", DBetterModelPlayerTagExtensions::register);
    }

    private void tryRegister(String featureName, Runnable registrationLogic) {
        try {
            registrationLogic.run();
        } catch (Throwable e) {
            Debug.echoError("Error registering feature '" + featureName + "'.");
            Debug.echoError(e);
        }
    }
}