/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

/**
 * Version-neutral reload outcome (5.x tech-debt item 8: enum instead of
 * {@code getClass().getSimpleName()} string matching).
 */
public enum BmReloadResult {
    SUCCESS("Success"),
    FAILURE("Failure"),
    ON_RELOAD("OnReload"),
    UNKNOWN("Unknown");

    private final String legacyName;

    BmReloadResult(String legacyName) {
        this.legacyName = legacyName;
    }

    /** The exact string the 5.x {@code bm finishes reload} event exposed as {@code <context.result>}. */
    public String legacyName() {
        return legacyName;
    }
}
