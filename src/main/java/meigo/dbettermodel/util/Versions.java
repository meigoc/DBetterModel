/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.util;

/** Release-version comparison for the update checker. Pure, unit-testable. */
public final class Versions {

    private Versions() {
    }

    /**
     * Whether the release tag is strictly newer than the current version.
     * Leading 'v' and any '-suffix' (e.g. -SNAPSHOT) are ignored, so
     * 'v6.0.0' vs '6.0.0-SNAPSHOT' is not an update. Unparseable input -> false.
     */
    public static boolean isNewer(String tag, String current) {
        int[] a = parse(tag);
        int[] b = parse(current);
        if (a == null || b == null) return false;
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int[] parse(String version) {
        if (version == null || version.isEmpty()) return null;
        String clean = version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
        String[] parts = clean.split("-")[0].split("\\.");
        int[] numbers = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                numbers[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return numbers;
    }
}
