package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.ViaSize;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.NetRules;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.AreaOffsets;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.ZonePours;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vias made over poured zones: a zone stitched to its net's pour on the other side, a track fenced by a net poured on both. Each
 * via stands where both pours lie under its whole pad and keeps its gap from everything else.
 */
class ViaRowsTest {

    private static final int CURVE_STEPS = 16;

    private static final double ARC_ERROR = 0.005;

    private static final Map<String, Double> VALUES = Map.of("clearance", 0.2, "edge-clearance", 0.3, "hole-clearance", 0.25, "thermal-gap", 0.3,
            "thermal-spoke-width", 0.3, "least-track-width", 0.15);

    private static final ViaSize VIA = new ViaSize(0.3, 0.6);

    private static final String SQUARE = "M0 0H20V20H0Z";

    /** A through-hole test point: what a pour of its net is anchored by, so it is not taken away as an island. */
    private static final FootprintDrawing POINT = new FootprintDrawing(List.of(new FootprintDrawing.Pad("1", "through-hole", "circle", 0, 0, 0, 1.6, 1.6,
            0.8, 0.8, 0, "", "", "", null, null)), List.of(), List.of(), null, List.of());

    /** A pcb poured with ground on both sides, or one, a ground test point in two corners, and a signal track between two of its own. */
    private static ViaRows rowsOf(List<Zone> zones, Track track) {
        List<Placement> points = List.of(new Placement("TP1", 2, 2, 0, PcbVocabulary.TOP, Optional.empty()),
                new Placement("TP2", 18, 18, 0, PcbVocabulary.TOP, Optional.empty()),
                new Placement("TP3", 2, 10, 0, PcbVocabulary.TOP, Optional.empty()),
                new Placement("TP4", 18, 10, 0, PcbVocabulary.TOP, Optional.empty()));
        PcbDrawing pcb = new PcbDrawing("circuit", Optional.empty(), List.of(), null, List.of(), List.of(), 2, SQUARE, points, List.of(track), List.of(),
                List.of(), zones, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        CopperNets nets = CopperNets.of(pcb, Map.of("TP1", POINT, "TP2", POINT, "TP3", POINT, "TP4", POINT), Map.of("TP1", Map.of("1", "GND"),
                "TP2", Map.of("1", "GND"), "TP3", Map.of("1", "SIG"), "TP4", Map.of("1", "SIG")),
                new PadOutline(CURVE_STEPS, 6), CURVE_STEPS);
        List<Optional<org.locationtech.jts.geom.Geometry>> pours = new ZonePours(pcb, nets, CopperPieces.of(pcb, nets, List.of(), CURVE_STEPS), VALUES, CURVE_STEPS, ARC_ERROR).pour();
        return new ViaRows(pcb, nets, pours, new NetRules(pcb, VALUES), new AreaOffsets(ARC_ERROR), CURVE_STEPS);
    }

    private static final Track SIGNAL = new Track(PcbVocabulary.TOP, 0.25, 0,
            List.of(new Point(2, 10), new Point(18, 10)));

    private static final List<Zone> BOTH = List.of(new Zone(PcbVocabulary.TOP, "GND", 0, 0, SQUARE),
            new Zone(PcbVocabulary.BOTTOM, "GND", 0, 0, SQUARE));

    /** Whether a via keeps the gap the rules give from the signal track. */
    private static boolean clearOfSignal(Via via) {
        return CopperShapes.via(via).gapTo(CopperShapes.track(SIGNAL, CURVE_STEPS)).gap() >= 0.2 - 1e-6;
    }

    @Test
    void stitchesAZoneToItsNetsPourOnTheOtherSideOnTheGridAndClearOfOtherCopper() {
        List<Via> vias = assertInstanceOf(ViaRows.Placed.Vias.class, rowsOf(BOTH, SIGNAL).stitching(0, 2.5, Optional.of(VIA))).vias();
        assertFalse(vias.isEmpty());
        for (Via via : vias) {
            assertEquals(0, Math.abs(via.x() / 2.5 - Math.round(via.x() / 2.5)), 1e-9, "on the pcb's grid: " + via);
            assertTrue(clearOfSignal(via), "clear of the signal: " + via);
        }
        // Stitched again, the same grid gives the same vias: stitching adds only what is missing.
        assertEquals(vias, assertInstanceOf(ViaRows.Placed.Vias.class, rowsOf(BOTH, SIGNAL).stitching(0, 2.5, Optional.of(VIA))).vias());
    }

    @Test
    void refusesWithWordsWhereNoOtherLayerPoursTheNetOrThePitchIsTooTight() {
        String alone = assertInstanceOf(ViaRows.Placed.Refused.class, rowsOf(BOTH.subList(0, 1), SIGNAL).stitching(0, 2.5, Optional.of(VIA))).refusal();
        assertTrue(alone.contains("No other layer pours GND"), alone);
        String tight = assertInstanceOf(ViaRows.Placed.Refused.class, rowsOf(BOTH, SIGNAL).stitching(0, 0.4, Optional.of(VIA))).refusal();
        assertTrue(tight.contains("at least 0.6 mm apart"), tight);
    }

    @Test
    void fencesATrackDownEachSideAsNearAsItsGapAllows() {
        List<Via> vias = assertInstanceOf(ViaRows.Placed.Vias.class, rowsOf(BOTH, SIGNAL).fence(0, "GND", 1.5, Optional.of(VIA))).vias();
        assertTrue(vias.stream().anyMatch(via -> via.y() < 10) && vias.stream().anyMatch(via -> via.y() > 10), "both sides: " + vias);
        for (Via via : vias) {
            assertTrue(clearOfSignal(via), "clear of the track it guards: " + via);
            assertTrue(Math.abs(via.y() - 10) < 1.5, "near the track: " + via);
        }
        String own = assertInstanceOf(ViaRows.Placed.Refused.class, rowsOf(BOTH, new Track(PcbVocabulary.TOP, 0.25, 0,
                List.of(new Point(2, 10), new Point(18, 10)))).fence(0, "SIGNAL", 1.5, Optional.of(VIA))).refusal();
        assertTrue(own.contains("No two layers pour SIGNAL"), own);
    }

}
