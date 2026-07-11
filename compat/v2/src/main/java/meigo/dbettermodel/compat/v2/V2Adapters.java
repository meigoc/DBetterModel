/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.bukkit.platform.BukkitEntity;
import kr.toxicity.model.api.bukkit.platform.BukkitLocation;
import kr.toxicity.model.api.bukkit.platform.BukkitPlayer;
import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Platform-type -> Bukkit-type conversions (the reverse of {@code BukkitAdapter.adapt}).
 * On a Bukkit server the instanceof fast path always applies; UUID lookup is the safety net.
 */
final class V2Adapters {

    private V2Adapters() {
    }

    static Player bukkit(PlatformPlayer player) {
        if (player == null) return null;
        if (player instanceof BukkitPlayer bukkit) return bukkit.source();
        return Bukkit.getPlayer(player.uuid());
    }

    static Entity bukkit(PlatformEntity entity) {
        if (entity == null) return null;
        if (entity instanceof BukkitEntity bukkit) return bukkit.source();
        return Bukkit.getEntity(entity.uuid());
    }

    static Location bukkit(PlatformLocation location) {
        if (location instanceof BukkitLocation bukkit) return bukkit.source();
        return null;
    }
}
