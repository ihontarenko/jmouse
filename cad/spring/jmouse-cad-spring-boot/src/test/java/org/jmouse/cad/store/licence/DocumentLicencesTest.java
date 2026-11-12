package org.jmouse.cad.store.licence;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.library.LicenceLedger;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.licence.Licences;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A file leaves only as the licences of what it carries let it, their credit beside it. */
class DocumentLicencesTest {

    private static final DocumentLicences LICENCES = new DocumentLicences(Licences.standard(), CadLibrary.library().holder());

    private static final ExportedFiles STEP = ExportedFiles.single("amplifier.step", "ISO-10303-21;", List.of());

    @Test
    void aBorrowedWorkLeavesWithItsCreditAndWhatOwesNothingAsItWasWritten() {
        LicenceLedger ledger = LICENCES.passedOn(read("""
                footprint:kicad-footprint("KiCad's") {
                    meta {
                        authors: "KiCad Libraries Team"
                        license: "CC-BY-SA-4.0 WITH KiCad-libraries-exception"
                    }
                    pad:1(surface, rectangle, at: 0 0, size: 1 x 1)
                }
                """), CatalogueDocuments.empty());
        // The footprint itself is passed on: it keeps its licence and its credit travels with it.
        ExportedFiles credited = LICENCES.credited(STEP, ledger);
        assertEquals(List.of("amplifier.step", "THIRD-PARTY-NOTICES.txt"), credited.files().stream().map(ExportedFile::fileName).toList());
        assertEquals("amplifier", credited.name());
        assertTrue(credited.files().get(1).text().contains("Held by: KiCad Libraries Team"));

        assertSame(STEP, LICENCES.credited(STEP, new LicenceLedger(List.of())));
    }

    @Test
    void aWorkThatMayNotTravelStopsTheExportNamingIt() {
        LicenceLedger ledger = LICENCES.passedOn(read("""
                footprint:vendor-footprint("Vendor's") {
                    meta {
                        authors: "A Vendor"
                        license: "LicenseRef-vendor-proprietary"
                    }
                    pad:1(surface, rectangle, at: 0 0, size: 1 x 1)
                }
                """), CatalogueDocuments.empty());
        CadProblem refused = assertThrows(CadProblem.class, () -> LICENCES.credited(STEP, ledger));
        assertTrue(refused.getMessage().contains("vendor-footprint"), refused.getMessage());
        assertEquals(Optional.of("refused"), LICENCES.worksOf(ledger).stream().map(DocumentLicences.BorrowedWork::verdict).findFirst());
    }

    private static DocumentNode read(String text) {
        return CadLanguage.shipped().language().readOne(DocumentInput.text("test", text));
    }
}
