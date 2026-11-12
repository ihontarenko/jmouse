package org.jmouse.kicad.library;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.kicad.footprint.KiCadPadShape;
import org.jmouse.kicad.footprint.KiCadPadType;
import org.jmouse.sexpression.SExpressionParser;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionString;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A project's own library, written as KiCad keeps one, is the library read: its footprints and symbols as they went in. */
class KiCadProjectLibrariesTest {

    private static final KiCadFootprint TRIMMER = new KiCadFootprint("trimmer-footprint", "A trimmer", List.of("trimmer"),
            Map.of("Author", "jMouse", "License", "CERN-OHL-W-2.0"), List.of(
            new KiCadPad("1", KiCadPadType.THROUGH_HOLE, KiCadPadShape.RECTANGLE, new KiCadPoint(-2.54, 0), 0, 1.6, 1.6,
                    Optional.of(new double[] {0.8, 0.8}), 0, List.of("*.Cu", "*.Mask"), Optional.empty(), Optional.empty(), List.of()),
            new KiCadPad("2", KiCadPadType.THROUGH_HOLE, KiCadPadShape.CIRCLE, new KiCadPoint(0, 0), 0, 1.6, 1.6,
                    Optional.of(new double[] {0.8, 0.8}), 0, List.of("*.Cu", "*.Mask"), Optional.empty(), Optional.empty(), List.of())),
            List.of(new KiCadFootprintGraphic.Arc(new KiCadPoint(-2, 0), new KiCadPoint(0, 2), new KiCadPoint(2, 0), "F.SilkS", 0.12, false)),
            List.of(), List.of());

    @Test
    void aFootprintIsWrittenAsItsLibraryKeepsItAndReadBack() {
        String written = KiCadProjectLibraries.footprint(TRIMMER, "F.SilkS", "F.Fab");
        KiCadFootprint read = KiCadFootprint.of(new SExpressionParser().parse("trimmer-footprint.kicad_mod", written));
        assertEquals(TRIMMER.name(), read.name());
        assertEquals(TRIMMER.description(), read.description());
        assertEquals(TRIMMER.pads().stream().map(KiCadPad::number).toList(), read.pads().stream().map(KiCadPad::number).toList());
        assertEquals(TRIMMER.pads().get(0).at(), read.pads().get(0).at());
        assertEquals(1, read.graphics().size());
        assertEquals("CERN-OHL-W-2.0", read.properties().get("License"));
        assertEquals("jMouse", read.properties().get("Author"));
        assertEquals(Optional.of("through_hole"), read.mounting());
        assertTrue(written.contains("(attr through_hole)"), written);
    }

    @Test
    void aSymbolIsNamedInItsLibraryByWhatFollowsItsNickname() {
        SExpressionList copy = SExpressionList.of("symbol", SExpressionString.of("jmCAD:1n4007"),
                SExpressionList.of("in_bom", SExpressionAtom.of("yes")), SExpressionList.of("symbol", SExpressionString.of("1n4007_0_1")));
        Map<String, SExpressionList> symbols = new LinkedHashMap<>();
        symbols.put("jmCAD:1n4007", copy);
        SExpressionList library = new SExpressionParser().parse("jmCAD.kicad_sym", KiCadProjectLibraries.symbols("jmCAD", symbols)).root();
        assertEquals("kicad_symbol_lib", library.name());
        assertEquals("1n4007", library.lists("symbol").getFirst().word(1));
    }

    @Test
    void theTablesFindTheLibrariesInTheProjectsOwnFolder() {
        String footprints = KiCadProjectLibraries.footprintTable("jmCAD", "Footprints of this design");
        assertTrue(footprints.contains("(uri \"${KIPRJMOD}/jmCAD.pretty\")"), footprints);
        assertTrue(footprints.startsWith("(fp_lib_table"), footprints);
        String symbols = KiCadProjectLibraries.symbolTable("jmCAD", "Symbols of this design");
        assertTrue(symbols.contains("(uri \"${KIPRJMOD}/jmCAD.kicad_sym\")"), symbols);
        assertEquals("jmCAD.pretty/trimmer-footprint.kicad_mod", KiCadProjectLibraries.footprintFile("jmCAD", "trimmer-footprint"));
    }
}
