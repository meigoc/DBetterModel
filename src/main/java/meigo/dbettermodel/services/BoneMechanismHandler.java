package meigo.dbettermodel.services;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.QuaternionTag;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class BoneMechanismHandler {

    private final Map<String, BiConsumer<BoneController, Mechanism>> handlers = new HashMap<>();

    public BoneMechanismHandler() {
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put("tint", this::handleTint);
        handlers.put("visible", this::handleVisible);
        handlers.put("item", this::handleItem);
        handlers.put("offset", this::handleOffset);
        handlers.put("scale", this::handleScale);
        handlers.put("rotate", this::handleRotate);
        handlers.put("interpolation_duration", this::handleInterpolationDuration);
        handlers.put("glow", this::handleGlow);
        handlers.put("glow_color", this::handleGlowColor);
        handlers.put("brightness", this::handleBrightness);
    }

    public void handle(BoneController controller, Mechanism mechanism) {
        handlers.getOrDefault(mechanism.getName(), (c, m) -> Debug.echoError("Unknown mechanism '" + m.getName() + "' for BMBoneTag."))
                .accept(controller, mechanism);
    }

    private void handleTint(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireInteger()) {
            controller.setTint(mechanism.getValue().asInt());
        }
    }

    private void handleVisible(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireBoolean()) {
            controller.setVisible(mechanism.getValue().asBoolean());
        }
    }

    private void handleItem(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(ItemTag.class)) {
            controller.setItem(mechanism.valueAsType(ItemTag.class).getItemStack());
        }
    }

    private void handleOffset(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(LocationTag.class)) {
            LocationTag loc = mechanism.valueAsType(LocationTag.class);
            controller.setOffset(new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ()));
        }
    }

    private void handleScale(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(LocationTag.class)) {
            LocationTag loc = mechanism.valueAsType(LocationTag.class);
            controller.setScale(new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ()));
        }
    }

    private void handleRotate(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(QuaternionTag.class)) {
            QuaternionTag quat = mechanism.valueAsType(QuaternionTag.class);
            controller.setRotation(new Quaternionf(quat.x, quat.y, quat.z, quat.w));
        }
    }

    private void handleInterpolationDuration(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(DurationTag.class)) {
            controller.setInterpolationDuration(mechanism.valueAsType(DurationTag.class).getTicksAsInt());
        }
    }

    private void handleGlow(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireBoolean()) {
            controller.setGlow(mechanism.getValue().asBoolean());
        }
    }

    private void handleGlowColor(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireInteger()) {
            controller.setGlowColor(mechanism.getValue().asInt());
        }
    }

    private void handleBrightness(BoneController controller, Mechanism mechanism) {
        ListTag list = mechanism.valueAsType(ListTag.class);
        if (list != null && list.size() == 2) {
            try {
                int blockLight = new ElementTag(list.get(0)).asInt();
                int skyLight = new ElementTag(list.get(1)).asInt();
                controller.setBrightness(blockLight, skyLight);
            } catch (Exception e) {
                Debug.echoError("Brightness mechanism requires a ListTag of two integers. Invalid input: " + list.identify());
            }
        } else {
            Debug.echoError("Brightness mechanism requires a ListTag of two integers.");
        }
    }
}