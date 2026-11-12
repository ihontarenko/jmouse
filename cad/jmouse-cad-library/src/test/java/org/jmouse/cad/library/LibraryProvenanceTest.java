package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.licence.Licences;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every document the library ships says whose it is and under what licence, in its own file ({@link ProvenanceAudit}). */
class LibraryProvenanceTest {

    @Test
    void everyDocumentNamesItsHolderAndAKnownLicence() {
        CadLibrary library = CadLibrary.library();
        List<DocumentNode> documents = library.slugs().stream().map(slug -> library.find(slug).orElseThrow()).toList();
        assertEquals(List.of(), ProvenanceAudit.faults(documents, Licences.standard()));
    }

    @Test
    void aDocumentSayingNothingIsNotTakenForOurs() {
        DocumentNode silent = CadLanguage.shipped().language().readOne("category:silent(\"Silent\") {\n    meta {\n        order: 0\n    }\n}\n",
                "silent.category.jm");
        assertEquals(List.of("silent names no holder", "silent names no licence"), ProvenanceAudit.faults(List.of(silent), Licences.standard()));
    }

    @Test
    void aLicenceWhoseTermsAreUnknownIsNamedAsSuch() {
        DocumentNode document = CadLanguage.shipped().language().readOne("category:odd(\"Odd\") {\n    meta {\n        order: 0\n    }\n}\n",
                "odd.category.jm");
        new Provenance(null, "Somebody", "Made-Up-Licence-1.0").writeInto(document.block(MetaNode.class).orElseThrow());
        assertEquals(List.of("odd names the licence 'Made-Up-Licence-1.0', whose terms the program does not know"),
                ProvenanceAudit.faults(List.of(document), Licences.standard()));
    }
}
