package net.openproject.dbettermodel.util;

import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.openproject.dbettermodel.DBetterModel;

/**
 * A utility for conditional logging that respects both global Denizen settings
 * and script-specific debug settings.
 */
public class DBMDebug {

    /**
     * Outputs a general log message (e.g., on plugin startup/shutdown).
     * Controlled by the 'options.enable-plugin-logging' option in config.yml.
     *
     * @param message The message to output.
     */
    public static void log(String message) {
        if (DBetterModel.enablePluginLogging) {
            Debug.log(message);
        }
    }

    /**
     * Outputs a success message if debugging is enabled for the ScriptEntry.
     *
     * @param entry   The current ScriptEntry.
     * @param message The message to output.
     */
    public static void approval(ScriptEntry entry, String message) {
        if (entry != null && entry.shouldDebug()) {
            Debug.echoApproval(message);
        }
    }

    /**
     * Outputs an error message if debugging is enabled for the ScriptEntry.
     *
     * @param entry   The current ScriptEntry.
     * @param message The error message.
     */
    public static void error(ScriptEntry entry, String message) {
        if (entry != null && entry.shouldDebug()) {
            Debug.echoError(message);
        }
    }

    /**
     * Outputs the stack trace of an exception. These errors are critical and should
     * always be displayed, regardless of debug settings.
     *
     * @param ex The exception to output.
     */
    public static void error(Throwable ex) {
        Debug.echoError(ex);
    }
}
