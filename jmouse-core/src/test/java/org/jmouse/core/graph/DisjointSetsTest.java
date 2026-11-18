package org.jmouse.core.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisjointSetsTest {

    @Test
    void joinsSetsAndTellsWhatIsTogether() {
        DisjointSets sets = new DisjointSets(5);
        sets.join(0, 1);
        sets.join(3, 4);
        assertTrue(sets.together(0, 1));
        assertFalse(sets.together(1, 3));
        sets.join(1, 4);
        assertTrue(sets.together(0, 3));
        assertFalse(sets.together(2, 0));
    }

    @Test
    void growsAsElementsAreAdded() {
        DisjointSets sets = new DisjointSets();
        for (int element = 0; element < 1000; element++) {
            assertEquals(element, sets.add());
            if (element > 0) {
                sets.join(element, element - 1);
            }
        }
        assertEquals(1000, sets.size());
        assertTrue(sets.together(0, 999));
    }
}
