/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.denizen.KeyframeScriptBridge;
import meigo.dbettermodel.denizen.objects.BMModelTag;
import org.bukkit.entity.Player;

import java.util.Map;

public class BMAnimationSignalEvent extends BukkitScriptEvent {

    // <--[event]
    // @Events
    // bm animation signal
    //
    // @Group DBetterModel
    //
    // @Switch name:<name> to only process the event if the signal name matches.
    //
    // @Cancellable false
    //
    // @Triggers when a Blockbench 'denizen:' instruction keyframe or a per-player animation signal fires.
    // Requires the ANIMATION_SIGNALS capability of the active compat layer.
    //
    // @Context
    // <context.name> returns the name of the signal.
    // <context.metadata> returns a MapTag of the 'key=value' metadata of the instruction keyframe (empty for per-player signals).
    // <context.player> returns the PlayerTag the signal targets, if it is a per-player signal.
    // <context.model> returns the BMModelTag whose animation emitted the signal. Available for 'denizen:' keyframe signals
    // on every BetterModel line; always null for per-player signals (no BetterModel line attaches the emitting model to those).
    // <context.model_name> returns the name of the emitting model, under the same availability rules as <context.model>.
    //
    // @Player When the signal is per-player.
    //
    // -->

    public static BMAnimationSignalEvent instance;

    public BmTracker tracker;
    public String signal;
    public Map<String, String> metadata;
    public Player player;

    public BMAnimationSignalEvent() {
        instance = this;
        registerCouldMatcher("bm animation signal");
        registerSwitches("name");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "name", signal)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(player == null ? null : new PlayerTag(player), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "name" -> {
                return new ElementTag(signal);
            }
            case "metadata" -> {
                MapTag map = new MapTag();
                if (metadata != null) {
                    metadata.forEach((key, value) -> map.putObject(key, new ElementTag(value)));
                }
                return map;
            }
            case "player" -> {
                return player == null ? null : new PlayerTag(player);
            }
            case "model" -> {
                return tracker == null ? null : new BMModelTag(tracker);
            }
            case "model_name" -> {
                return tracker == null ? null : new ElementTag(tracker.name());
            }
        }
        return super.getContext(name);
    }

    /** Fired by the compat event sink (ANIMATION_SIGNALS / PLAYER_ANIMATION_EVENTS). Tracker is null for per-player signals. */
    public static void handle(BmTracker tracker, String signal, Map<String, String> metadata, Player player) {
        if (KeyframeScriptBridge.SIGNAL.equals(signal) && metadata != null && metadata.containsKey("script")) {
            KeyframeScriptBridge.handle(tracker, metadata, player);
        }
        if (instance != null) {
            instance.tracker = tracker;
            instance.signal = signal;
            instance.metadata = metadata;
            instance.player = player;
            instance.fire();
        }
    }
}
