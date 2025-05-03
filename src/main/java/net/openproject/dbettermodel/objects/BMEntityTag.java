package net.openproject.dbettermodel.objects;

import com.denizenscript.denizen.objects.EntityFormObject;
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
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.tracker.EntityTracker;
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
    // Represents an entity that has a model on it.
    //
    // -->

    // ——————————————————————————
    // 1) Константы / Object Fetcher
    // ——————————————————————————

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
            EntityTracker tracker = BetterModel.inst().modelManager().tracker(entity);
            if (tracker == null) return null;
            return new BMEntityTag(tracker);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    // ——————————————————————————
    // 2) Поля и конструкторы
    // ——————————————————————————

    private final EntityTracker tracker;

    /** Главный конструктор: хранит EntityTracker **/
    public BMEntityTag(EntityTracker tracker) {
        this.tracker = tracker;
    }

    /** Получение BMEntity из Bukkit-entity **/
    public BMEntityTag(Entity entity) {
        this(BetterModel.inst().modelManager().tracker(entity));
    }

    public EntityTracker getTracker() {
        return tracker;
    }

    // ——————————————————————————
    // 3) ObjectTag API
    // ——————————————————————————

    private String prefix = PREFIX;

    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { prefix = s; return this; }
    @Override public boolean isUnique() { return true; }
    @Override public String identify() {
        return PREFIX + "@"
                + tracker.source().getUniqueId();
    }
    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return tracker; }
    @Override public String toString() { return identify(); }

    // ——————————————————————————
    // 4) Tags registration
    // ——————————————————————————

    public static final ObjectTagProcessor<BMEntityTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        // <--[tag]
        // @attribute <BMEntityTag.base_entity>
        // @returns EntityTag
        // @plugin DBetterModel
        // @description
        // Returns base entity.
        //
        // -->
        tagProcessor.registerTag(EntityFormObject.class, "base_entity", (attr, obj) ->
                EntityTag.valueOf(obj.tracker.source().getUniqueId().toString(), attr.context)
        );

        // <--[tag]
        // @attribute <BMEntityTag.model>
        // @returns BMModelTag
        // @plugin DBetterModel
        // @description
        // Returns the model with the specified name on the modeled entity.
        //
        // -->
        tagProcessor.registerTag(BMModelTag.class, "model", (attr, obj) ->
                new BMModelTag(obj.tracker)
        );
    }

    @Override public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    // ——————————————————————————
    // 5) Mechanisms
    // ——————————————————————————

    @Override public void adjust(Mechanism mech) {
        tagProcessor.processMechanism(this, mech);
    }

    @Override public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMEntityTag!");
    }
}
