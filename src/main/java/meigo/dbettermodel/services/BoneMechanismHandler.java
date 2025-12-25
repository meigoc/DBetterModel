/*
 * Copyright 2025 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.services;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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
        handlers.put("view_range", this::handleViewRange);
        handlers.put("shadow_radius", this::handleShadowRadius);
        handlers.put("billboard", this::handleBillboard);
    }

    public void handle(BoneController controller, Mechanism mechanism) {
        BiConsumer<BoneController, Mechanism> handler = handlers.get(mechanism.getName());

        if (handler != null) {
            handler.accept(controller, mechanism);
            mechanism.fulfill();
        }
    }

    private void handleTint(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireInteger()) {
            controller.setTint(mechanism.getValue().asInt());
        }
    }

    private void handleVisible(BoneController controller, Mechanism mechanism) {
        boolean visible;
        ListTag targets = null;

        if (mechanism.getValue().canBeType(ListTag.class)) {
            ListTag list = mechanism.getValue().asType(ListTag.class, mechanism.context);
            if (list.isEmpty() || !list.getObject(0).canBeType(ElementTag.class) || !list.getObject(0).asElement().isBoolean()) {
                mechanism.echoError("If using a ListTag for 'visible', the first element must be a boolean (true/false).");
                return;
            }
            visible = list.getObject(0).asElement().asBoolean();
            if (list.size() > 1) {
                targets = new ListTag(list.subList(1, list.size()));
            }
        } else if (mechanism.requireBoolean()) {
            visible = mechanism.getValue().asBoolean();
        } else {
            return;
        }

        if (targets == null || targets.isEmpty()) {
            controller.setVisible(visible);
        } else {
            List<Player> players = targets.filter(PlayerTag.class, mechanism.context)
                    .stream()
                    .map(PlayerTag::getPlayerEntity)
                    .collect(Collectors.toList());
            if (!players.isEmpty()) {
                controller.setVisible(visible, players);
            }
        }
    }

    private void handleItem(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(ItemTag.class)) {
            controller.setItem(mechanism.getValue().asType(ItemTag.class, mechanism.context).getItemStack());
        }
    }

    private void handleOffset(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(LocationTag.class)) {
            LocationTag loc = mechanism.getValue().asType(LocationTag.class, mechanism.context);
            controller.setOffset(new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ()));
        }
    }

    private void handleScale(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(LocationTag.class)) {
            LocationTag loc = mechanism.getValue().asType(LocationTag.class, mechanism.context);
            controller.setScale(new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ()));
        }
    }

    private void handleRotate(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(QuaternionTag.class)) {
            QuaternionTag quat = mechanism.getValue().asType(QuaternionTag.class, mechanism.context);
            controller.setRotation(new Quaternionf(quat.x, quat.y, quat.z, quat.w));
        }
    }

    private void handleViewRange(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireFloat()) {
            controller.setViewRange(mechanism.getValue().asFloat());
        }
    }

    private void handleInterpolationDuration(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireObject(DurationTag.class)) {
            controller.setInterpolationDuration(mechanism.getValue().asType(DurationTag.class, mechanism.context).getTicksAsInt());
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
        if (mechanism.getValue().canBeType(ListTag.class)) {
            ListTag list = mechanism.getValue().asType(ListTag.class, mechanism.context);
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
        } else {
            Debug.echoError("Brightness mechanism requires a ListTag of two integers.");
        }
    }

    private void handleShadowRadius(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireFloat()) {
            controller.setShadowRadius(mechanism.getValue().asFloat());
        }
    }

    private void handleBillboard(BoneController controller, Mechanism mechanism) {
        if (mechanism.requireEnum(Display.Billboard.class)) {
            controller.setBillboard(Display.Billboard.valueOf(mechanism.getValue().asString().toUpperCase()));
        }
    }
}