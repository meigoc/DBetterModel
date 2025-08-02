package net.openproject.dbettermodel.objects;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.tracker.EntityTracker;

import java.util.Optional;

public class BMModelTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMModelTag
    // @prefix bmmodel
    // @base ElementTag
    // @format
    // The identity format for a BMModelTag is <uuid>,<model_name>
    // For example: 'bmmodel@dfc67056-b15d-45dd-b239-482d92e482e5,dummy'.
    //
    // @plugin DBetterModel
    // @description
    // Represents a specific model instance attached to an entity.
    //
    // -->

    public static final String PREFIX = "bmmodel";

    public static BMModelTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("bmmodel")
    public static BMModelTag valueOf(String string, TagContext context) {
        if (string == null) return null;

        String lower = CoreUtilities.toLowerCase(string);
        if (!lower.startsWith(PREFIX + "@")) return null;
        String body = lower.substring(PREFIX.length() + 1);

        String[] parts = body.split(",", 2);
        if (parts.length < 2) return null;

        EntityTag entityTag = EntityTag.valueOf(parts[0], context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        return BetterModel.registry(entityTag.getBukkitEntity())
                .map(registry -> registry.tracker(parts[1]))
                .map(BMModelTag::new)
                .orElse(null);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    private final EntityTracker tracker;

    public BMModelTag(EntityTracker tracker) {
        this.tracker = tracker;
    }

    public EntityTracker getTracker() { return tracker; }

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { this.prefix = s;
                                                    return this; }
    @Override public boolean isUnique() { return true; }

    @Override public String identify() {
        return PREFIX + "@"
                + tracker.registry().uuid()
                + "," + tracker.name();
    }

    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return tracker; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMModelTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMModelTag.bm_entity>
        // @returns BMEntityTag
        // @plugin DBetterModel
        // @description
        // Returns the parent BMEntityTag of this model.
        // -->
        tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attr, obj) ->
                new BMEntityTag(obj.getTracker().registry())
        );

        // <--[tag]
        // @attribute <BMModelTag.name>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the name of the model.
        // -->
        tagProcessor.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.getTracker().name())
        );

        // <--[tag]
        // @attribute <BMModelTag.bones>
        // @returns MapTag(BMBoneTag)
        // @plugin DBetterModel
        // @description
        // Returns a map of all the bones of the model, with the bone name as the key and the BMBoneTag as the value.
        // -->
        tagProcessor.registerTag(MapTag.class, "bones", (attr, obj) -> {
            MapTag map = new MapTag();
            for (RenderedBone bone : obj.getTracker().bones()) {
                map.putObject(bone.getName().name(), new BMBoneTag(obj.getTracker(), bone));
            }
            return map;
        });

        // <--[tag]
        // @attribute <BMModelTag.bone[<name>]>
        // @returns BMBoneTag
        // @plugin DBetterModel
        // @description
        // Returns the bone with the specified name from the model.
        // -->
        tagProcessor.registerTag(BMBoneTag.class, "bone", (attr, obj) -> {
            if (!attr.hasContext(1)) return null;
            String name = attr.getContext(1);
            return Optional.ofNullable(obj.getTracker().bone(name))
                    .map(bone -> new BMBoneTag(obj.getTracker(), bone))
                    .orElse(null);
        });

        // <--[tag]
        // @attribute <BMModelTag.get_animation_duration[<name>]>
        // @returns DurationTag
        // @plugin DBetterModel
        // @description
        // Returns the total duration of the specified animation.
        // -->
        tagProcessor.registerTag(DurationTag.class, "get_animation_duration", (attr, obj) -> {
            if (!attr.hasContext(1)) {
                attr.echoError("The get_animation_duration tag must have an animation name specified.");
                return null;
            }
            String animationName = attr.getContext(1);
            return obj.getTracker().renderer().animation(animationName)
                    .map(anim -> new DurationTag(anim.length()))
                    .orElse(null);
        });
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override public void adjust(Mechanism mechanism) {
        // <--[mechanism]
        // @object BMModelTag
        // @name interpolation_duration
        // @input DurationTag
        // @plugin DBetterModel
        // @description
        // Sets the movement interpolation duration for all bones in the model.
        // This affects how smoothly the model transitions between animation keyframes.
        // @tags
        // <BMModelTag.interpolation_duration>
        // @example
        // # Set the interpolation duration to 10 ticks for a smoother look
        // - adjust <[my_model]> interpolation_duration:10t
        // -->
        if (mechanism.matches("interpolation_duration") && mechanism.requireObject(DurationTag.class)) {
            int durationTicks = mechanism.valueAsType(DurationTag.class).getTicksAsInt();
            tracker.getPipeline().bones().forEach(bone -> bone.moveDuration(durationTicks));
        }

        // <--[mechanism]
        // @object BMModelTag
        // @name force_update
        // @input ElementTag(Boolean)
        // @plugin DBetterModel
        // @description
        // Forces an immediate visual update of the model for all viewers.
        // This is useful after making manual adjustments to bones (like tint, item, rotation)
        // to ensure the changes are sent to the client immediately.
        // Can be used without a value (defaults to true).
        // @tags
        // none
        // @example
        // # After changing a bone's item, force an update to make it visible
        // - adjust <[my_model].bone[sword]> item:<item[diamond_sword]>
        // - adjust <[my_model]> force_update
        // -->
        if (mechanism.matches("force_update")) {
            if (!mechanism.hasValue() || mechanism.requireBoolean()) {
                tracker.forceUpdate(true);
            }
        }

        tagProcessor.processMechanism(this, mechanism);
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMModelTag!");
    }
}
