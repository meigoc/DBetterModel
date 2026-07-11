/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen.events;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import meigo.dbettermodel.compat.api.BmRunningAnimation;
import meigo.dbettermodel.compat.api.BmTracker;
import org.bukkit.entity.Player;

public class BMAnimationStartEvent extends BMTrackerScriptEvent {

    // <--[event]
    // @Events
    // bm animation starts
    //
    // @Group DBetterModel
    //
    // @Switch model:<name> to only process the event if the model name matches.
    // @Switch animation:<name> to only process the event if the animation name matches (best-effort, see below).
    //
    // @Cancellable false
    //
    // @Triggers when an animation sequence starts playing on a model for a player.
    // Requires the ANIMATION_LIFECYCLE_EVENTS capability of the active compat layer (all supported BetterModel lines).
    // BetterModel only emits this for per-player animation playback, so the player context is always present.
    // BetterModel does not attach the animation name to the underlying event; it is resolved
    // best-effort from the tracker's running animation, so <context.animation> can be null
    // (and the 'animation:' switch will then not match) for animations started outside DBetterModel.
    //
    // @Context
    // <context.model> returns the BMModelTag whose animation started.
    // <context.model_name> returns the name of the model.
    // <context.animation> returns the name of the animation, when resolvable.
    // <context.entity> returns the source EntityTag, if the tracker is entity-bound.
    // <context.player> returns the PlayerTag the animation plays for.
    //
    // @Player Always.
    //
    // -->

    public static BMAnimationStartEvent instance;

    public Player player;
    public String animation;

    public BMAnimationStartEvent() {
        instance = this;
        registerCouldMatcher("bm animation starts");
        registerSwitches("animation");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "animation", animation)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(player == null ? null : new PlayerTag(player), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "animation" -> {
                return animation == null ? null : new ElementTag(animation);
            }
            case "player" -> {
                return player == null ? null : new PlayerTag(player);
            }
        }
        return super.getContext(name);
    }

    /** Fired by the compat event sink (ANIMATION_LIFECYCLE_EVENTS). */
    public static void handle(BmTracker tracker, Player player) {
        if (instance != null) {
            instance.tracker = tracker;
            instance.player = player;
            instance.animation = tracker == null ? null
                    : tracker.runningAnimation().map(BmRunningAnimation::name).orElse(null);
            instance.fire();
        }
    }
}
