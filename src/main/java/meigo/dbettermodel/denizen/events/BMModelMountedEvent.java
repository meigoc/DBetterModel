/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import meigo.dbettermodel.compat.api.BmBone;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.denizen.objects.BMBoneTag;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class BMModelMountedEvent extends BMTrackerScriptEvent {

    // <--[event]
    // @Events
    // bm model mounted
    //
    // @Group DBetterModel
    //
    // @Switch model:<name> to only process the event if the model name matches.
    //
    // @Cancellable false
    //
    // @Triggers when an entity mounts a model bone hitbox (seat). Requires the MOUNT_EVENTS capability of the active compat layer.
    //
    // @Context
    // <context.model> returns the mounted BMModelTag.
    // <context.model_name> returns the name of the model.
    // <context.entity> returns the source EntityTag, if the tracker is entity-bound.
    // <context.bone> returns the seat BMBoneTag (entity-bound trackers only).
    // <context.bone_name> returns the name of the seat bone.
    // <context.passenger> returns the EntityTag that mounted.
    //
    // -->

    public static BMModelMountedEvent instance;

    public BmBone bone;
    public Entity passenger;

    public BMModelMountedEvent() {
        instance = this;
        registerCouldMatcher("bm model mounted");
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "bone" -> {
                UUID uuid = tracker == null ? null : tracker.entityUuid();
                return uuid == null || bone == null ? null : new BMBoneTag(uuid, tracker.name(), bone.name());
            }
            case "bone_name" -> {
                return bone == null ? null : new ElementTag(bone.name());
            }
            case "passenger" -> {
                return passenger == null ? null : new EntityTag(passenger);
            }
        }
        return super.getContext(name);
    }

    /** Fired by the compat event sink (MOUNT_EVENTS). */
    public static void handle(BmTracker tracker, BmBone bone, Entity passenger) {
        if (instance != null) {
            instance.tracker = tracker;
            instance.bone = bone;
            instance.passenger = passenger;
            instance.fire();
        }
    }
}
