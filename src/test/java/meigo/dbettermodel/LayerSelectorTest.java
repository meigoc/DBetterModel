/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerSelectorTest {

    private static final String V1 = "meigo.dbettermodel.compat.v1.V1Platform";
    private static final String V2 = "meigo.dbettermodel.compat.v2.V2Platform";
    private static final String V3 = "meigo.dbettermodel.compat.v3.V3Platform";

    @Test
    void oneFifteenSelectsV1() {
        assertEquals(V1, LayerSelector.select("1.15.0").layerClass());
        assertEquals(V1, LayerSelector.select("1.15.2").layerClass());
        assertFalse(LayerSelector.select("1.15.2").bestEffort());
    }

    @Test
    void oneFourteenIsTooOld() {
        LayerSelector.Selection s = LayerSelector.select("1.14.4");
        assertNull(s.layerClass());
        assertTrue(s.reason().contains("too old"));
    }

    @Test
    void twoSelectsV2() {
        assertEquals(V2, LayerSelector.select("2.0.1").layerClass());
        assertEquals(V2, LayerSelector.select("2.2.0").layerClass());
    }

    @Test
    void snapshotSuffixIsStripped() {
        assertEquals(V2, LayerSelector.select("2.2.0-SNAPSHOT-123").layerClass());
    }

    @Test
    void threeSelectsV3() {
        assertEquals(V3, LayerSelector.select("3.0.0").layerClass());
        assertEquals(V3, LayerSelector.select("3.2.1").layerClass());
        assertEquals(V3, LayerSelector.select("3.3.0").layerClass());
        assertFalse(LayerSelector.select("3.3.0").bestEffort());
    }

    @Test
    void futureMajorIsBestEffortV3() {
        LayerSelector.Selection s = LayerSelector.select("4.0.0");
        assertEquals(V3, s.layerClass());
        assertTrue(s.bestEffort());
    }

    @Test
    void garbageAndEmptySelectNone() {
        assertNull(LayerSelector.select("garbage").layerClass());
        assertNull(LayerSelector.select("").layerClass());
        assertNull(LayerSelector.select(null).layerClass());
        assertNull(LayerSelector.select("1").layerClass());
    }
}
