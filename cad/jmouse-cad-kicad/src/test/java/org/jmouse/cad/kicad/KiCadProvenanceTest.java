package org.jmouse.cad.kicad;

import org.jmouse.cad.document.Provenance;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whose a footprint is travels with it into KiCad and back: written as fields KiCad keeps, read again as whose it was. A
 * footprint of KiCad's own libraries that says nothing is still the KiCad Libraries Team's, and a project's own the project's.
 */
class KiCadProvenanceTest {

    private static final KiCadLibraries LIBRARIES = KiCadLibraries.standard();

    private static KiCadFootprint footprint(Map<String, String> properties) {
        return new KiCadFootprint("TO-220-3_Vertical", "", List.of(), properties, List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void ourFootprintGoesOutWithItsHolderLicenceAndSource() {
        Map<String, String> fields = LIBRARIES.propertiesOf(new Provenance("JEDEC TO-220AB", "jMouse", "CERN-OHL-W-2.0"));
        assertEquals(Map.of("Source", "JEDEC TO-220AB", "Author", "jMouse", "License", "CERN-OHL-W-2.0"), fields);
        assertTrue(LIBRARIES.propertiesOf(Provenance.OURS).isEmpty());
    }

    @Test
    void andComesBackAsWhoseItWas() {
        Map<String, String> fields = LIBRARIES.propertiesOf(new Provenance("JEDEC TO-220AB", "jMouse", "CERN-OHL-W-2.0"));
        Provenance back = KiCadFootprintDrawing.provenanceOf("Project:TO-220-3_Vertical", footprint(fields), false, LIBRARIES);
        assertEquals(new Provenance("JEDEC TO-220AB", "jMouse", "CERN-OHL-W-2.0"), back);
    }

    @Test
    void oneThatSaysNothingIsWhoseItsLibraryIs() {
        Provenance installed = KiCadFootprintDrawing.provenanceOf("Package_TO_SOT_THT:TO-220-3_Vertical", footprint(Map.of()), true, LIBRARIES);
        assertEquals(LIBRARIES.holder(), installed.authors());
        assertEquals(LIBRARIES.license(), installed.license());
        Provenance project = KiCadFootprintDrawing.provenanceOf("Project:Odd", footprint(Map.of("Reference", "U1")), false, LIBRARIES);
        assertEquals(null, project.authors());
    }

    @Test
    void aLicenceAloneIsEnoughToSayWhoseItIs() {
        Provenance back = KiCadFootprintDrawing.provenanceOf("Project:Odd", footprint(Map.of("License", "CC0-1.0")), false, LIBRARIES);
        assertEquals("CC0-1.0", back.license());
        assertEquals(KiCadSources.footprint("Project:Odd", false), back.source());
    }
}
