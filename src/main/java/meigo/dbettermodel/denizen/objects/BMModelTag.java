package meigo.dbettermodel.denizen.objects;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BMModelTag implements ObjectTag, Adjustable {

    public static final String PREFIX = "bmmodel";

    @Fetchable("bmmodel")
    public static BMModelTag valueOf(String string, TagContext context) {
        if (string == null) return null;
        String lower = CoreUtilities.toLowerCase(string);
        if (!lower.startsWith(PREFIX + "@")) return null;
        String body = string.substring(PREFIX.length() + 1);

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
    @Override public String identify() { return PREFIX + "@" + tracker.registry().uuid() + "," + tracker.name(); }
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

        tagProcessor.registerTag(MapTag.class, "bones", (attr, obj) -> {
            MapTag map = new MapTag();
            UUID uuid = obj.getTracker().registry().uuid();
            String modelName = obj.getTracker().name();
            for (RenderedBone bone : obj.getTracker().bones()) {
                map.putObject(bone.name().name(), new BMBoneTag(uuid, modelName, bone.name().name()));
            }
            return map;
        });

        tagProcessor.registerTag(BMBoneTag.class, "bone", (attr, obj) -> {
            if (!attr.hasContext(1)) return null;
            String boneName = attr.getContext(1);
            return Optional.ofNullable(obj.getTracker().bone(boneName))
                    .map(bone -> new BMBoneTag(obj.getTracker().registry().uuid(), obj.getTracker().name(), bone.name().name()))
                    .orElse(null);
        });

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

        tagProcessor.registerTag(ListTag.class, "animations", (attr, obj) -> {
            ModelRenderer renderer = obj.getTracker().getPipeline().getParent();
            if (renderer == null) {
                attr.echoError("Could not retrieve model renderer for " + obj.identify());
                return null;
            }
            Set<String> animationNames = renderer.animations().keySet();
            return new ListTag(animationNames);
        });
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void adjust(Mechanism mechanism) {
        if (mechanism.matches("force_update")) {
            if (!mechanism.hasValue() || mechanism.requireBoolean()) {
                tracker.forceUpdate(true);
            }
        }
        tagProcessor.processMechanism(this, mechanism);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMModelTag!");
    }
}