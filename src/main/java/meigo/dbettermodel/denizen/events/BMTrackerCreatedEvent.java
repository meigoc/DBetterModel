/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import meigo.dbettermodel.compat.api.BmTracker;

public class BMTrackerCreatedEvent extends BMTrackerScriptEvent {

    // <--[event]
    // @Events
    // bm tracker created
    //
    // @Group DBetterModel
    //
    // @Switch model:<name> to only process the event if the model name matches.
    //
    // @Cancellable false
    //
    // @Triggers when a BetterModel tracker (an active model instance, entity-bound or dummy) is created.
    //
    // @Context
    // <context.model> returns the BMModelTag of the created tracker.
    // <context.model_name> returns the name of the model.
    // <context.dummy> returns whether the tracker is a location-bound dummy (no source entity).
    // <context.entity> returns the source EntityTag, if the tracker is entity-bound.
    // <context.location> returns the source entity's LocationTag, if the tracker is entity-bound.
    //
    // -->

    public static BMTrackerCreatedEvent instance;

    public BMTrackerCreatedEvent() {
        instance = this;
        registerCouldMatcher("bm tracker created");
    }

    /** Fired by the compat event sink. */
    public static void handle(BmTracker tracker) {
        if (instance != null) {
            instance.tracker = tracker;
            instance.fire();
        }
    }
}
