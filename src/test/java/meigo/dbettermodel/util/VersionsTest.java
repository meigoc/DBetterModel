/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionsTest {

    @Test
    void newerReleaseIsAnUpdate() {
        assertTrue(Versions.isNewer("v6.1.0", "6.0.0-SNAPSHOT"));
        assertTrue(Versions.isNewer("6.0.1", "6.0.0"));
        assertTrue(Versions.isNewer("7.0.0", "6.9.9"));
    }

    @Test
    void snapshotOfSameVersionIsNotAnUpdate() {
        assertFalse(Versions.isNewer("v6.0.0", "6.0.0-SNAPSHOT"));
        assertFalse(Versions.isNewer("6.0.0", "6.0.0"));
    }

    @Test
    void olderReleaseIsNotAnUpdate() {
        assertFalse(Versions.isNewer("5.9.0", "6.0.0"));
        assertFalse(Versions.isNewer("v6.0.0", "6.1.0"));
    }

    @Test
    void leadingVIsIgnored() {
        assertTrue(Versions.isNewer("V6.0.1", "6.0.0"));
    }

    @Test
    void differentComponentCountsCompare() {
        assertTrue(Versions.isNewer("6.0.0.1", "6.0.0"));
        assertFalse(Versions.isNewer("6.0", "6.0.0"));
    }

    @Test
    void unparseableInputIsNotAnUpdate() {
        assertFalse(Versions.isNewer("garbage", "6.0.0"));
        assertFalse(Versions.isNewer(null, "6.0.0"));
        assertFalse(Versions.isNewer("6.0.0", ""));
    }
}
