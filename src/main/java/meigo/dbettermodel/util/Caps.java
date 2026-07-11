/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.util;

import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmCapability;
import meigo.dbettermodel.compat.api.BmPlatform;

/** Uniform capability gating for the Denizen surface: one clean error line, never a stacktrace. */
public final class Caps {

    private Caps() {}

    /** Whether the active layer declares the capability. False (silently) when no layer is up. */
    public static boolean has(BmCapability capability) {
        BmPlatform platform = DBetterModel.platform();
        return platform != null && platform.capabilities().contains(capability);
    }

    /**
     * Checks the capability and reports a one-line error naming the missing capability and
     * the minimum BetterModel version when absent.
     *
     * @return true when the capability is available
     */
    public static boolean require(ScriptEntry entry, BmCapability capability) {
        BmPlatform platform = DBetterModel.platform();
        if (platform == null) {
            Debug.echoError("DBetterModel compat layer is not available.");
            return false;
        }
        if (!platform.capabilities().contains(capability)) {
            Debug.echoError("This requires the " + capability + " capability, which BetterModel "
                    + platform.bmVersion() + " does not provide — need BetterModel " + minVersion(capability) + " or newer.");
            return false;
        }
        return true;
    }

    /** Human hint: minimum BetterModel version that provides the capability (per COMPAT-MATRIX). */
    public static String minVersion(BmCapability capability) {
        return switch (capability) {
            case EVENT_BUS -> "2.0.1";
            case TRACKER_ANIMATION_API -> "2.2.0";
            case ANIMATION_PRIORITY -> "3.0.0";
            case ANIMATION_LIFECYCLE_EVENTS -> "2.0.1";
            case HITBOX_DAMAGE_EVENTS -> "1.15.2 (2.1.0+ on the 2.x line)";
            default -> "1.15.2";
        };
    }
}
