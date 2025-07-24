package net.openproject.dbettermodel.objects;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.Optional;

public class BMBoneTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMBoneTag
    // @prefix bmbone
    // @base ElementTag
    // @format
    // The identity format for a BMBoneTag is <uuid>,<model_name>,<bone_name>
    // For example: 'bmbone@dfc67056-b15d-45dd-b239-482d92e482e5,dummy,head'.
    //
    // @plugin DBetterModel
    // @description
    // Represents a single bone within a specific model instance on an entity.
    //
    // -->

    public static final String PREFIX = "bmbone";

    public static BMBoneTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("bmbone")
    public static BMBoneTag valueOf(String string, TagContext context) {
        if (string == null) return null;
        String lower = CoreUtilities.toLowerCase(string);

        if (!lower.startsWith(PREFIX + "@")) return null;
        String body = lower.substring(PREFIX.length() + 1);

        String[] parts = body.split(",", 3);
        if (parts.length < 3) return null;

        EntityTag entityTag = EntityTag.valueOf(parts[0], context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        return BetterModel.registry(entityTag.getBukkitEntity())
                .map(registry -> registry.tracker(parts[1]))
                .map(tracker -> Optional.ofNullable(tracker.bone(parts[2]))
                        .map(bone -> new BMBoneTag(tracker, bone))
                        .orElse(null))
                .orElse(null);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    private final EntityTracker tracker;
    private final RenderedBone bone;

    public BMBoneTag(EntityTracker tracker, RenderedBone bone) {
        this.tracker = tracker;
        this.bone = bone;
    }

    public RenderedBone getBone() { return bone; }

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { this.prefix = s; return this; }
    @Override public boolean isUnique() { return true; }

    @Override
    public String identify() {
        return PREFIX + "@"
                + tracker.registry().uuid()
                + "," + tracker.name()
                + "," + bone.getName().name();
    }

    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return bone; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMBoneTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMBoneTag.name>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the name of the bone.
        // -->
        tagProcessor.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.getBone().getName().name())
        );

        // <--[tag]
        // @attribute <BMBoneTag.global_position>
        // @returns LocationTag
        // @plugin DBetterModel
        // @description
        // Returns the bone's current position in the world.
        // -->
        tagProcessor.registerTag(LocationTag.class, "global_position", (attr, obj) -> {
            Vector3f worldPos = obj.getBone().worldPosition();
            Vector pos = new Vector(worldPos.x(), worldPos.y(), worldPos.z());
            return new LocationTag(obj.tracker.sourceEntity().getWorld(), pos.getX(), pos.getY(), pos.getZ());
        });

        // <--[tag]
        // @attribute <BMBoneTag.is_visible>
        // @returns ElementTag(Boolean)
        // @plugin DBetterModel
        // @description
        // Returns whether the bone is currently visible.
        // -->
        tagProcessor.registerTag(ElementTag.class, "is_visible", (attr, obj) ->
                new ElementTag(obj.getBone().isVisible())
        );

        // <--[tag]
        // @attribute <BMBoneTag.bm_model>
        // @returns BMModelTag
        // @plugin DBetterModel
        // @description
        // Returns the parent model of this bone.
        // -->
        tagProcessor.registerTag(BMModelTag.class, "bm_model", (attr, obj) ->
                new BMModelTag(obj.tracker)
        );
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override public void adjust(Mechanism mechanism) {
        // <--[mechanism]
        // @object BMBoneTag
        // @name tint
        // @input ElementTag(Integer)
        // @plugin DBetterModel
        // @description
        // Applies a color tint to the bone's item. The color is specified as a single integer representing the RGB value.
        // For example, red is 16711680.
        // @tags
        // <BMBoneTag.tint>
        // -->
        if (mechanism.matches("tint") && mechanism.requireInteger()) {
            int color = mechanism.getValue().asInt();
            if (bone.tint(BonePredicate.TRUE, color)) {
                tracker.forceUpdate(true);
            }
        }

        tagProcessor.processMechanism(this, mechanism);
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMBoneTag!");
    }
}
