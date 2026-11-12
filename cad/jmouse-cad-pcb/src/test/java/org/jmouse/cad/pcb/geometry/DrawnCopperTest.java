package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A shape drawn on a copper layer is copper: it joins what it touches, says a net, counts in its net's length where it is lines.
 * A shape on any other layer is none of these. Moved from the editor's own tests on 2026-11-12, with the copper it measured.
 */
class DrawnCopperTest {

    private static final int CURVE_STEPS = 16;

    private static PcbDrawing.Shape square(String layer, String net, String path) {
        return new PcbDrawing.Shape(layer, 0, true, net, path);
    }

    private static PcbDrawing.Shape square(String net) {
        return square(PcbDrawing.TOP, net, "M0 0H10V10H0Z");
    }

    private static PcbDrawing.Track trackThrough(double y, String layer) {
        return new PcbDrawing.Track(layer, 0.5, 0, List.of(new PcbDrawing.Point(-5, y), new PcbDrawing.Point(15, y)));
    }

    private static PcbDrawing pcb(List<PcbDrawing.Shape> shapes, List<PcbDrawing.Track> tracks, List<PcbDrawing.Via> vias, List<PcbDrawing.Arc> arcs) {
        return new PcbDrawing("circuit", Optional.empty(), List.of(), null, List.of(), List.of(), 2, null, List.of(), tracks, arcs, vias, List.of(),
                List.of(), List.of(), List.of(), List.of(), shapes, List.of());
    }

    private static CopperNets netsOf(PcbDrawing pcb) {
        return CopperNets.of(pcb, Map.of(), Map.of(), new PadOutline(CURVE_STEPS, 6), CURVE_STEPS);
    }

    @Test
    void isOnlyWhatStandsOnACopperLayer() {
        PcbDrawing pcb = pcb(List.of(square(""), square("top-mask", "", "M0 0H10V10H0Z"), square("top-silkscreen", "", "M0 0H10V10H0Z")),
                List.of(), List.of(), List.of());
        assertEquals(List.of(0), CopperShapes.drawn(pcb, CURVE_STEPS).stream().map(CopperShapes.Drawn::index).toList());
    }

    @Test
    void isOnePieceForEachOutlineItHas() {
        assertEquals(2, CopperShapes.drawn(pcb(List.of(square(PcbDrawing.TOP, "", "M0 0H1V1H0Z M5 5H6V6H5Z")), List.of(), List.of(), List.of()),
                CURVE_STEPS).size());
    }

    @Test
    void joinsTheTracksItTouchesAndGivesThemTheNetItSays() {
        CopperNets nets = netsOf(pcb(List.of(square("POWER")), List.of(trackThrough(5, PcbDrawing.TOP), trackThrough(20, PcbDrawing.TOP),
                trackThrough(5, PcbDrawing.BOTTOM)), List.of(), List.of()));
        assertEquals(List.of(Optional.of("POWER")), nets.drawnNets());
        assertEquals(List.of(Optional.of("POWER"), Optional.empty(), Optional.empty()), nets.trackNets());
        assertEquals(nets.trackGroup(0), nets.drawnGroup(0));
    }

    @Test
    void saidOnOneNetAndTouchingCopperOfAnotherIsAShort() {
        CopperNets nets = netsOf(pcb(List.of(square("POWER"), square(PcbDrawing.TOP, "GROUND", "M9 0H19V10H9Z")), List.of(), List.of(), List.of()));
        assertEquals(List.of(List.of("GROUND", "POWER")), nets.shorts().stream().map(found -> found.nets().stream().sorted().toList()).toList());
        assertEquals(List.of(Optional.empty(), Optional.empty()), nets.drawnNets());
    }

    @Test
    void keepsArcsAndViasInTheirOwnPlaces() {
        PcbDrawing.Arc arc = new PcbDrawing.Arc(PcbDrawing.BOTTOM, 0.2, new PcbDrawing.Point(30, 0), new PcbDrawing.Point(31, 1), new PcbDrawing.Point(32, 0));
        CopperNets nets = netsOf(pcb(List.of(square("POWER")), List.of(), List.of(new PcbDrawing.Via(5, 5, 0.3, 0.6, null, null)), List.of(arc)));
        assertEquals(List.of(Optional.of("POWER")), nets.viaNets());
        assertEquals(List.of(Optional.empty()), nets.arcNets());
    }

    @Test
    void countsInItsNetsLengthWhereItIsLinesAndNotWhereItIsAnArea() {
        PcbDrawing.Shape lines = new PcbDrawing.Shape(PcbDrawing.TOP, 0.5, false, "POWER", "M0 0H10V5");
        PcbDrawing pcb = pcb(List.of(lines, square(PcbDrawing.TOP, "POWER", "M20 20H30V30H20Z")), List.of(), List.of(), List.of());
        assertEquals(15, NetLengths.of(pcb, netsOf(pcb), 0).get("POWER").copper(), 1e-6);
    }
}
