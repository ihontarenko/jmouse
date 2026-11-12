package org.jmouse.kicad.footprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jmouse.kicad.KiCadPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

/** How far a graphic reaches: exactly, an arc and a curve at their furthest, not at the points they are written by. */
class KiCadFootprintGraphicTest {

    private static double least(List<KiCadPoint> points, boolean across) {
        return points.stream().mapToDouble(point -> across ? point.x() : point.y()).min().orElseThrow();
    }

    @Test
    void anArcReachesAsFarAsItsCircleWhereItPassesThere() {
        // Half a circle from its top through its left to its bottom: it reaches as far left as its radius, between its ends.
        KiCadFootprintGraphic arc = new KiCadFootprintGraphic.Arc(new KiCadPoint(10, -5), new KiCadPoint(6.4644661, 3.5355339),
                new KiCadPoint(10, 5), "Edge.Cuts", 0.2, false);
        assertEquals(5, least(arc.reach(), true), 1e-6);
    }

    @Test
    void aQuarterArcReachesNoFurtherThanItsEnds() {
        KiCadFootprintGraphic arc = new KiCadFootprintGraphic.Arc(new KiCadPoint(10, 0), new KiCadPoint(7.0710678, 7.0710678),
                new KiCadPoint(0, 10), "Edge.Cuts", 0.1, false);
        assertEquals(0, least(arc.reach(), true), 1e-9);
        assertEquals(0, least(arc.reach(), false), 1e-9);
    }

    @Test
    void aCurveReachesWhereItTurnsBack() {
        KiCadFootprintGraphic curve = new KiCadFootprintGraphic.Curve(new KiCadPoint(0, 0), new KiCadPoint(0, -4), new KiCadPoint(4, -4),
                new KiCadPoint(4, 0), "F.SilkS", 0.1, false);
        assertEquals(-3, least(curve.reach(), false), 1e-9);
    }
}
