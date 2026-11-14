package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.Provenance;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The terms LibrePCB's own installed libraries stand under come from the table, by the library's UUID; a project's claim none. */
class LibrePcbFilingTermsTest {

    @Test
    void theShippedTableNamesTheTermsOfLibrePcbsOwnLibrariesByTheirIdentifier() {
        LibrePcbFiling filing = LibrePcbFiling.shipped();

        assertEquals(Optional.of("CC0-1.0"), filing.licenceOf("a9ddf0c6-9b1c-4730-b300-01b4f192ad40"));
        assertEquals(Optional.empty(), filing.licenceOf("a community library"));
        assertEquals(new Provenance("LibrePCB package X", "U. Bruhin", null), filing.provenance("LibrePCB package X", "U. Bruhin"));
        assertEquals(new Provenance("LibrePCB package X", "U. Bruhin", "CC0-1.0"),
                filing.claiming("CC0-1.0").provenance("LibrePCB package X", "U. Bruhin"));
    }
}
