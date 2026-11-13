package org.jmouse.cad.simulation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a reader means by a document filed "in" a category: it and every category under it. */
class CategoryDocumentTest {

    private static final List<CategoryDocument> CATEGORIES = List.of(
            new CategoryDocument("circuits", "Circuits", null, null, 0),
            new CategoryDocument("rc-circuits", "RC circuits", null, "circuits", 1),
            new CategoryDocument("rc-filters", "RC filters", null, "rc-circuits", 0),
            new CategoryDocument("power", "Power", null, null, 1));

    @Test
    void aCategoryHoldsEveryCategoryUnderItHoweverDeep() {
        assertEquals(Set.of("circuits", "rc-circuits", "rc-filters"), CategoryDocument.within("circuits", CATEGORIES));
    }

    @Test
    void aLeafHoldsItselfAlone() {
        assertEquals(Set.of("rc-filters"), CategoryDocument.within("rc-filters", CATEGORIES));
    }

    @Test
    void anUnknownCategoryHoldsItselfAlone() {
        assertEquals(Set.of("lessons"), CategoryDocument.within("lessons", CATEGORIES));
    }
}
