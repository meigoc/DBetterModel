/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import meigo.dbettermodel.compat.api.BmTracker;

public class BMHitboxDamagedEvent extends BMTrackerScriptEvent {

    // <--[event]
    // @Events
    // bm hitbox damaged
    //
    // @Group DBetterModel
    //
    // @Switch model:<name> to only process the event if the model name matches.
    //
    // @Cancellable false
    //
    // @Triggers when a model hitbox takes damage. Requires the HITBOX_DAMAGE_EVENTS capability of the active compat layer.
    // Informational only: the compat api callback is not cancellable and does not carry the bone or the damager.
    //
    // @Context
    // <context.model> returns the damaged BMModelTag.
    // <context.model_name> returns the name of the model.
    // <context.entity> returns the source EntityTag, if the tracker is entity-bound.
    // <context.damage> returns the damage amount.
    //
    // -->

    public static BMHitboxDamagedEvent instance;

    public double damage;

    public BMHitboxDamagedEvent() {
        instance = this;
        registerCouldMatcher("bm hitbox damaged");
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("damage")) {
            return new ElementTag(damage);
        }
        return super.getContext(name);
    }

    /** Fired by the compat event sink (HITBOX_DAMAGE_EVENTS). */
    public static void handle(BmTracker tracker, double damage) {
        if (instance != null) {
            instance.tracker = tracker;
            instance.damage = damage;
            instance.fire();
        }
    }
}
