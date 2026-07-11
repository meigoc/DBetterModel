/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import meigo.dbettermodel.compat.api.BmTracker;

public class BMTrackerClosedEvent extends BMTrackerScriptEvent {

    // <--[event]
    // @Events
    // bm tracker closed
    //
    // @Group DBetterModel
    //
    // @Switch model:<name> to only process the event if the model name matches.
    //
    // @Cancellable false
    //
    // @Triggers when a BetterModel tracker (an active model instance, entity-bound or dummy) is closed/removed.
    //
    // @Context
    // <context.model> returns the BMModelTag of the closed tracker.
    // <context.model_name> returns the name of the model.
    // <context.dummy> returns whether the tracker was a location-bound dummy (no source entity).
    // <context.entity> returns the source EntityTag, if the tracker was entity-bound.
    // <context.location> returns the source entity's LocationTag, if the tracker was entity-bound.
    //
    // -->

    public static BMTrackerClosedEvent instance;

    public BMTrackerClosedEvent() {
        instance = this;
        registerCouldMatcher("bm tracker closed");
    }

    /** Fired by the compat event sink. */
    public static void handle(BmTracker tracker) {
        if (instance != null) {
            instance.tracker = tracker;
            instance.fire();
        }
    }
}
