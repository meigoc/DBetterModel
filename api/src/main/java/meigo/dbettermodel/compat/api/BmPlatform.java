/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The version-neutral entry point implemented once per BetterModel line
 * ({@code meigo.dbettermodel.compat.vN.VNPlatform}). Instances are created reflectively
 * by the bootstrap via a public constructor taking {@code (org.bukkit.plugin.Plugin ownPlugin)}.
 *
 * <p>Types crossing this boundary: Bukkit API, JOML, java.util — never BetterModel types
 * (enforced by the {@code importBan} Gradle checks).</p>
 */
public interface BmPlatform {

    /** Installed BetterModel version string. */
    String bmVersion();

    /** What this layer can do on the running BetterModel line. */
    Set<BmCapability> capabilities();

    /** Names of all registered models. */
    Collection<String> modelNames();

    /** Names of all registered player limb models. */
    Collection<String> limbNames();

    /** Blueprint lookup by model name. */
    Optional<BmModel> model(String name);

    /** Blueprint lookup by limb model name. */
    Optional<BmModel> limb(String name);

    /** Whether the entity currently has any models attached. */
    boolean isModeled(Entity entity);

    /** Tracker of a named model on an entity. */
    Optional<BmTracker> tracker(Entity entity, String model);

    /** Tracker lookup by source entity UUID (entity may be unloaded). */
    Optional<BmTracker> tracker(UUID entityId, String model);

    /** All trackers attached to an entity. */
    List<BmTracker> trackers(Entity entity);

    /** All trackers by source entity UUID. */
    List<BmTracker> trackers(UUID entityId);

    /**
     * Attaches a model to an entity (create). Empty when the model does not exist.
     */
    Optional<BmTracker> attach(Entity entity, String model);

    /**
     * Removes a named model from an entity.
     *
     * @return true if the model was present and removed
     */
    boolean removeModel(Entity entity, String model);

    /**
     * Spawns a location-bound dummy tracker. Guarded by {@link BmCapability#DUMMY_TRACKERS}.
     */
    Optional<BmTracker> summon(Location location, String model);

    /** Player limb operations. */
    BmPlayerLimbs playerLimbs();

    /** Registers the callback sink; layers wire their line's event delivery to it. */
    void registerEvents(BmEventSink sink);

    /** Releases listeners/subscriptions and cached state. */
    void shutdown();
}
