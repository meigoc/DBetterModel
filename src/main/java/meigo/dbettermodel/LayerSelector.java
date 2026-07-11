/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel;

/**
 * Pure BetterModel-version -> compat-layer decision, kept Bukkit-free so the bootstrap
 * logic is unit-testable. {@link DBetterModel#selectPlatform} owns logging and classloading.
 */
public final class LayerSelector {

    /** layerClass is null when no layer applies; reason then says why. */
    public record Selection(String layerClass, boolean bestEffort, String reason) {
        static Selection layer(String layerClass, boolean bestEffort) {
            return new Selection(layerClass, bestEffort, null);
        }

        static Selection none(String reason) {
            return new Selection(null, false, reason);
        }
    }

    private LayerSelector() {
    }

    public static Selection select(String version) {
        int major = -1;
        int minor = -1;
        try {
            String[] parts = version == null ? new String[0] : version.split("-")[0].split("\\.");
            if (parts.length >= 2) {
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException ignored) {
        }
        if (major < 0) {
            return Selection.none("could not parse the version string");
        }
        if (major == 1 && minor >= 15) {
            return Selection.layer("meigo.dbettermodel.compat.v1.V1Platform", false);
        }
        if (major == 2) {
            return Selection.layer("meigo.dbettermodel.compat.v2.V2Platform", false);
        }
        if (major == 3) {
            return Selection.layer("meigo.dbettermodel.compat.v3.V3Platform", false);
        }
        if (major >= 4) {
            // Unknown future major: the v3 layer probes its own signatures on load.
            return Selection.layer("meigo.dbettermodel.compat.v3.V3Platform", true);
        }
        return Selection.none("too old (need 1.15+)");
    }
}
