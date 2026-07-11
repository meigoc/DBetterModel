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
import meigo.dbettermodel.denizen.objects.BMModelTag;
import org.bukkit.entity.Player;

import java.util.Map;

public class BMPlayerAnimationSignalEvent extends BukkitScriptEvent {

    // <--[event]
    // @Events
    // bm player animation signal
    //
    // @Group DBetterModel
    //
    // @Switch name:<name> to only process the event if the signal name matches.
    //
    // @Cancellable false
    //
    // @Triggers when an animation signal targets a specific player. Requires the PLAYER_ANIMATION_EVENTS capability of the active compat layer.
    //
    // @Context
    // <context.name> returns the name of the signal.
    // <context.metadata> returns a MapTag of the signal metadata, if any.
    // <context.model> returns the BMModelTag whose animation emitted the signal. Always null on this event —
    // no BetterModel line (1.x/2.x/3.x) attaches the emitting model to per-player signals. Kept for symmetry
    // with <@link event bm animation signal>, where 'denizen:' keyframe signals do carry it.
    // <context.model_name> returns the name of the emitting model, under the same availability rules as <context.model>.
    //
    // @Player Always.
    //
    // -->

    public static BMPlayerAnimationSignalEvent instance;

    public BmTracker tracker;
    public String signal;
    public Map<String, String> metadata;
    public Player player;

    public BMPlayerAnimationSignalEvent() {
        instance = this;
        registerCouldMatcher("bm player animation signal");
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
        return new BukkitScriptEntryData(new PlayerTag(player), null);
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
            case "model" -> {
                return tracker == null ? null : new BMModelTag(tracker);
            }
            case "model_name" -> {
                return tracker == null ? null : new ElementTag(tracker.name());
            }
        }
        return super.getContext(name);
    }

    /** Fired by the compat event sink when the signal carries a player. Tracker is null on every BM line today. */
    public static void handle(BmTracker tracker, String signal, Map<String, String> metadata, Player player) {
        if (instance != null) {
            instance.tracker = tracker;
            instance.signal = signal;
            instance.metadata = metadata;
            instance.player = player;
            instance.fire();
        }
    }
}
