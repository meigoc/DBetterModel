/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.api;

/**
 * Snapshot of an animation currently playing on a tracker.
 *
 * @param name        animation name
 * @param rawTypeName the BetterModel iterator type name as-is ({@code PLAY_ONCE}, {@code LOOP},
 *                    {@code HOLD_ON_LAST}) — stable across all supported lines, exposed to
 *                    scripts lower-cased for 5.x compatibility
 */
public record BmRunningAnimation(String name, String rawTypeName) {
}
