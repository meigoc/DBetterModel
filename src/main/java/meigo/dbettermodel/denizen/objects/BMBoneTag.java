package meigo.dbettermodel.denizen.objects;

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
import meigo.dbettermodel.services.ModelService;
import org.bukkit.Location;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public class BMBoneTag implements ObjectTag, Adjustable {

    public static final String PREFIX = "bmbone";

    private final UUID entityUUID;
    private final String modelName;
    private final String boneName;

    public BMBoneTag(UUID entityUUID, String modelName, String boneName) {
        this.entityUUID = entityUUID;
        this.modelName = modelName;
        this.boneName = boneName;
    }

    @Fetchable("bmbone")
    public static BMBoneTag valueOf(String string, TagContext context) {
        if (string == null) return null;
        String lower = CoreUtilities.toLowerCase(string);
        if (!lower.startsWith(PREFIX + "@")) return null;
        String body = string.substring(PREFIX.length() + 1);

        String[] parts = body.split(",", 3);
        if (parts.length < 3) return null;

        EntityTag entityTag = EntityTag.valueOf(parts[0], context);
        if (entityTag == null || entityTag.getBukkitEntity() == null) return null;

        return new BMBoneTag(entityTag.getUUID(), parts[1], parts[2]);
    }

    public static boolean matches(String arg) {
        return arg != null && CoreUtilities.toLowerCase(arg).startsWith(PREFIX + "@");
    }

    public UUID getEntityUUID() { return entityUUID; }
    public String getModelName() { return modelName; }
    public String getBoneName() { return boneName; }

    private String prefix = PREFIX;
    @Override public String getPrefix() { return prefix; }
    @Override public ObjectTag setPrefix(String s) { this.prefix = s;
        return this; }
    @Override public boolean isUnique() { return true; }
    @Override public String identify() { return PREFIX + "@" + entityUUID + "," + modelName + "," + boneName; }
    @Override public String identifySimple() { return identify(); }
    @Override public Object getJavaObject() { return this; }
    @Override public String toString() { return identify(); }

    public static final ObjectTagProcessor<BMBoneTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTags() {
        tagProcessor.registerTag(ElementTag.class, "name", (attr, obj) -> new ElementTag(obj.boneName));

        tagProcessor.registerTag(LocationTag.class, "world_location", (attr, obj) -> {
            Location loc = ModelService.getInstance().getBoneWorldLocation(obj.entityUUID, obj.modelName, obj.boneName);
            return loc != null ? new LocationTag(loc) : null;
        });

        tagProcessor.registerTag(LocationTag.class, "world_rotation_euler", (attr, obj) -> {
            Vector3f euler = ModelService.getInstance().getBoneWorldRotationEuler(obj.entityUUID, obj.modelName, obj.boneName);
            return euler != null ? new LocationTag(null, euler.x, euler.y, euler.z) : null;
        });

        tagProcessor.registerTag(ElementTag.class, "is_visible", (attr, obj) ->
                new ElementTag(ModelService.getInstance().isBoneVisible(obj.entityUUID, obj.modelName, obj.boneName))
        );

        tagProcessor.registerTag(BMModelTag.class, "bm_model", (attr, obj) ->
                BetterModel.registry(obj.entityUUID)
                        .flatMap(registry -> Optional.ofNullable(registry.tracker(obj.modelName)))
                        .map(BMModelTag::new)
                        .orElse(null)
        );
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void adjust(Mechanism mechanism) {
        ModelService.getInstance().adjustBone(this, mechanism);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a BMBoneTag!");
    }
}