/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.event.ModelEvent;
import kr.toxicity.model.api.event.ModelEventApplication;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.ModelInteractionHand;
import kr.toxicity.model.api.platform.PlatformPlayer;

import java.lang.reflect.Method;

/**
 * Hitbox damage/interact wiring for BM 2.0.x, whose {@code ModelDamagedEvent} /
 * {@code ModelInteractEvent} classes were deleted in 2.1.0 and thus cannot be referenced
 * statically from a layer compiled against 2.2.0. Accessors are resolved by return type,
 * not name, to survive lombok naming details.
 */
final class V2HitboxLegacy {

    private V2HitboxLegacy() {
    }

    @SuppressWarnings("unchecked")
    static boolean register(V2Listener listener, ModelEventApplication application) {
        try {
            Class<? extends ModelEvent> damaged = (Class<? extends ModelEvent>)
                    Class.forName("kr.toxicity.model.api.event.ModelDamagedEvent");
            Class<? extends ModelEvent> interact = (Class<? extends ModelEvent>)
                    Class.forName("kr.toxicity.model.api.event.ModelInteractEvent");
            Method damagedHitBox = accessor(damaged, HitBox.class);
            Method damagedDamage = accessor(damaged, float.class);
            Method interactHitBox = accessor(interact, HitBox.class);
            Method interactWho = accessor(interact, PlatformPlayer.class);
            Method interactHand = accessor(interact, ModelInteractionHand.class);
            listener.subscribeRaw(application, damaged, event -> {
                try {
                    listener.fireDamaged((HitBox) damagedHitBox.invoke(event),
                            ((Number) damagedDamage.invoke(event)).doubleValue());
                } catch (ReflectiveOperationException ignored) {
                }
            });
            listener.subscribeRaw(application, interact, event -> {
                try {
                    listener.fireInteracted((HitBox) interactHitBox.invoke(event),
                            (PlatformPlayer) interactWho.invoke(event),
                            ((ModelInteractionHand) interactHand.invoke(event)).name());
                } catch (ReflectiveOperationException ignored) {
                }
            });
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Method accessor(Class<?> event, Class<?> returnType) throws NoSuchMethodException {
        for (Method method : event.getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() == returnType) return method;
        }
        throw new NoSuchMethodException(event.getName() + " accessor returning " + returnType.getName());
    }
}
