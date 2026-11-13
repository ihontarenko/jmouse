package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.simulation.ProjectDocument;
import org.jmouse.cad.simulation.ProjectPurpose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A project as its document says it, written by the language and read back the same. */
class ProjectDocumentTest {

    private final DocumentLanguage language = CadLanguage.shipped().language();

    @Test
    void aDesignWritesNeitherItsPurposeNorItsStatus() {
        ProjectDocument design = new ProjectDocument("amplifier", "Amplifier", "A class-AB stage", ProjectPurpose.DESIGN, DocumentStatus.PUBLISHED);
        String written = language.write(design.document());
        assertFalse(written.contains("purpose"), written);
        assertFalse(written.contains("status"), written);
        assertEquals(design, read(written));
    }

    @Test
    void draftsAndAnArchivedProjectSayItAndReadBack() {
        ProjectDocument drafts = new ProjectDocument("drafts", "Drafts", null, ProjectPurpose.DRAFTS, DocumentStatus.PUBLISHED).archived(true);
        ProjectDocument read = read(language.write(drafts.document()));
        assertEquals(drafts, read);
        assertTrue(read.archived());
        assertEquals(ProjectPurpose.DRAFTS, read.purpose());
    }

    @Test
    void aWordNamingNoPurposeIsRefused() {
        String text = "project:amplifier(\"Amplifier\") {\n    meta {\n        purpose: lessons\n    }\n}\n";
        assertThrows(IllegalArgumentException.class, () -> read(text));
    }

    private ProjectDocument read(String text) {
        return ProjectDocument.of(language.readOne(text, "project"));
    }
}
