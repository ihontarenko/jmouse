package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The library's documents name each other by code name, and every code is one document's ({@link ReferenceAudit}). */
class LibraryReferencesTest {

    @Test
    void everyReferenceIsACodeNameOfAHeldDocumentAndNoCodeIsTwoDocuments() {
        CadLibrary library = CadLibrary.library();
        List<DocumentNode> documents = library.slugs().stream().map(slug -> library.find(slug).orElseThrow()).toList();
        assertEquals(List.of(), ReferenceAudit.faults(documents, library::find));
    }

    @Test
    void aSlugWhereACodeIsMeantIsSaid() {
        CadLibrary library = CadLibrary.library();
        DocumentNode component = CadLanguage.shipped().language().readOne("""
                component:probe("Probe", type: resistor) {
                    meta {
                        summary: "A probe"
                    }
                    footprint(chip-resistor-0805-footprint)
                }
                """, "probe.component.jm");
        assertEquals(List.of("probe names chip-resistor-0805-footprint by its slug; its code is FP.CHIP:R0805"),
                ReferenceAudit.faults(List.of(component), word -> library.find(word).or(Optional::empty)));
    }
}
