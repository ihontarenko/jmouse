package org.jmouse.core.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A text in Latin letters keeps its case, its punctuation and its spacing; its slug folds all three. */
class SlugifierLatinTest {

    private final Slugifier slugifier = Slugifier.standard();

    @Test
    @DisplayName("a capital stays a capital, a digraph opening with one")
    void caseIsKept() {
        assertEquals("Volodar Persniv", slugifier.latin("Володар Перснів"));
        assertEquals("Zhuk", slugifier.latin("Жук"));
        assertEquals("Lodz", slugifier.latin("Łódź"));
        assertEquals("GND@1", slugifier.latin("GND@1"));
    }

    @Test
    @DisplayName("the slug of a name is the same in either case")
    void slugsFoldCase() {
        assertEquals("volodar-persniv", slugifier.slug("Володар Перснів"));
        assertEquals(slugifier.slug("жук"), slugifier.slug("ЖУК"));
    }
}
