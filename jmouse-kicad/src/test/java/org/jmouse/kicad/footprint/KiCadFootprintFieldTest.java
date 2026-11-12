package org.jmouse.kicad.footprint;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProperty;
import org.jmouse.kicad.library.KiCadProjectLibraries;
import org.jmouse.sexpression.SExpressionParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a footprint prints its part's reference and value, as KiCad 8 writes it and as KiCad wrote it before. */
class KiCadFootprintFieldTest {

    private static KiCadFootprint read(String text) {
        return KiCadFootprint.of(new SExpressionParser().parse("R_0805.kicad_mod", text));
    }

    @Test
    void kiCadEightWritesThemAsPropertiesWithAPlace() {
        KiCadFootprint footprint = read("""
                (footprint "R_0805" (version 20240108) (layer "F.Cu")
                  (property "Reference" "REF**" (at 0 -1.65 0) (layer "F.SilkS") (effects (font (size 1 1) (thickness 0.15))))
                  (property "Value" "R_0805" (at 0 1.65 90) (layer "F.Fab") (effects (font (size 0.8 0.8) (thickness 0.12))))
                  (property "Datasheet" "" (at 0 0 0) (layer "F.Fab") (hide yes) (effects (font (size 1 1)))))
                """);
        assertEquals(List.of(
                new KiCadFootprintField(KiCadProperty.REFERENCE, new KiCadPoint(0, -1.65), 0, "F.SilkS", 1, false),
                new KiCadFootprintField(KiCadProperty.VALUE, new KiCadPoint(0, 1.65), 90, "F.Fab", 0.8, false)), footprint.fields());
    }

    @Test
    void beforeKiCadEightTheyAreTextsOfTheirKind() {
        KiCadFootprint footprint = read("""
                (footprint "R_0805" (layer "F.Cu")
                  (fp_text reference "REF**" (at 0 -1.65) (layer "F.SilkS") (effects (font (size 1 1) (thickness 0.15))))
                  (fp_text value "R_0805" (at 0 1.65) (layer "F.Fab") hide (effects (font (size 1 1) (thickness 0.15))))
                  (fp_text user "${REFERENCE}" (at 0 0) (layer "F.Fab") (effects (font (size 0.5 0.5)))))
                """);
        assertEquals(2, footprint.fields().size());
        assertTrue(footprint.fields().get(1).hidden());
        assertEquals(KiCadProperty.VALUE, footprint.fields().get(1).property());
    }

    @Test
    void aLibraryIsWrittenWithTheFieldsWhereTheFootprintPrintsThem() {
        KiCadFootprint footprint = read("""
                (footprint "R_0805" (layer "F.Cu")
                  (property "Reference" "REF**" (at 0.5 -2 0) (layer "F.SilkS") (effects (font (size 0.8 0.8)))))
                """);
        KiCadFootprint again = read(KiCadProjectLibraries.footprint(footprint, "F.SilkS", "F.Fab"));
        assertEquals(new KiCadPoint(0.5, -2), again.fields().getFirst().at());
        assertEquals(0.8, again.fields().getFirst().height());
    }
}
