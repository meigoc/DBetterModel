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
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.BonePredicate;
import kr.toxicity.model.api.util.TransformedItemStack;
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
    // The identity format for a BMBoneTag is <uuid> + <model_name> + <bone_id>
    // Where <uuid> is the UUID of the base entity, <model_name> is the name of the model, and <bone_id> is the internal name/id of the bone.
    // For example: 'bmbone@dfc67056-b15d-45dd-b239-482d92e482e5,dummy,head'.
    //
    // @plugin DBetterModel
    // @description
    // Represents a bone in an BMModel.
    //
    // -->

    // ——————————————————————————
    // 1) Константы / Object Fetcher
    // ——————————————————————————

    public static final String PREFIX = "bmbone";

    /** Парсер тега bmbone@<uuid>,<modelName>,<boneId> для получения EntityTracker, BMEntityTag, BMBoneTag**/
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

        // parts[0]=entity UUID, parts[1]=model name, parts[2]=bone id
        EntityTag entityTag = EntityTag.valueOf(parts[0], context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        EntityTracker tracker = EntityTracker.tracker(entityTag.getBukkitEntity());
        if (tracker == null) return null;

        RenderInstance inst = tracker.getInstance();
        RenderedBone bone = inst.boneOf(b -> b.getName().name().equals(parts[2]));
        if (bone == null) return null;

        return new BMBoneTag(tracker, inst, bone);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    // ——————————————————————————
    // 2) Поля и конструкторы
    // ——————————————————————————

    private final EntityTracker tracker;
    private final RenderInstance instance;
    private final RenderedBone bone;

    /** Конструктор из трекера + инстанса + кости **/
    public BMBoneTag(EntityTracker tracker, RenderInstance instance, RenderedBone bone) {
        this.tracker  = tracker;
        this.instance = instance;
        this.bone     = bone;
    }

    public EntityTracker getTracker() { return tracker; }
    public RenderInstance getInstance() { return instance; }
    public RenderedBone getBone() { return bone; }

    // ——————————————————————————
    // 3) ObjectTag API
    // ——————————————————————————

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { prefix = s; return this; }
    @Override public boolean isUnique() { return true; }

    @Override
    public String identify() {
        return PREFIX + "@"
                + tracker.uuid()
                + "," + instance.getParent().name()
                + "," + bone.getName().name();
    }

    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return bone; }
    @Override public String toString() { return identify(); }

    // ——————————————————————————
    // 4) Tags Registration
    // ——————————————————————————

    public static final ObjectTagProcessor<BMBoneTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMBoneTag.name>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the id of the bone.
        //
        // -->
        tagProcessor.registerTag(ElementTag.class, "name", (attr,obj) ->
                new ElementTag(obj.getBone().getName().name())
        );

        // <--[tag]
        // @attribute <BMBoneTag.global_position>
        // @returns LocationTag
        // @plugin DBetterModel
        // @description
        // Returns the position of the bone as a vector.
        //
        // -->
        tagProcessor.registerTag(LocationTag.class, "global_position", (attr,obj) -> {
            Vector pos = Vector.fromJOML(obj.getBone().worldPosition());
            return new LocationTag(pos);
        });

        // <--[tag]
        // @attribute <BMBoneTag.bm_model>
        // @returns BMModelTag
        // @plugin DBetterModel
        // @description
        // Returns the bmmodel of the bone.
        //
        // -->
        tagProcessor.registerTag(BMModelTag.class, "bm_model", (attr,obj) ->
                new BMModelTag(obj.tracker, obj.instance)
        );

        // <--[tag]
        // @attribute <BMBoneTag.bm_entity>
        // @returns BMEntityTag
        // @plugin DBetterModel
        // @description
        // Returns the bm_entity of the bone.
        //
        // -->
        tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attr,obj) ->
                new BMEntityTag(obj.tracker)
        );
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    // ——————————————————————————
    // 5) Mechanisms
    // ——————————————————————————

    /**
     * Механизмы для костей:
     *   - adjust <bone> item:<itemTag>
     *   - adjust <bone> scale:<location>
     *   - adjust <bone> pivot_offset:<location>
     */
    @Override public void adjust(Mechanism mechanism) {
        tagProcessor.processMechanism(this, mechanism);
    }

    static {
        // <--[mechanism]
        // @object BMBoneTag
        // @name scale
        // @input LocationTag
        // @plugin DBetterModel
        // @description
        // Sets the scale of the bone.
        //
        // -->
        tagProcessor.registerMechanism("scale", false, LocationTag.class, (object, mech, value) -> {
            Vector3f scale = new Vector3f(
                    (float)value.getX(),
                    (float)value.getY(),
                    (float)value.getZ());

            object.bone.addAnimationMovementModifier(
                    BonePredicate.of(BonePredicate.State.NOT_SET, b -> true),
                    mov -> {
                        assert mov.scale() != null;
                        mov.scale().set(scale);
                    }
            );
        });

        // <--[mechanism]
        // @object BMBoneTag
        // @name item
        // @input ListTag
        // @plugin DBetterModel
        // @description
        // Sets the item that bone uses (<list[item|offset]>)
        //
        // -->
        tagProcessor.registerMechanism("item", false, ListTag.class, (object, mech, list) -> {
            if (list.isEmpty()) {
                Debug.echoError("The ListTag must contain at least one element.");
                return;
            }

            ObjectTag itemObj = list.getObject(0);
            if (!(itemObj instanceof ItemTag itemTag)) {
                Debug.echoError("'item' key must be a valid ItemTag.");
                return;
            }

            Vector3f localOffset = new Vector3f(0f, 0f, 0f);

            if (list.size() > 1) {
                ObjectTag offsetObj = list.getObject(1);
                if (!(offsetObj instanceof LocationTag loc)) {
                    Debug.echoError("'offset' key must be a LocationTag.");
                    return;
                }
                localOffset.set(
                        (float) loc.getX(),
                        (float) loc.getY(),
                        (float) loc.getZ()
                );
            }

            Vector3f globalOffset = new Vector3f(0f, 0f, 0f);
            TransformedItemStack tis = new TransformedItemStack(
                    globalOffset,
                    localOffset,
                    new Vector3f(1f, 1f, 1f),
                    itemTag.getItemStack()
            );
            object.getBone().itemStack(BonePredicate.TRUE, tis);
        });
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMBoneTag!");
    }
}
