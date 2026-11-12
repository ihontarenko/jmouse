package org.jmouse.kicad.pcb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.sexpression.SExpressionParser;
import org.junit.jupiter.api.Test;

class KiCadPcbTest {

    private static final String BOARD = """
            (kicad_pcb (version 20240108) (generator "pcbnew")
              (layers (0 "F.Cu" signal) (31 "B.Cu" signal) (44 "Edge.Cuts" user))
              (footprint "Resistor_SMD:R_0805" (layer "F.Cu")
                (at 100 50 90)
                (property "Reference" "R3" (at 0 -1.5 90) (layer "F.SilkS"))
                (path "/sheet/symbol")
                (pad "1" smd rect (at -1 0 120) (size 1 1.2) (layers "F.Cu" "F.Mask") (net 1 "/power/VCC"))
                (pad "2" smd rect (at 1 0 120) (size 1 1.2) (layers "F.Cu" "F.Mask") (net 2 "GND")))
              (footprint "Local:Hole" (layer "B.Cu")
                (at 10 20 30)
                (fp_text reference "H1" (at 0 2 30) (layer "B.SilkS"))
                (fp_line (start 1 2) (end 3 4) (stroke (width 0.1) (type solid)) (layer "B.SilkS"))
                (pad "1" thru_hole circle (at 1 2 50) (size 3 3) (drill 2) (layers "*.Cu" "B.Mask") (net 2 "GND")))
              (gr_line (start 0 0) (end 10 0) (stroke (width 0.1) (type default)) (layer "Edge.Cuts"))
              (gr_text "HELLO" (at 5 6 90) (layer "F.SilkS") (effects (font (size 1.5 1.2))))
              (segment (start 1 2) (end 3 4) (width 0.25) (layer "F.Cu") (net 1))
              (arc (start 1 1) (mid 2 2) (end 3 1) (width 0.2) (layer "B.Cu") (net 2))
              (via (at 7 8) (size 0.8) (drill 0.4) (layers "F.Cu" "B.Cu") (net 2))
              (zone (net 2) (net_name "GND") (layers "F.Cu" "B.Cu") (priority 3)
                (connect_pads (clearance 0.5)) (polygon (pts (xy 0 0) (xy 1 0) (xy 1 1))))
              (zone (net 0) (net_name "") (layer "F.Cu") (keepout (tracks not_allowed))
                (polygon (pts (xy 0 0) (xy 2 0) (xy 2 2)))))
            """;

    private final KiCadPcb board = KiCadPcb.of(new SExpressionParser().parse("board.kicad_pcb", BOARD));

    @Test
    void readsCopperLayersAndCopper() {
        assertEquals(java.util.List.of("F.Cu", "B.Cu"), board.copper());
        assertEquals(1, board.tracks().size());
        assertEquals(1, board.arcs().size());
        assertEquals(java.util.List.of("F.Cu", "B.Cu"), board.vias().getFirst().layers());
        assertEquals(1, board.graphics().size());
        assertEquals("HELLO", board.texts().getFirst().text());
        assertEquals(1.5, board.texts().getFirst().height());
    }

    @Test
    void aFrontFootprintLosesItsTurnFromItsPads() {
        KiCadPcbFootprint resistor = board.footprints().getFirst();
        assertEquals("Resistor_SMD:R_0805", resistor.libraryId());
        assertEquals("R_0805", resistor.footprint().name());
        assertEquals("R3", resistor.reference());
        assertEquals(KiCadSide.FRONT, resistor.side());
        assertEquals(90, resistor.angle());
        assertEquals("/power/VCC", resistor.padNets().get("1"));
        KiCadPad pad = resistor.footprint().pads().getFirst();
        assertEquals(30, pad.angle(), 1e-9);
        assertEquals(-1, pad.at().x());
        assertEquals(0, resistor.lettering().orElseThrow().angle(), 1e-9);
        assertTrue(resistor.ofSchematic());
    }

    @Test
    void aBackFootprintIsUnmirroredAndOnTheFrontLayers() {
        KiCadPcbFootprint hole = board.footprints().get(1);
        assertEquals(KiCadSide.BACK, hole.side());
        assertEquals("H1", hole.reference());
        assertFalse(hole.ofSchematic());
        KiCadPad pad = hole.footprint().pads().getFirst();
        assertEquals(-2, pad.at().y());
        assertEquals(340, pad.angle(), 1e-9);
        assertEquals(java.util.List.of("*.Cu", "F.Mask"), pad.layers());
        assertEquals("F.SilkS", hole.footprint().graphics().getFirst().layer());
        assertEquals(-2, hole.lettering().orElseThrow().at().y());
    }

    @Test
    void tellsPoursFromKeepouts() {
        KiCadPcb.Zone pour = board.zones().getFirst();
        assertEquals(KiCadPcb.ZoneKind.COPPER, pour.kind());
        assertEquals("GND", pour.net());
        assertEquals(java.util.List.of("F.Cu", "B.Cu"), pour.layers());
        assertEquals(0.5, pour.clearance());
        assertEquals(3, pour.priority());
        assertEquals(3, pour.outlines().getFirst().size());
        assertEquals(KiCadPcb.ZoneKind.KEEPOUT, board.zones().get(1).kind());
    }
}
