package org.jmouse.kicad.pcb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadModel;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.kicad.footprint.KiCadPadShape;
import org.jmouse.kicad.footprint.KiCadPadType;
import org.jmouse.sexpression.SExpressionParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A board built is the board read: each footprint comes back as its library holds it, wherever and however it stands. */
class KiCadPcbBuilderTest {

    private static final KiCadPcbBuilder.Extent EXTENT = new KiCadPcbBuilder.Extent(0, 0, 40, 30);

    /** A footprint of a custom pad, a turned oval pad, a curve and an arc, and a model. */
    private static final KiCadFootprint FOOTPRINT = new KiCadFootprint("Odd", "An odd one", List.of(), Map.of(), List.of(
            new KiCadPad("1", KiCadPadType.SURFACE_MOUNT, KiCadPadShape.CUSTOM, new KiCadPoint(1, 2), 30, 0.5, 0.5, Optional.empty(), 0,
                    List.of("F.Cu", "F.Mask"), Optional.empty(), Optional.empty(),
                    List.of(new KiCadFootprintGraphic.Polygon(List.of(new KiCadPoint(-1, -0.5), new KiCadPoint(1, -0.5), new KiCadPoint(0, 1)), "", 0, true))),
            new KiCadPad("2", KiCadPadType.THROUGH_HOLE, KiCadPadShape.OVAL, new KiCadPoint(-3, 0.5), 90, 1.2, 2, Optional.of(new double[] {0.6, 1}),
                    0, List.of("*.Cu", "*.Mask"), Optional.of(0.05), Optional.empty(), List.of())),
            List.of(new KiCadFootprintGraphic.Curve(new KiCadPoint(0, 0), new KiCadPoint(1, -2), new KiCadPoint(3, -2), new KiCadPoint(4, 0),
                            "F.SilkS", 0.12, false),
                    new KiCadFootprintGraphic.Arc(new KiCadPoint(-2, 0), new KiCadPoint(0, 2), new KiCadPoint(2, 0), "F.Fab", 0.1, false)),
            List.of(), List.of(new KiCadModel("${KICAD8_3DMODEL_DIR}/Odd.3dshapes/Odd.wrl", new KiCadModel.Axes(0, 0, 1), KiCadModel.Axes.ONE,
                    new KiCadModel.Axes(0, 0, 90), false)));

    private static KiCadPcb builtWith(KiCadSide side, double angle) {
        KiCadPcbBuilder.Field reference = new KiCadPcbBuilder.Field(new KiCadPoint(0.5, -3), 15, "F.SilkS", 1, false);
        KiCadPcbBuilder board = KiCadPcbBuilder.of("seed", "Board", EXTENT, 2, 1.6, 0.15, 0.05)
                .footprint(new KiCadPcbBuilder.PlacedFootprint("Lib:Odd", FOOTPRINT, Optional.of("/symbol"), "U1", "Odd", side,
                        new KiCadPoint(10, 12), angle, reference, reference, Map.of("1", "IN", "2", "OUT"), false, KiCadArrangement.FREE));
        return KiCadPcb.of(new SExpressionParser().parse("board.kicad_pcb", board.print("board.kicad_pcb")));
    }

    @Test
    void aFootprintOnEitherSideTurnedAnyWayReadsBackAsItsLibraryHoldsIt() {
        for (KiCadSide side : KiCadSide.values()) {
            for (double angle : List.of(0.0, 37.5, 90.0, -120.0)) {
                KiCadPcbFootprint read = builtWith(side, angle).footprints().getFirst();
                String said = side + " " + angle;
                assertEquals(side, read.side(), said);
                assertEquals(0, Math.floorMod(Math.round(read.angle() - angle), 360), said);
                assertEquals(Optional.of("/symbol"), read.path(), said);
                assertEquals("U1", read.reference(), said);
                assertEquals(Map.of("1", "IN", "2", "OUT"), read.padNets(), said);
                for (int index = 0; index < FOOTPRINT.pads().size(); index++) {
                    KiCadPad written = FOOTPRINT.pads().get(index);
                    KiCadPad back = read.footprint().pads().get(index);
                    assertSame(written.at(), back.at(), said);
                    assertEquals(0, Math.floorMod(Math.round(written.angle() - back.angle()), 360), said + " pad " + written.number());
                    assertEquals(written.layers(), back.layers(), said);
                    assertEquals(written.primitives().size(), back.primitives().size(), said);
                }
                KiCadFootprintGraphic.Polygon primitive = (KiCadFootprintGraphic.Polygon) read.footprint().pads().getFirst().primitives().getFirst();
                assertSame(new KiCadPoint(0, 1), primitive.points().get(2), said + " primitive");
                KiCadFootprintGraphic.Curve curve = (KiCadFootprintGraphic.Curve) read.footprint().graphics().getFirst();
                assertSame(new KiCadPoint(1, -2), curve.first(), said + " curve");
                assertEquals("F.SilkS", curve.layer(), said);
                assertEquals(new KiCadPcbFootprint.Lettering(new KiCadPoint(0.5, -3), 15).at(), read.lettering().orElseThrow().at(), said);
                assertEquals(15, read.lettering().orElseThrow().angle(), 1e-9, said);
                assertEquals(1, read.footprint().models().size(), said);
            }
        }
    }

    @Test
    void theBoardStandsInTheMiddleOfThePageThatHoldsIt() {
        KiCadPcbFootprint read = builtWith(KiCadSide.FRONT, 0).footprints().getFirst();
        // A4 is 297 × 210: a board 40 × 30 stands from 128.5, 90.
        assertSame(new KiCadPoint(128.5 + 10, 90 + 12), read.at(), "where the footprint stands");
    }

    @Test
    void copperReadsBackOnItsLayersAndNets() {
        KiCadPcbBuilder board = KiCadPcbBuilder.of("seed", "Board", EXTENT, 4, 1.6, 0.15, 0.05)
                .track(new KiCadPoint(1, 1), new KiCadPoint(5, 1), 0.25, "In1.Cu", "IN")
                .arc(new KiCadPoint(5, 1), new KiCadPoint(6, 2), new KiCadPoint(7, 1), 0.25, "In1.Cu", "IN")
                .via(new KiCadPoint(7, 1), 0.6, 0.3, "F.Cu", "In2.Cu", "IN")
                .area(new KiCadPcbBuilder.Area(KiCadPcbBuilder.AreaKind.POUR, "GROUND", List.of("B.Cu"), 0.3, 1, 0.2, 0.5, 0.5,
                        List.of(new KiCadPoint(0, 0), new KiCadPoint(40, 0), new KiCadPoint(40, 30))));
        KiCadPcb read = KiCadPcb.of(new SExpressionParser().parse("board.kicad_pcb", board.print("board.kicad_pcb")));
        assertEquals(List.of("F.Cu", "In1.Cu", "In2.Cu", "B.Cu"), read.copper());
        assertEquals("In1.Cu", read.tracks().getFirst().layer());
        assertEquals("In1.Cu", read.arcs().getFirst().layer());
        assertEquals(List.of("F.Cu", "In2.Cu"), read.vias().getFirst().layers());
        assertEquals("GROUND", read.zones().getFirst().net());
        assertEquals(0.3, read.zones().getFirst().clearance(), 1e-9);
        assertEquals(1, read.zones().getFirst().priority());
        assertTrue(read.zones().getFirst().kind() == KiCadPcb.ZoneKind.COPPER);
    }

    private static void assertSame(KiCadPoint expected, KiCadPoint actual, String said) {
        assertEquals(expected.x(), actual.x(), 1e-6, said + " x");
        assertEquals(expected.y(), actual.y(), 1e-6, said + " y");
    }

    @Test
    void whatIsLockedAndGroupedReadsBackLockedAndGrouped() {
        KiCadPcbBuilder.Field reference = new KiCadPcbBuilder.Field(new KiCadPoint(0.5, -3), 0, "F.SilkS", 1, false);
        KiCadPcbBuilder board = KiCadPcbBuilder.of("seed", "Board", EXTENT, 2, 1.6, 0.15, 0.05)
                .group("front-panel", KiCadLock.LOCKED)
                .footprint(new KiCadPcbBuilder.PlacedFootprint("Lib:Odd", FOOTPRINT, Optional.empty(), "U1", "Odd", KiCadSide.FRONT,
                        new KiCadPoint(10, 12), 0, reference, reference, Map.of(), false, new KiCadArrangement("front-panel", KiCadLock.FREE)))
                .track(new KiCadPoint(1, 1), new KiCadPoint(5, 1), 0.25, "F.Cu", "IN", new KiCadArrangement("", KiCadLock.LOCKED))
                .via(new KiCadPoint(5, 1), 0.6, 0.3, "F.Cu", "B.Cu", "IN", new KiCadArrangement("front-panel", KiCadLock.FREE));
        KiCadPcb read = KiCadPcb.of(new SExpressionParser().parse("board.kicad_pcb", board.print("board.kicad_pcb")));
        assertEquals(KiCadLock.LOCKED, read.tracks().getFirst().standing().lock());
        assertEquals(KiCadLock.FREE, read.footprints().getFirst().standing().lock());
        KiCadPcb.Group group = read.groups().getFirst();
        assertEquals("front-panel", group.name());
        assertEquals(KiCadLock.LOCKED, group.standing().lock());
        assertEquals(List.of(read.footprints().getFirst().standing().identity(), read.vias().getFirst().standing().identity()), group.members());
    }
}
