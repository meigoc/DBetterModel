/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v3;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;

import java.lang.reflect.Method;

/**
 * Resolves BetterModel managers across the 3.x line, whose accessor was refactored in 3.3.0.
 *
 * <p>The v3 layer compiles against 3.3.0 but the single jar loads on every BM 3.x server
 * (and best-effort 4.x), where two mutually incompatible shapes exist: on 3.2.0 and below only
 * the named getters ({@code modelManager()}, {@code scriptManager()}, {@code skinManager()})
 * exist and {@code manager(Class)} does not; on 3.3.0+ {@link BetterModelPlatform#manager(Class)}
 * is the real accessor and the named getters are {@code @Deprecated} delegates slated for removal
 * in a future major. Neither call can therefore be made statically, so this helper resolves the
 * accessor reflectively — mirroring {@code V2HitboxLegacy}'s bridge across a BM API break.
 */
final class V3Managers {

    private static volatile Method unifiedAccessor;
    private static volatile boolean unifiedResolved;

    private V3Managers() {
    }

    static <T> T get(Class<T> managerClass, String legacyGetter) {
        BetterModelPlatform platform = BetterModel.platform();
        try {
            Method unified = unifiedAccessor();
            if (unified != null) {
                return managerClass.cast(unified.invoke(platform, managerClass));
            }
            return managerClass.cast(BetterModelPlatform.class.getMethod(legacyGetter).invoke(platform));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("BetterModel manager lookup failed for " + managerClass.getName(), e);
        }
    }

    private static Method unifiedAccessor() {
        if (!unifiedResolved) {
            synchronized (V3Managers.class) {
                if (!unifiedResolved) {
                    try {
                        unifiedAccessor = BetterModelPlatform.class.getMethod("manager", Class.class);
                    } catch (NoSuchMethodException e) {
                        unifiedAccessor = null;
                    }
                    unifiedResolved = true;
                }
            }
        }
        return unifiedAccessor;
    }
}
