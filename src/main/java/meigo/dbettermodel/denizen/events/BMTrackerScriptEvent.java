/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.denizen.objects.BMModelTag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * Base for events carrying a tracker: shared 'model' switch plus the
 * model/model_name/entity/dummy/location context set.
 */
public abstract class BMTrackerScriptEvent extends BukkitScriptEvent {

    public BmTracker tracker;

    protected BMTrackerScriptEvent() {
        registerSwitches("model");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (tracker != null && !runGenericSwitchCheck(path, "model", tracker.name())) {
            return false;
        }
        return super.matches(path);
    }

    protected Entity sourceEntity() {
        UUID uuid = tracker == null ? null : tracker.entityUuid();
        return uuid == null ? null : Bukkit.getEntity(uuid);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (tracker != null) {
            switch (name) {
                case "model" -> {
                    return new BMModelTag(tracker);
                }
                case "model_name" -> {
                    return new ElementTag(tracker.name());
                }
                case "dummy" -> {
                    return new ElementTag(tracker.entityUuid() == null);
                }
                case "entity" -> {
                    Entity entity = sourceEntity();
                    return entity == null ? null : new EntityTag(entity);
                }
                case "location" -> {
                    // The compat api exposes no tracker location; dummies report null here.
                    Entity entity = sourceEntity();
                    return entity == null ? null : new LocationTag(entity.getLocation());
                }
            }
        }
        return super.getContext(name);
    }
}
