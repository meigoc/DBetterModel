/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import meigo.dbettermodel.compat.api.BmPlatform;
import meigo.dbettermodel.denizen.BMServerTags;
import meigo.dbettermodel.denizen.commands.*;
import meigo.dbettermodel.denizen.events.*;
import meigo.dbettermodel.denizen.objects.BMBoneTag;
import meigo.dbettermodel.denizen.objects.BMEntityTag;
import meigo.dbettermodel.denizen.objects.BMModelTag;
import meigo.dbettermodel.denizen.properties.DBetterModelEntityTagExtensions;
import meigo.dbettermodel.denizen.properties.DBetterModelPlayerTagExtensions;
import meigo.dbettermodel.services.ModelService;
import meigo.dbettermodel.util.Metrics;
import meigo.dbettermodel.util.Versions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main plugin class for DBetterModel.
 * Selects the BetterModel compat layer, then wires services and the Denizen surface.
 */
public class DBetterModel extends JavaPlugin {

    public static DBetterModel instance;
    private static final int BSTATS_ID = 28477;

    public static boolean checkForUpdates;
    public static boolean enablePluginLogging = true;

    private BmPlatform platform;

    @Override
    public void onEnable() {
        instance = this;
        platform = selectPlatform();
        if (platform == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Debug.log("DBetterModel " + version() + " loading...");
        saveDefaultConfig();
        reloadConfig();
        checkForUpdates = getConfig().getBoolean("options.check-for-updates", true);
        enablePluginLogging = getConfig().getBoolean("options.enable-plugin-logging", true);
        ModelService.getInstance().initialize(this, platform);

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
        if (platform != null) {
            try {
                platform.shutdown();
            } catch (Throwable t) {
                getLogger().warning("Compat layer shutdown failed: " + t.getMessage());
            }
            platform = null;
        }
        Debug.log("DBetterModel disabled.");
    }

    public static DBetterModel getInstance() {
        return instance;
    }

    /** Plugin version, single-sourced from plugin.yml (expanded from gradle.properties). */
    public static String version() {
        return instance.getDescription().getVersion();
    }

    /** The active compat layer, or null when the plugin failed to bootstrap. */
    public static BmPlatform platform() {
        return instance == null ? null : instance.platform;
    }

    /**
     * Reads the installed BetterModel version, delegates the layer decision to
     * {@link LayerSelector}, then reflectively loads the selected layer
     * ({@code meigo.dbettermodel.compat.vN.VNPlatform}, constructor {@code (Plugin ownPlugin)}).
     * Never throws — returns null (and logs why) on any unsupported/failed case.
     */
    private BmPlatform selectPlatform() {
        Plugin bm = Bukkit.getPluginManager().getPlugin("BetterModel");
        if (bm == null) {
            getLogger().severe("BetterModel not found!");
            return null;
        }
        String version = bm.getDescription().getVersion();
        LayerSelector.Selection selection = LayerSelector.select(version);
        if (selection.layerClass() == null) {
            getLogger().severe("BetterModel " + version + ": " + selection.reason() + " — disabling.");
            return null;
        }
        if (selection.bestEffort()) {
            // The v3 layer only uses public API, so try it and let its constructor
            // probe the signatures it needs before committing.
            getLogger().warning("BetterModel " + version + " is newer than this build knows. "
                    + "Trying the v3 compat layer in best-effort mode — if anything is off, "
                    + "the plugin will disable itself. See https://github.com/meigoc/DBetterModel");
        }

        String layerClass = selection.layerClass();
        try {
            Class<?> clazz = Class.forName(layerClass);
            BmPlatform loaded = (BmPlatform) clazz.getConstructor(Plugin.class).newInstance(this);
            getLogger().info("Detected BetterModel " + version + " — using compat layer " + layerClass);
            return loaded;
        } catch (Throwable t) {
            getLogger().severe("Failed to load compat layer " + layerClass + " for BetterModel "
                    + version + ": " + t + " — disabling.");
            return null;
        }
    }

    private void initMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_ID);
        metrics.addCustomChart(new Metrics.SimplePie("Denizen", () -> pluginVersion("Denizen")));
        metrics.addCustomChart(new Metrics.SimplePie("BetterModel", () -> pluginVersion("BetterModel")));
    }

    private static String pluginVersion(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin == null ? "unknown" : plugin.getDescription().getVersion();
    }

    private void runUpdateChecker() {
        Bukkit.getAsyncScheduler().runDelayed(this, task -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest request = HttpRequest.newBuilder(
                                URI.create("https://api.github.com/repos/meigoc/DBetterModel/releases/latest"))
                        .header("User-Agent", "DBetterModel-UpdateChecker")
                        .header("Accept", "application/vnd.github+json")
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    getLogger().info("Update check failed: GitHub responded with HTTP " + response.statusCode());
                    return;
                }
                Matcher matcher = TAG_NAME.matcher(response.body());
                if (!matcher.find()) {
                    getLogger().info("Update check failed: no tag_name in the GitHub response.");
                    return;
                }
                String latestTag = matcher.group(1);
                if (Versions.isNewer(latestTag, version())) {
                    Debug.log("Found a new version: " + latestTag);
                    Debug.log("Download it on Modrinth: https://modrinth.com/plugin/dbettermodel");
                }
            } catch (Exception e) {
                getLogger().info("Update check failed: " + e);
            }
        }, 60L, java.util.concurrent.TimeUnit.SECONDS);
    }

    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private void registerCommands() {
        tryRegister("BMModelCommand", () -> DenizenCore.commandRegistry.registerCommand(BMModelCommand.class));
        tryRegister("BMStateCommand", () -> DenizenCore.commandRegistry.registerCommand(BMStateCommand.class));
        tryRegister("BMBillboardCommand", () -> DenizenCore.commandRegistry.registerCommand(BMBillboardCommand.class));
        tryRegister("BMLimbCommand", () -> DenizenCore.commandRegistry.registerCommand(BMLimbCommand.class));
        tryRegister("BMPartCommand", () -> DenizenCore.commandRegistry.registerCommand(BMPartCommand.class));
        tryRegister("BMMountCommand", () -> DenizenCore.commandRegistry.registerCommand(BMMountCommand.class));
        tryRegister("BMSummonCommand", () -> DenizenCore.commandRegistry.registerCommand(BMSummonCommand.class));
    }

    private void registerObjects() {
        tryRegister("BMEntityTag", () -> ObjectFetcher.registerWithObjectFetcher(BMEntityTag.class, BMEntityTag.tagProcessor));
        tryRegister("BMModelTag", () -> ObjectFetcher.registerWithObjectFetcher(BMModelTag.class, BMModelTag.tagProcessor));
        tryRegister("BMBoneTag", () -> ObjectFetcher.registerWithObjectFetcher(BMBoneTag.class, BMBoneTag.tagProcessor));
    }

    private void registerEvents() {
        tryRegister("BMReloadStartEvent", () -> ScriptEvent.registerScriptEvent(BMReloadStartEvent.class));
        tryRegister("BMReloadEndEvent", () -> ScriptEvent.registerScriptEvent(BMReloadEndEvent.class));
        tryRegister("BMTrackerCreatedEvent", () -> ScriptEvent.registerScriptEvent(BMTrackerCreatedEvent.class));
        tryRegister("BMTrackerClosedEvent", () -> ScriptEvent.registerScriptEvent(BMTrackerClosedEvent.class));
        tryRegister("BMModelSpawnEvent", () -> ScriptEvent.registerScriptEvent(BMModelSpawnEvent.class));
        tryRegister("BMModelDespawnEvent", () -> ScriptEvent.registerScriptEvent(BMModelDespawnEvent.class));
        tryRegister("BMAnimationSignalEvent", () -> ScriptEvent.registerScriptEvent(BMAnimationSignalEvent.class));
        tryRegister("BMPlayerAnimationSignalEvent", () -> ScriptEvent.registerScriptEvent(BMPlayerAnimationSignalEvent.class));
        tryRegister("BMAnimationStartEvent", () -> ScriptEvent.registerScriptEvent(BMAnimationStartEvent.class));
        tryRegister("BMAnimationEndEvent", () -> ScriptEvent.registerScriptEvent(BMAnimationEndEvent.class));
        tryRegister("BMHitboxDamagedEvent", () -> ScriptEvent.registerScriptEvent(BMHitboxDamagedEvent.class));
        tryRegister("BMHitboxInteractedEvent", () -> ScriptEvent.registerScriptEvent(BMHitboxInteractedEvent.class));
        tryRegister("BMModelMountedEvent", () -> ScriptEvent.registerScriptEvent(BMModelMountedEvent.class));
        tryRegister("BMModelDismountedEvent", () -> ScriptEvent.registerScriptEvent(BMModelDismountedEvent.class));
    }

    private void registerExtensions() {
        tryRegister("DBetterModelEntityTagExtensions", DBetterModelEntityTagExtensions::register);
        tryRegister("DBetterModelPlayerTagExtensions", DBetterModelPlayerTagExtensions::register);
        tryRegister("BMServerTags", BMServerTags::init);
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
