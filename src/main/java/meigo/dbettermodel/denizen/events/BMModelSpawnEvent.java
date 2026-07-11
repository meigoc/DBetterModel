/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.entity.Player;

public class BMModelSpawnEvent extends BMTrackerScriptEvent {

    // <--[event]
    // @Events
    // bm model spawns for player
    //
    // @Group DBetterModel
    //
    // @Switch model:<name> to only process the event if the model name matches.
    //
    // @Cancellable false
    //
    // @Triggers when a model becomes visible to (spawns for) a player. Requires the SPAWN_EVENTS capability of the active compat layer.
    //
    // @Context
    // <context.model> returns the BMModelTag that spawned.
    // <context.model_name> returns the name of the model.
    // <context.entity> returns the source EntityTag, if the tracker is entity-bound.
    // <context.player> returns the PlayerTag the model spawned for.
    //
    // @Player Always.
    //
    // -->

    public static BMModelSpawnEvent instance;

    public Player player;

    public BMModelSpawnEvent() {
        instance = this;
        registerCouldMatcher("bm model spawns for player");
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(new PlayerTag(player), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("player")) {
            return new PlayerTag(player);
        }
        return super.getContext(name);
    }

    /** Fired by the compat event sink (SPAWN_EVENTS). */
    public static void handle(BmTracker tracker, Player player) {
        if (instance != null) {
            instance.tracker = tracker;
            instance.player = player;
            instance.fire();
        }
    }
}
