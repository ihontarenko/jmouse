package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where lines from a few points are shortest together, as the lines are laid: measured at 45° steps — the longer way and the
 * shorter one's diagonal —, at 90° — the two ways added —, or straight. At 45° or 90° the length is a sum of distances that bends
 * only along the lines through each point at the steps' angles, so the shortest is where two of those lines cross: each crossing is
 * tried. Straight, Weiszfeld's steps find it.
 */
final class FermatPoints {

    /** How many of Weiszfeld's steps are taken at most, and how near the next one comes before they stop, in millimetres. */
    private static final int WEISZFELD_STEPS = 64;
    private static final double RESTED = 1e-6;

    private FermatPoints() {
    }

    /** Where lines from these points are shortest together, at an angle step; {@code from} where there are fewer than two. */
    static Point of(Point from, List<Point> points, double angleStep) {
        if (points.size() < 2) {
            return from;
        }
        if (angleStep == 45 || angleStep == 90) {
            return crossingShortest(from, points, angleStep);
        }
        return weiszfeld(from, points);
    }

    /** The length of a line laid from one point to another at an angle step: its ways as a track at that step runs them. */
    static double laidLength(Point from, Point to, double angleStep) {
        double dx = Math.abs(to.x() - from.x());
        double dy = Math.abs(to.y() - from.y());
        if (angleStep == 45) {
            return Math.max(dx, dy) + (Math.sqrt(2) - 1) * Math.min(dx, dy);
        }
        if (angleStep == 90) {
            return dx + dy;
        }
        return Math.hypot(dx, dy);
    }

    /**
     * Of the crossings of the lines through the points — and through where the joint stands — at the steps' angles, the one whose
     * lines to the points are shortest; of as short ones, the nearest the joint: where the shortest is a whole stretch — two lines
     * through a via —, the joint goes no further than onto it.
     */
    private static Point crossingShortest(Point from, List<Point> points, double angleStep) {
        double half = Math.sqrt(0.5);
        List<Point> ways = angleStep == 45
                ? List.of(new Point(1, 0), new Point(0, 1), new Point(half, half), new Point(half, -half))
                : List.of(new Point(1, 0), new Point(0, 1));
        List<Point> through = new ArrayList<>(points);
        through.add(from);
        List<Point> candidates = new ArrayList<>(through);
        for (int first = 0; first < through.size(); first++) {
            for (int second = first + 1; second < through.size(); second++) {
                for (Point firstWay : ways) {
                    for (Point secondWay : ways) {
                        Optional<Point> crossing = Lines.crossingOf(through.get(first), firstWay, through.get(second), secondWay);
                        crossing.ifPresent(candidates::add);
                    }
                }
            }
        }
        Point best = from;
        double shortest = Double.POSITIVE_INFINITY;
        for (Point candidate : candidates) {
            double length = points.stream().mapToDouble(point -> laidLength(candidate, point, angleStep)).sum();
            boolean shorter = length < shortest - Lines.SAME_POINT;
            boolean asShortAndNearer = Math.abs(length - shortest) <= Lines.SAME_POINT
                                       && Lines.lengthBetween(candidate, from) < Lines.lengthBetween(best, from);
            if (shorter || asShortAndNearer) {
                shortest = Math.min(shortest, length);
                best = candidate;
            }
        }
        return best;
    }

    /** Weiszfeld's steps from a point towards where the straight lines to the points are shortest. */
    private static Point weiszfeld(Point from, List<Point> points) {
        Point at = from;
        for (int step = 0; step < WEISZFELD_STEPS; step++) {
            double x = 0;
            double y = 0;
            double weight = 0;
            for (Point point : points) {
                double distance = Lines.lengthBetween(at, point);
                if (distance < RESTED) {
                    return point;
                }
                x += point.x() / distance;
                y += point.y() / distance;
                weight += 1 / distance;
            }
            Point next = new Point(x / weight, y / weight);
            boolean rested = Lines.lengthBetween(next, at) < RESTED;
            at = next;
            if (rested) {
                break;
            }
        }
        return at;
    }
}
