/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.entity.Player;

public class BMHitboxInteractedEvent extends BMTrackerScriptEvent {

    // <--[event]
    // @Events
    // bm hitbox interacted
    //
    // @Group DBetterModel
    //
    // @Switch model:<name> to only process the event if the model name matches.
    //
    // @Cancellable false
    //
    // @Triggers when a player interacts with a model hitbox. Requires the HITBOX_INTERACT_EVENT or HITBOX_INTERACT_AT_EVENT capability of the active compat layer.
    // The compat api callback does not carry the bone, so there is no bone context.
    //
    // @Context
    // <context.model> returns the interacted BMModelTag (may be missing when the layer cannot resolve the tracker).
    // <context.model_name> returns the name of the model, when resolved.
    // <context.entity> returns the source EntityTag, if the tracker is entity-bound.
    // <context.hand> returns the hand used to interact (HAND/OFF_HAND).
    //
    // @Player Always.
    //
    // -->

    public static BMHitboxInteractedEvent instance;

    public Player player;
    public String hand;

    public BMHitboxInteractedEvent() {
        instance = this;
        registerCouldMatcher("bm hitbox interacted");
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(new PlayerTag(player), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("hand")) {
            return new ElementTag(hand);
        }
        return super.getContext(name);
    }

    /** Fired by the compat event sink; tracker may be null. */
    public static void handle(BmTracker tracker, Player player, String hand) {
        if (instance != null) {
            instance.tracker = tracker;
            instance.player = player;
            instance.hand = hand;
            instance.fire();
        }
    }
}
