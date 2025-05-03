package net.openproject.dbettermodel.objects;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.tracker.EntityTracker;

/**
 * Denizen ObjectTag for a specific model instance on an entity
 */
public class BMModelTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMModelTag
    // @prefix bmmodel
    // @base ElementTag
    // @format
    // The identity format for model is <uuid> + <model_name>
    // Where <uuid> is the UUID of the base entity, and <model_name> is the name of the model.
    // For example: 'bmentity@dfc67056-b15d-45dd-b239-482d92e482e5,dummy'.
    //
    // @plugin DBetterModel
    // @description
    // Represents a model that it attached to an entity.
    //
    // -->

    // ——————————————————————————
    // 1) Константы / Object Fetcher
    // ——————————————————————————

    public static final String PREFIX = "bmmodel";

    /** Парсер тега bmmodel@<uuid>,<modelName> для получения EntityTracker, BMEntityTag **/

    public static BMModelTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("bmmodel")
    public static BMModelTag valueOf(String string, TagContext context) {
        if (string == null) return null;

        String lower = CoreUtilities.toLowerCase(string);
        if (!lower.startsWith(PREFIX + "@")) return null;
        String body = lower.substring((PREFIX + "@").length());

        String[] parts = body.split(",", 2);
        if (parts.length < 2) return null;

        EntityTag entityTag = EntityTag.valueOf(parts[0], context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        EntityTracker tracker = BetterModel.inst().modelManager().tracker(entityTag.getBukkitEntity());
        if (tracker == null) return null;

        RenderInstance inst = tracker.getInstance();
        return inst == null ? null : new BMModelTag(tracker, inst);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    // ——————————————————————————
    // 2) Поля и конструкторы
    // ——————————————————————————

    private final EntityTracker tracker;
    private final RenderInstance instance;

    /** Служебный конструктор из трекера + инстанса **/
    public BMModelTag(EntityTracker tracker, RenderInstance instance) {
        this.tracker  = tracker;
        this.instance = instance;
    }

    /** Получение BMModel из трекера **/
    public BMModelTag(EntityTracker tracker) {
        this(tracker, tracker.getInstance());
    }

    public EntityTracker getTracker() { return tracker; }
    public RenderInstance getInstance() { return instance; }

    // ——————————————————————————
    // 3) ObjectTag API
    // ——————————————————————————

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { prefix = s; return this; }
    @Override public boolean isUnique() { return true; }
    @Override public String identify() {
        return PREFIX + "@"
                + tracker.uuid()
                + "," + instance.getParent().name();
    }
    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return instance; }
    @Override public String toString() { return identify(); }

    // ——————————————————————————
    // 4) Tags registration
    // ——————————————————————————

    public static final ObjectTagProcessor<BMModelTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMModelTag.bm_entity>
        // @returns BMEntityTag
        // @plugin DBetterModel
        // @description
        // Returns the bmentity of the model.
        //
        // -->
        tagProcessor.registerTag(BMEntityTag.class, "bm_entity", (attr, obj) ->
                new BMEntityTag(obj.tracker)
        );

        // <--[tag]
        // @attribute <BMModelTag.name>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the name of the model.
        //
        // -->
        tagProcessor.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.instance.getParent().name())
        );

        // <--[tag]
        // @attribute <BMModelTag.bones>
        // @returns MapTag(BMBoneTag)
        // @plugin DBetterModel
        // @description
        // Returns a map of all the bones of the model, which the bone id as the key and the bone as the value.
        //
        // -->
        tagProcessor.registerTag(MapTag.class, "bones", (attr, obj) -> {
            MapTag map = new MapTag();
            for (RenderedBone bone : obj.instance.bones()) {
                map.putObject(bone.getName().name(),
                        new BMBoneTag(obj.tracker, obj.instance, bone));
            }
            return map;
        });

        // <--[tag]
        // @attribute <BMModelTag.bone[<id>]>
        // @returns BMBoneTag
        // @plugin DBetterModel
        // @description
        // Returns the bone with the specified id of the model.
        //
        // -->
        tagProcessor.registerTag(BMBoneTag.class, "bone", (attr, obj) -> {
            String id = attr.getParam();
            if (id == null) return null;
            for (RenderedBone bone : obj.instance.bones()) {
                if (bone.getName().name().equalsIgnoreCase(id)) {
                    return new BMBoneTag(obj.tracker, obj.instance, bone);
                }
            }
            return null;
        });
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    // ——————————————————————————
    // 5) Mechanisms
    // ——————————————————————————

    @Override public void adjust(Mechanism mechanism) {
        tagProcessor.processMechanism(this, mechanism);
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMModelTag!");
    }
}
