package org.jmouse.cad.library;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.Teardrops;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A drop widens a track into the copper it enters and never stands out of it: whatever the pad's shape, however it is turned,
 * wherever on it the track ends. Ivan's board (2026-11-06) had bottom tracks entering pads near their corners, and each drop
 * stuck out of the pad on both sides of the track as a fishtail. Moved from the editor's own test with the drops themselves
 * on 2026-11-11: the editor draws the engine's.
 */
class TeardropsTest {

    /** How the artwork shapes a drop (`pcb-artwork.json`). */
    private static final Teardrops.Shape SHAPE = new Teardrops.Shape(8, 0.9, 0.8, 14, 24);

    private static final double HALF_TRACK = 0.2;

    /** A ten-thousandth for the halving that finds the copper's edge, and the places a figure is written to. */
    private static final double SLACK = 1e-3;

    private record Case(String name, String pad, int rotation, double[][] track) {
    }

    private static final List<Case> CASES = List.of(
            new Case("into a square pad's middle", "rectangle, at: 0 0, size: 1.7 x 1.7", 0, new double[][] {{10, 10}, {10, 16}}),
            new Case("into a square pad near its corner, from below", "rectangle, at: 0 0, size: 1.7 x 1.7", 0, new double[][] {{10.6, 10.5}, {10.6, 16}}),
            new Case("into a square pad's corner, on a diagonal", "rectangle, at: 0 0, size: 1.7 x 1.7", 0, new double[][] {{10.6, 10.6}, {15, 15}}),
            new Case("into a round pad off its middle", "circle, at: 0 0, size: 1.7 x 1.7", 0, new double[][] {{10.5, 10.3}, {10.5, 16}}),
            new Case("into a rounded pad turned 45°", "rounded-rectangle, at: 0 0, rotate: 45, size: 2.4 x 1.2", 0, new double[][] {{10.3, 10}, {16, 10}}),
            new Case("into an oval pad along its length", "oval, at: 0 0, size: 1.2 x 2.4", 90, new double[][] {{10.4, 10.2}, {10.4, 16}}));

    private record Laid(List<Teardrops.Drop> drops, CopperShape copper) {
    }

    private static Laid laid(String pad, int rotation, double[][] track) {
        FootprintDrawing footprint = FootprintDrawing.of(CadLanguage.shipped().language().readOne("""
                footprint:drop-probe("Drop probe") {
                    pad:1(through-hole, %s, drill: 0.8)
                }
                """.formatted(pad), "drop-probe"));
        PcbDrawing pcb = PcbDrawing.of(CadLanguage.shipped().language().readOne(String.format(Locale.ROOT, """
                pcb:drop-board("Drop board") {
                    circuit(nowhere)
                    layers(2)
                    teardrops(round, length: 0.5, width: 1, curved)
                    place(P, at: 10 10, rotate: %d)
                    track(bottom, width: 0.4, from: %s %s, to: %s %s)
                }
                """, rotation, track[0][0], track[0][1], track[1][0], track[1][1]), "drop-board"));
        CopperNets nets = CopperNets.of(pcb, Map.of("P", footprint), Map.of(), new PadOutline(16, 6), 16);
        return new Laid(Teardrops.of(pcb, nets.pads(), SHAPE), nets.pads().getFirst().copper());
    }

    @Test
    void standsItsBaseOnThePadAndGrowsNoWiderThanIt() {
        for (Case example : CASES) {
            Laid laid = laid(example.pad(), example.rotation(), example.track());
            assertFalse(laid.drops().isEmpty(), example.name());
            double[] end = example.track()[0];
            double[] next = example.track()[1];
            double length = Math.hypot(next[0] - end[0], next[1] - end[1]);
            double[] along = {(next[0] - end[0]) / length, (next[1] - end[1]) / length};
            for (Teardrops.Drop drop : laid.drops()) {
                List<Point> corners = drop.outline();
                Point first = corners.getFirst();
                Point last = corners.getLast();
                for (Point base : List.of(first, last)) {
                    assertTrue(onCopper(laid.copper(), base) || Math.abs(lateral(along, end, base)) <= HALF_TRACK + SLACK,
                            example.name() + ": base " + base + " stands on the pad");
                }
                // Its narrow end is the track: the corners farthest along stand on the track's two edges, not beside it.
                double farthest = corners.stream().mapToDouble(corner -> forward(along, end, corner)).max().orElseThrow();
                List<Double> tip = corners.stream().filter(corner -> forward(along, end, corner) > farthest - SLACK)
                        .map(corner -> lateral(along, end, corner)).sorted().toList();
                assertEquals(2, tip.size(), example.name() + ": the narrow end");
                assertEquals(-HALF_TRACK, tip.getFirst(), SLACK, example.name() + ": the narrow end's one edge");
                assertEquals(HALF_TRACK, tip.getLast(), SLACK, example.name() + ": the narrow end's other edge");
                // No fishtail: on each side, nothing stands farther out than that side's base, or the track's own edge.
                for (Point corner : corners) {
                    double out = lateral(along, end, corner);
                    Point sideBase = (out >= 0) == (lateral(along, end, first) >= 0) ? first : last;
                    double limit = Math.max(HALF_TRACK, Math.abs(lateral(along, end, sideBase)));
                    assertTrue(Math.abs(out) <= limit + SLACK, example.name() + ": " + corner + " stands out of the drop's side");
                }
            }
        }
    }

    @Test
    void isAsWideOnBothSidesWhenTheTrackEntersTheCopperMiddle() {
        List<Point> corners = laid("circle, at: 0 0, size: 1.7 x 1.7", 0, new double[][] {{10, 10}, {10, 16}}).drops().getFirst().outline();
        double left = corners.stream().map(Point::x).min(Comparator.naturalOrder()).orElseThrow();
        double right = corners.stream().map(Point::x).max(Comparator.naturalOrder()).orElseThrow();
        assertEquals(10 - left, right - 10, SLACK);
        assertTrue(right - left > 0.4);
    }

    @Test
    void isWidenedOnlyOnTheSideThePadIsForATrackAtItsEdge() {
        // The track's right edge stands just past the pad's right side: there is no copper to widen into on the right.
        List<Point> corners = laid("rectangle, at: 0 0, size: 1.7 x 1.7", 0, new double[][] {{10.8, 10.4}, {10.8, 16}}).drops().getFirst().outline();
        assertTrue(corners.stream().mapToDouble(Point::x).max().orElseThrow() <= 10.8 + HALF_TRACK + SLACK);
        assertTrue(corners.stream().mapToDouble(Point::x).min().orElseThrow() < 10.8 - HALF_TRACK - 0.1);
    }

    private static boolean onCopper(CopperShape copper, Point point) {
        double[][] steps = {{0, 0}, {SLACK, 0}, {-SLACK, 0}, {0, SLACK}, {0, -SLACK}};
        for (double[] step : steps) {
            if (copper.covers(point.x() + step[0], point.y() + step[1])) {
                return true;
            }
        }
        return false;
    }

    private static double lateral(double[] along, double[] end, Point point) {
        return along[0] * (point.y() - end[1]) - along[1] * (point.x() - end[0]);
    }

    private static double forward(double[] along, double[] end, Point point) {
        return along[0] * (point.x() - end[0]) + along[1] * (point.y() - end[1]);
    }
}
