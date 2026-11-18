package org.jmouse.svg.path.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcThroughTest {

    @Test
    void aQuarterOfAUnitCircleIsAQuarterOfItsRound() {
        ArcThrough arc = ArcThrough.of(1, 0, Math.sqrt(0.5), Math.sqrt(0.5), 0, 1).orElseThrow();
        assertEquals(Math.PI / 2, arc.length(), 1e-12);
        assertArrayEquals(new double[] {0, 1}, arc.pointAt(1, 1), 1e-12);
    }

    @Test
    void theArcGoesTheWayRoundThatPassesItsMiddle() {
        // From the right through the bottom to the top: three quarters, the other way round.
        ArcThrough arc = ArcThrough.of(1, 0, 0, -1, 0, 1).orElseThrow();
        assertEquals(3 * Math.PI / 2, arc.length(), 1e-12);
        assertTrue(arc.sweep() < 0);
        assertArrayEquals(new double[] {0, -1}, arc.pointAt(1, 3), 1e-12);
    }

    @Test
    void threePointsOnALineMakeNoArc() {
        assertTrue(ArcThrough.of(0, 0, 1, 1, 2, 2).isEmpty());
    }
}
