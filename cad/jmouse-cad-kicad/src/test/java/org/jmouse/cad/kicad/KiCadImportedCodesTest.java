package org.jmouse.cad.kicad;

import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.schematic.SchematicModule;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A document KiCad's import brought before imports named by codes is given the code the import gives it now. */
class KiCadImportedCodesTest {

    private final KiCadLibraries libraries = KiCadLibraries.shipped();

    private Optional<String> codeOf(org.jmouse.cad.document.DocumentKind kind, String slug, String source) {
        return libraries.codeOfImported(kind, slug, new Provenance(source, null, null));
    }

    @Test
    void aFootprintAModelAndASymbolAreCodedAsTheImportCodesThem() {
        assertEquals(Optional.of(KiCadFootprintDrawing.codeOf("1289-ST:1289ST")),
                codeOf(PcbModule.FOOTPRINT, "kicad-1289-st-1289st-footprint", KiCadSources.footprint("1289-ST:1289ST", false)));
        assertEquals(Optional.of(KiCadFootprintDrawing.modelCodeOf("Package_SO.3dshapes/SOIC-8_3.9x4.9mm_P1.27mm")),
                codeOf(ModelModule.MODEL, "kicad-package-so-soic-8-model", KiCadSources.model("Package_SO.3dshapes/SOIC-8_3.9x4.9mm_P1.27mm")));
        assertEquals(Optional.of("SY.DEVICE:BUZZER"), codeOf(SchematicModule.SYMBOL, "kicad-device-buzzer-symbol", KiCadSources.symbol("Device:Buzzer")));
    }

    @Test
    void aUnitOfSeveralKeepsItsLetterAndAnotherProgramsDocumentIsNotKiCads() {
        assertEquals(Optional.of("SY.AMPLIFIER-OPERATIONAL:LM358-UNIT-B"), codeOf(SchematicModule.SYMBOL,
                "kicad-amplifier-operational-lm358-unit-b-symbol", KiCadSources.symbol("Amplifier_Operational:LM358")));
        assertEquals(Optional.empty(), codeOf(PcbModule.FOOTPRINT, "librepcb-r0805-12345678-footprint", "LibrePCB package R0805 abcdef12"));
    }
}
