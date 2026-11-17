package org.jmouse.core.collections;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongMapsTest {

    @Test
    void doubleMapAgreesWithABoxedMapThroughManyGrowths() {
        LongDoubleMap map = new LongDoubleMap(2);
        Map<Long, Double> expected = new HashMap<>();
        Random random = new Random(7);
        for (int round = 0; round < 50_000; round++) {
            // Keys of a grid, packed column and row, and negative ones too: neighbours must not crowd one run.
            long key = (random.nextInt(400) - 200) * 65536L + random.nextInt(400) - 200;
            double value = random.nextDouble();
            map.put(key, value);
            expected.put(key, value);
        }
        assertEquals(expected.size(), map.size());
        expected.forEach((key, value) -> assertEquals(value, map.get(key, Double.NaN)));
        assertTrue(Double.isNaN(map.get(Long.MIN_VALUE, Double.NaN)));
        assertFalse(map.containsKey(Long.MIN_VALUE));
    }

    @Test
    void doubleMapWorksAValueOutOnce() {
        LongDoubleMap map = new LongDoubleMap();
        int[] asked = {0};
        for (int round = 0; round < 3; round++) {
            for (long key = 0; key < 100; key++) {
                double value = map.computeIfAbsent(key, worked -> {
                    asked[0]++;
                    return worked * 0.5;
                });
                assertEquals(key * 0.5, value);
            }
        }
        assertEquals(100, asked[0]);
        assertEquals(100, map.size());
    }

    @Test
    void objectMapKeepsTheCellsOfAGridDiagonalsIncluded() {
        LongObjectMap<String> map = new LongObjectMap<>(2);
        for (long column = -60; column < 60; column++) {
            for (long row = -60; row < 60; row++) {
                assertEquals("cell", map.computeIfAbsent((column + 32768) * 65536 + row + 32768, key -> "cell"));
                map.put((column + 32768) * 65536 + row + 32768, column + ":" + row);
            }
        }
        assertEquals(120 * 120, map.size());
        assertEquals("-60:59", map.get((-60 + 32768) * 65536L + 59 + 32768));
        assertEquals("none", map.getOrDefault(-1, "none"));
        assertThrows(IllegalArgumentException.class, () -> map.put(1, null));
    }

    @Test
    void booleanMapKeepsBothAnswersThroughGrowth() {
        LongBooleanMap map = new LongBooleanMap(2);
        for (long key = -5_000; key < 5_000; key++) {
            map.put(key, key % 3 == 0);
        }
        map.put(9, false);
        assertEquals(10_000, map.size());
        for (long key = -5_000; key < 5_000; key++) {
            assertEquals(key != 9 && key % 3 == 0, map.get(key, !(key % 3 == 0)));
        }
        assertTrue(map.get(123_456, true));
        assertFalse(map.get(123_456, false));
    }

    @Test
    void booleanMapWorksAnAnswerOutOnce() {
        LongBooleanMap map = new LongBooleanMap();
        int[] asked = {0};
        for (int round = 0; round < 3; round++) {
            for (long key = 0; key < 200; key++) {
                assertEquals(key % 2 == 0, map.computeIfAbsent(key, worked -> {
                    asked[0]++;
                    return worked % 2 == 0;
                }));
            }
        }
        assertEquals(200, asked[0]);
    }
}
