package org.jmouse.cad.kicad;

import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.sexpression.SExpressionParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A KiCad footprint's reference and value become its fields, where KiCad prints them and on what they are printed on. */
class KiCadFootprintFieldsTest {

    @Test
    void theReferenceAndValueStandWhereKiCadPrintsThemAndAHiddenOneIsNotPrinted() {
        KiCadFootprint footprint = KiCadFootprint.of(new SExpressionParser().parse("R_0805.kicad_mod", """
                (footprint "R_0805" (layer "F.Cu")
                  (property "Reference" "REF**" (at 0 -1.65 0) (layer "F.SilkS") (effects (font (size 1 1))))
                  (property "Value" "R_0805" (at 0 1.65 90) (layer "F.Fab") (effects (font (size 0.8 0.8))))
                  (fp_text value "R_0805" (at 0 3 0) (layer "F.Fab") hide (effects (font (size 1 1)))))
                """));
        assertEquals(List.of(
                new FootprintLines.Field(PcbVocabulary.FIELD_REFERENCE, 0, -1.65, 1, 0, "silkscreen"),
                new FootprintLines.Field(PcbVocabulary.FIELD_VALUE, 0, 1.65, 0.8, 90, "fabrication")),
                KiCadFootprintDrawing.fieldsOf(footprint, KiCadLibraries.standard()));
    }

    @Test
    void aJustifiedFieldKeepsItsPointAlongTheLettersAndStandsOnItsMiddleAcrossThem() {
        KiCadFootprint footprint = KiCadFootprint.of(new SExpressionParser().parse("R_0805.kicad_mod", """
                (footprint "R_0805" (layer "F.Cu")
                  (property "Reference" "REF**" (at -1 -2 0) (layer "F.SilkS") (effects (font (size 1 1)) (justify left bottom))))
                """));
        // Standing on its bottom at y = -2, Y downwards: its middle half its height above, at -2.5; its letters begin at x = -1.
        assertEquals(List.of(new FootprintLines.Field(PcbVocabulary.FIELD_REFERENCE, -1, -2.5, 1, 0, "silkscreen", PcbVocabulary.TEXT_START)),
                KiCadFootprintDrawing.fieldsOf(footprint, KiCadLibraries.standard()));
    }
}
