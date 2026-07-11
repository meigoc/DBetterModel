/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.event.ModelEventApplication;
import kr.toxicity.model.api.event.hitbox.HitBoxDamagedEvent;
import kr.toxicity.model.api.event.hitbox.HitBoxInteractEvent;

/**
 * Hitbox damage/interact wiring for BM >= 2.1.0 ({@code event.hitbox.*} package).
 * Those classes do not exist in 2.0.x — this class must never be loaded there
 * (callers gate on the sub-line probe; JVM lazy loading keeps it safe).
 */
final class V2HitboxModern {

    private V2HitboxModern() {
    }

    static void register(V2Listener listener, ModelEventApplication application) {
        listener.subscribeRaw(application, HitBoxDamagedEvent.class, event ->
                listener.fireDamaged(event.getHitBox(), event.getDamage()));
        listener.subscribeRaw(application, HitBoxInteractEvent.class, event ->
                listener.fireInteracted(event.getHitBox(), event.getWho(), event.getHand().name()));
    }
}
