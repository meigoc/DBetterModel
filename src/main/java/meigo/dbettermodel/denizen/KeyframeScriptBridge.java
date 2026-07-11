/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmTracker;
import meigo.dbettermodel.denizen.objects.BMModelTag;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Experimental: runs a Denizen task script straight from a Blockbench keyframe.
 * Keyframe format: {@code denizen:run{script=knight_strike;key=value}} — the reserved
 * signal name 'run' starts the named task with every other metadata entry as a
 * definition, plus 'model', 'model_name' and 'player' when resolvable.
 * Gated by the 'experimental.keyframe-script-bridge' config option.
 * The regular 'bm animation signal' event still fires alongside.
 */
public final class KeyframeScriptBridge {

    public static final String SIGNAL = "run";

    private KeyframeScriptBridge() {
    }

    public static void handle(BmTracker tracker, Map<String, String> metadata, Player player) {
        DBetterModel plugin = DBetterModel.getInstance();
        if (plugin == null || !plugin.getConfig().getBoolean("experimental.keyframe-script-bridge", true)) {
            return;
        }
        String scriptName = metadata.get("script");
        if (scriptName == null || scriptName.isEmpty()) {
            Debug.echoError("Keyframe script bridge: 'run' signal without a script= key.");
            return;
        }
        ScriptTag script = ScriptTag.valueOf(scriptName, CoreUtilities.noDebugContext);
        if (script == null || script.getContainer() == null) {
            Debug.echoError("Keyframe script bridge: script '" + scriptName + "' not found.");
            return;
        }
        PlayerTag playerTag = player == null ? null : new PlayerTag(player);
        ScriptEntryData data = new BukkitScriptEntryData(playerTag, null);
        ScriptUtilities.createAndStartQueue(script.getContainer(), null, data, null, queue -> {
            metadata.forEach((key, value) -> {
                if (!key.equals("script")) {
                    queue.addDefinition(key, new ElementTag(value));
                }
            });
            if (tracker != null) {
                queue.addDefinition("model", new BMModelTag(tracker));
                queue.addDefinition("model_name", new ElementTag(tracker.name()));
            }
            if (playerTag != null) {
                queue.addDefinition("player", playerTag);
            }
        }, null, null, null, null);
    }
}
