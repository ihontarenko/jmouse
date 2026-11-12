package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.licence.LicenceUse;
import org.jmouse.licence.Licences;
import org.jmouse.licence.Verdict;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a design passed on owes the holders of what it is built from, and that our own library owes nobody. */
class LicenceLedgerTest {

    private static final CadLibrary LIBRARY  = CadLibrary.library();
    private static final Licences   LICENCES = Licences.standard();

    private static final String KICAD_FOOTPRINT = """
            footprint:kicad-resistor-footprint("R_0805") {
                meta {
                    source: "KiCad footprint Resistor_SMD:R_0805_2012Metric"
                    authors: "KiCad Libraries Team"
                    license: "CC-BY-SA-4.0 WITH KiCad-libraries-exception"
                }
                pad:1(surface, rectangle, at: -1 0, size: 1 x 1.2)
                pad:2(surface, rectangle, at: 1 0, size: 1 x 1.2)
                model("vendor-resistor-model")
            }
            """;

    private static final String VENDOR_MODEL = """
            model:vendor-resistor-model("R_0805 body") {
                meta {
                    source: "A vendor's 3D library"
                    authors: "A Vendor"
                    license: "CC-BY-4.0"
                }
                box(housing, size: [2, 1.25, 0.5], at: [0, 0, 0.25])
            }
            """;

    private static final String UNSTATED_FOOTPRINT = """
            footprint:unstated-footprint("Somebody's") {
                meta {
                    authors: "Somebody"
                }
                pad:1(surface, rectangle, at: 0 0, size: 1 x 1)
            }
            """;

    @Test
    void ourOwnLibraryOwesNobodyAndHoldsNothingItCouldNotMakeItsOwn() {
        List<DocumentNode> shipped = LIBRARY.slugs().stream().map(slug -> LIBRARY.find(slug).orElseThrow()).toList();
        LicenceLedger ledger = LicenceLedger.adopted(shipped, LICENCES, LIBRARY.holder());
        assertEquals(List.of(), ledger.refusals(), "a shipped document is somebody else's work under terms that keep it theirs");
    }

    @Test
    void aComponentIsBuiltFromItsFootprintsAndTheirModels() {
        List<String> built = BuiltFrom.of(LIBRARY.find("1n4007").orElseThrow(), LIBRARY).stream().map(DocumentNode::slug).toList();
        assertTrue(built.contains("axial-diode-do-41-pitch-1016-horizontal-footprint"), built.toString());
        assertTrue(built.stream().anyMatch(slug -> slug.endsWith("-model")), built.toString());
    }

    @Test
    void aKiCadFootprintIsFreeInADesignAndItsVendorModelOwesCredit() {
        CatalogueDocuments catalogue = catalogueOf(read(KICAD_FOOTPRINT), read(VENDOR_MODEL));
        LicenceLedger ledger = LicenceLedger.passedOn(read(KICAD_FOOTPRINT), catalogue, LICENCES, LIBRARY.holder());

        // The footprint itself passed on keeps its licence; the model built into it owes its credit.
        assertEquals(List.of(LicenceUse.REDISTRIBUTED, LicenceUse.EMBEDDED), ledger.entries().stream().map(entry -> entry.ruling().use()).toList());
        assertEquals(Verdict.SAME_TERMS, ledger.verdict());
        String notice = ledger.notice(LICENCES);
        assertTrue(notice.contains("Held by: A Vendor"), notice);
        assertTrue(notice.contains("https://spdx.org/licenses/CC-BY-4.0.html"), notice);
    }

    @Test
    void aWorkOfUnstatedTermsTravelsInsideADesignButNeverAsItself() {
        DocumentNode footprint = read(UNSTATED_FOOTPRINT);
        LicenceLedger alone = LicenceLedger.passedOn(footprint, catalogueOf(), LICENCES, LIBRARY.holder());
        assertEquals(1, alone.refused().size());
        assertTrue(alone.refusals().getFirst().contains("No licence is stated"), alone.refusals().toString());
    }

    @Test
    void whatOurLibraryHoldsIsLeftOutOfADesignsLedger() {
        LicenceLedger ledger = LicenceLedger.passedOn(LIBRARY.find("1n4007").orElseThrow(), LIBRARY, LICENCES, LIBRARY.holder());
        assertEquals(List.of(), ledger.entries());
        assertEquals(Verdict.FREE, ledger.verdict());
    }

    private static DocumentNode read(String text) {
        return CadLanguage.shipped().language().readOne(DocumentInput.text("test", text));
    }

    private static CatalogueDocuments catalogueOf(DocumentNode... documents) {
        Map<String, DocumentNode> bySlug = Arrays.stream(documents).collect(Collectors.toMap(DocumentNode::slug, Function.identity()));
        return slug -> Optional.ofNullable(bySlug.get(slug));
    }
}
