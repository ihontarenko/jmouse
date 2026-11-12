package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CategoryDocument;
import org.jmouse.cad.simulation.SimulationModule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every document of the library says where it belongs, in its own file: its {@code meta category} names a category the
 * library ships, and the categories make one tree — each under a parent that is one, never under itself.
 */
class LibraryCategoriesTest {

    @Test
    void everyDocumentIsFiledInACategoryTheLibraryHolds() {
        CadLibrary library = CadLibrary.library();
        Map<String, CategoryDocument> categories = new LinkedHashMap<>();
        Map<String, String> filed = new LinkedHashMap<>();
        for (String slug : library.slugs()) {
            DocumentNode document = library.find(slug).orElseThrow();
            if (document.kind().equals(SimulationModule.CATEGORY)) {
                categories.put(slug, CategoryDocument.of(document));
            } else {
                filed.put(slug, DocumentIndex.of(document).category());
            }
        }
        List<String> wrong = new ArrayList<>();
        filed.forEach((slug, category) -> {
            if (category == null) {
                wrong.add(slug + " says no category");
            } else if (!categories.containsKey(category)) {
                wrong.add(slug + " is filed in '" + category + "', which is no category of the library");
            }
        });
        assertEquals(List.of(), wrong);
        assertEquals(categories.size(), CategoryDocument.parentsFirst(categories.values()).size());
    }

    @Test
    void aCategoryIsWrittenAndReadBackAsItsOwnDocument() {
        CategoryDocument written = new CategoryDocument("resistor-networks", "Resistor networks", "Several resistors in one package",
                "resistors", 3);
        assertEquals(written, CategoryDocument.of(written.document()));
    }

    @Test
    void aTreeIsReadParentsFirstAndARingIsRefused() {
        CategoryDocument top = new CategoryDocument("top", "Top", null, null, 0);
        CategoryDocument second = new CategoryDocument("second", "Second", null, "top", 1);
        CategoryDocument first = new CategoryDocument("first", "First", null, "top", 0);
        assertEquals(List.of(top, first, second), CategoryDocument.parentsFirst(List.of(second, first, top)));
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> CategoryDocument.parentsFirst(List.of(
                new CategoryDocument("one", "One", null, "two", 0), new CategoryDocument("two", "Two", null, "one", 0))));
        assertTrue(refused.getMessage().contains("stands under itself"), refused.getMessage());
    }
}
