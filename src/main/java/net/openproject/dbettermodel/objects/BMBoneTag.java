package net.openproject.dbettermodel.objects;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

/**
 * Denizen ObjectTag for a single bone in a BetterModel RenderInstance.
 */
public class BMBoneTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMBoneTag
    // @prefix bmbone
    // @base ElementTag
    // @format
    // The identity format for a BMBoneTag is <uuid>|<model_name>|<bone_id>
    // Where <uuid> is the UUID of the base entity, <model_name> is the name of the model, and <bone_id> is the internal name/id of the bone.
    // For example: 'bmbone@dfc67056-b15d-45dd-b239-482d92e482e5,dummy,head'.
    //
    // @plugin DBetterModel
    // @description
    // Represents a bone in an BMModel.
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
        String body = lower.substring((PREFIX + "@").length());

        String[] parts = body.split(",", 3);
        if (parts.length < 3) return null;

        EntityTag entityTag = EntityTag.valueOf(parts[0], context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        EntityTrackerRegistry registry = EntityTrackerRegistry.registry(entityTag.getBukkitEntity()); //
        if (registry == null) return null;

        EntityTracker tracker = registry.tracker(parts[1]); //
        if (tracker == null) return null;

        RenderedBone bone = tracker.bone(parts[2]); // [cite: 17]
        if (bone == null) return null;

        return new BMBoneTag(tracker, bone);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    private final EntityTracker tracker;
    private final RenderedBone bone;

    public BMBoneTag(EntityTracker tracker, RenderedBone bone) {
        this.tracker = tracker;
        this.bone    = bone;
    }

    public EntityTracker getTracker() { return tracker; }
    public RenderedBone getBone() { return bone; }

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { prefix = s; return this; }
    @Override public boolean isUnique() { return true; }

    @Override
    public String identify() {
        return PREFIX + "@"
                + tracker.registry().uuid() //
                + "," + tracker.name() // [cite: 14]
                + "," + bone.getName().name(); // [cite: 26]
    }

    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return bone; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMBoneTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        tagProcessor.registerTag(ElementTag.class, "name", (attr,obj) ->
                new ElementTag(obj.getBone().getName().name()) // [cite: 26]
        );

        tagProcessor.registerTag(LocationTag.class, "global_position", (attr,obj) -> {
            Vector3f worldPos = obj.getBone().worldPosition(); //
            Vector pos = new Vector(worldPos.x, worldPos.y, worldPos.z);
            return new LocationTag(obj.getTracker().sourceEntity().getWorld(), pos.getX(), pos.getY(), pos.getZ()); //
        });

        tagProcessor.registerTag(BMModelTag.class, "bm_model", (attr,obj) ->
                new BMModelTag(obj.tracker)
        );

        tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attr,obj) ->
                new BMEntityTag(obj.getTracker().registry())
        );
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override public void adjust(Mechanism mechanism) {
        tagProcessor.processMechanism(this, mechanism);
    }

    static {
        // Механизм 'scale' был удален, так как метод addAnimationMovementModifier был удален из API,
        // и прямого аналога для изменения масштаба кости с помощью Vector3f не существует. (закиньте пулл реквест в случае решения)

        // <--[mechanism]
        // @object BMBoneTag
        // @name item
        // @input ListTag
        // @plugin DBetterModel
        // @description
        // Sets the item that the bone uses.
        // Input is a ListTag containing the ItemTag, and optionally a LocationTag for local offset.
        // Example: - adjust <[bone]> item:<[stick|l@0,0.5,0]>
        // -->
        tagProcessor.registerMechanism("item", false, ListTag.class, (object, mech, list) -> {
            if (list.isEmpty()) {
                Debug.echoError("The ListTag must contain at least one element (an ItemTag).");
                return;
            }

            ItemTag itemTag = list.getObject(0).asType(ItemTag.class, mech.context);
            if (itemTag == null) {
                Debug.echoError("'item' value must be a valid ItemTag.");
                return;
            }

            Vector3f localOffset = new Vector3f(0f, 0f, 0f);
            if (list.size() > 1) {
                LocationTag offsetLoc = list.getObject(1).asType(LocationTag.class, mech.context);
                if (offsetLoc == null) {
                    Debug.echoError("The second element in the list must be a LocationTag for the offset.");
                    return;
                }
                localOffset.set((float) offsetLoc.getX(), (float) offsetLoc.getY(), (float) offsetLoc.getZ());
            }

            TransformedItemStack tis = TransformedItemStack.of(
                    new Vector3f(0f, 0f, 0f), // position (global offset)
                    localOffset,                       // offset (local offset)
                    new Vector3f(1f, 1f, 1f), // scale
                    itemTag.getItemStack()
            );

            object.getBone().itemStack(BonePredicate.TRUE, tis);
        });
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMBoneTag!");
    }
}
