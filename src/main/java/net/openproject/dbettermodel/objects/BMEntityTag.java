package net.openproject.dbettermodel.objects;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Entity;

/**
 * Denizen ObjectTag for a BetterModel‐tracked entity
 */
public class BMEntityTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name BMEntityTag
    // @prefix bmentity
    // @base ElementTag
    // @format
    // The identity format for modeled entities is <uuid>
    // Where <uuid> is the UUID of the base entity.
    // For example: 'bmentity@dfc67056-b15d-45dd-b239-482d92e482e5'.
    //
    // @plugin DBetterModel
    // @description
    // Represents an entity that has one or more models on it.
    //
    // -->

    public static final String PREFIX = "bmentity";

    public static BMEntityTag valueOf(String string) {
        return valueOf(string, null);
    }

    /**
     * Парсер тега bmentity@<uuid> для получения EntityTracker, BMEntityTag
     */
    @Fetchable("bmentity")
    public static BMEntityTag valueOf(String string, TagContext context) {
        if (string == null) return null;

        String lower = CoreUtilities.toLowerCase(string);
        if (!lower.startsWith(PREFIX + "@")) return null;

        String uuidPart = lower.substring((PREFIX + "@").length());
        try {
            Entity entity = EntityTag.valueOf(uuidPart, context).getBukkitEntity();
            if (entity == null) return null;

            EntityTrackerRegistry registry = EntityTrackerRegistry.registry(entity); //
            if (registry == null || !EntityTrackerRegistry.hasModelData(entity)) return null; //

            return new BMEntityTag(registry);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    private final EntityTrackerRegistry registry;

    /** Главный конструктор: хранит EntityTrackerRegistry **/
    public BMEntityTag(EntityTrackerRegistry registry) {
        this.registry = registry;
    }

    /** Получение BMEntity из Bukkit-entity **/
    public BMEntityTag(Entity entity) {
        this(EntityTrackerRegistry.registry(entity));
    }

    public EntityTrackerRegistry getRegistry() {
        return registry;
    }

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { prefix = s; return this; }
    @Override public boolean isUnique() { return true; }

    @Override
    public String identify() {
        return PREFIX + "@" + registry.entity().getUniqueId(); //
    }

    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return registry; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMEntityTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMEntityTag.base_entity>
        // @returns EntityTag
        // @plugin DBetterModel
        // @description
        // Returns the base bukkit entity.
        // -->
        tagProcessor.registerTag(EntityTag.class, "base_entity", (attr, obj) ->
                new EntityTag(obj.getRegistry().entity()) //
        );

        // <--[tag]
        // @attribute <BMEntityTag.model[<model_name>]>
        // @returns BMModelTag
        // @plugin DBetterModel
        // @description
        // Returns the model with the specified name on the entity.
        // -->
        tagProcessor.registerTag(BMModelTag.class, "model", (attr, obj) -> {
            if (!attr.hasContext(1)) {
                Debug.echoError("The tag BMEntityTag.model[...] must have a model name specified.");
                return null;
            }
            String modelName = attr.getContext(1);
            EntityTracker tracker = obj.getRegistry().tracker(modelName); //
            if (tracker == null) {
                Debug.echoError("The entity does not have a model named '" + modelName + "'.");
                return null;
            }
            return new BMModelTag(tracker);
        });
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override public void adjust(Mechanism mech) {
        tagProcessor.processMechanism(this, mech);
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMEntityTag!");
    }
}
