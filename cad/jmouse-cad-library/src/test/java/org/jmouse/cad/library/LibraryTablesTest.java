package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.table.TableModule;
import org.jmouse.sheet.Sheet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The library's tables are documents of the engine's whole language: each reads, among every other kind, to its sheet. */
class LibraryTablesTest {

    @Test
    void everyTableReadsWithTheEnginesLanguage() {
        CadLibrary library = CadLibrary.library();
        List<String> tables = library.slugs().stream()
                .filter(slug -> library.path(slug).orElseThrow().endsWith(TableModule.TABLE.fileSuffix())).toList();
        assertFalse(tables.isEmpty(), "the library ships its tables");
        List<String> refused = new ArrayList<>();
        for (String slug : tables) {
            String path = library.path(slug).orElseThrow();
            try {
                DocumentNode table = CadLanguage.standard().language().readOne(library.text(path), path);
                Sheet sheet = TableModule.sheetOf(table);
                assertEquals(slug, sheet.slug());
            } catch (RuntimeException refusal) {
                refused.add(path + ": " + refusal.getMessage());
            }
        }
        assertTrue(refused.isEmpty(), String.join("\n", refused));
    }

    @Test
    void theStandardsReadAsTheyShip() {
        assertFalse(CadStandards.shipped().pcbRules().valuesOf(java.util.Optional.empty()).isEmpty());
        assertFalse(CadStandards.shipped().landPatternRules().filing().authors().isBlank());
    }
}
