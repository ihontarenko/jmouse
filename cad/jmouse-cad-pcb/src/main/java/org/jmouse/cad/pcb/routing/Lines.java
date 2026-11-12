package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The shape of a track, as every part of the router speaks of it: a line of points, and the steps a track is laid by — every
 * 90°, 45°, any other step, or any way. Geometry only, in millimetres, nothing of a pcb.
 */
public final class Lines {

    /** How near two points must be to be one: what a coordinate written to the pcb's places loses. */
    public static final double SAME_POINT = Math.pow(10, -PcbVocabulary.DECIMALS);

    /** How far off its straight stretch a point may stand and still lie on it: what a few written coordinates lose between them. */
    private static final double ON_STRETCH = 1e-3;

    /** The sine of the angle below which two ways are taken to run together: lines nearer parallel than that meet nowhere worth the name. */
    private static final double PARALLEL = 1e-6;

    /** How far past a right angle a corner may seem to turn and still be one: what points written to four places lose over a short stretch. */
    private static final double SQUARE = 1e-2;

    /** How much a cross product may be and the point still stand on the straight run through its neighbours. */
    private static final double STRAIGHT = 1e-9;

    /** The scale a point is written to. */
    private static final double SCALE = Math.pow(10, PcbVocabulary.DECIMALS);

    /** The eight ways a track may run at 45°, each a unit length. */
    static final List<Point> OCTILINEAR_WAYS = List.of(new Point(1, 0), diagonal(1, 1), new Point(0, 1), diagonal(-1, 1),
            new Point(-1, 0), diagonal(-1, -1), new Point(0, -1), diagonal(1, -1));

    /** The same eight ways, the four straight ones first: where two ways reach as far, the straight one is taken. */
    static final List<Point> STRAIGHT_FIRST_WAYS = List.of(new Point(1, 0), new Point(0, 1), new Point(-1, 0), new Point(0, -1),
            diagonal(1, 1), diagonal(-1, 1), diagonal(-1, -1), diagonal(1, -1));

    private Lines() {
    }

    private static Point diagonal(double x, double y) {
        return new Point(x * Math.sqrt(0.5), y * Math.sqrt(0.5));
    }

    public static double lengthBetween(Point from, Point to) {
        return Math.hypot(to.x() - from.x(), to.y() - from.y());
    }

    public static boolean samePoint(Point first, Point second) {
        return lengthBetween(first, second) < SAME_POINT;
    }

    /** How long a line is, stretch by stretch. */
    public static double lengthOf(List<Point> points) {
        double sum = 0;
        for (int index = 1; index < points.size(); index++) {
            sum += lengthBetween(points.get(index - 1), points.get(index));
        }
        return sum;
    }

    /** Whether a point stands on the straight stretch between two others. */
    public static boolean liesBetween(Point point, Point from, Point to) {
        return Math.abs(lengthBetween(from, point) + lengthBetween(point, to) - lengthBetween(from, to)) < ON_STRETCH;
    }

    /** Whether a line runs through a point: at a corner of it, or along one of its stretches. */
    public static boolean passesThrough(List<Point> line, Point point) {
        for (int index = 0; index < line.size(); index++) {
            if (samePoint(line.get(index), point) || (index > 0 && liesBetween(point, line.get(index - 1), line.get(index)))) {
                return true;
            }
        }
        return false;
    }

    /** A point written to the pcb's places. */
    public static Point settled(Point point) {
        return new Point(settled(point.x()), settled(point.y()));
    }

    /** A coordinate written to the pcb's places. */
    public static double settled(double value) {
        return Math.round(value * SCALE) / SCALE;
    }

    /** A coordinate written to so many places. */
    public static double writtenTo(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }

    /** The unit length from a point towards another; empty for one point. */
    public static Optional<Point> wayBetween(Point from, Point to) {
        double length = lengthBetween(from, to);
        if (length < SAME_POINT) {
            return Optional.empty();
        }
        return Optional.of(new Point((to.x() - from.x()) / length, (to.y() - from.y()) / length));
    }

    /** How far a point stands along a way from another: positive ahead of it. */
    public static double reachAlong(Point from, Point way, Point point) {
        return (point.x() - from.x()) * way.x() + (point.y() - from.y()) * way.y();
    }

    public static double dot(Point first, Point second) {
        return first.x() * second.x() + first.y() * second.y();
    }

    public static double cross(Point first, Point second) {
        return first.x() * second.y() - first.y() * second.x();
    }

    public static Point offset(Point point, Point way, double distance) {
        return new Point(point.x() + way.x() * distance, point.y() + way.y() * distance);
    }

    /** Where the lines through two points, each going a way, cross; empty for lines that run together. */
    public static Optional<Point> crossingOf(Point first, Point firstWay, Point second, Point secondWay) {
        // Ways are unit lengths, so this is the sine of the angle between them.
        double across = cross(firstWay, secondWay);
        if (Math.abs(across) < PARALLEL) {
            return Optional.empty();
        }
        double reach = ((second.x() - first.x()) * secondWay.y() - (second.y() - first.y()) * secondWay.x()) / across;
        return Optional.of(offset(first, firstWay, reach));
    }

    /**
     * A line's corners: its two ends and every point where it turns — a point standing on a straight run between its
     * neighbours is no corner and is left out.
     */
    public static List<Point> cornersOf(List<Point> line) {
        List<Point> corners = new ArrayList<>();
        for (int index = 0; index < line.size(); index++) {
            if (index == 0 || index == line.size() - 1) {
                corners.add(line.get(index));
                continue;
            }
            Point point = line.get(index);
            Point previous = line.get(index - 1);
            Point next = line.get(index + 1);
            double turn = (point.x() - previous.x()) * (next.y() - point.y()) - (point.y() - previous.y()) * (next.x() - point.x());
            if (Math.abs(turn) > STRAIGHT) {
                corners.add(point);
            }
        }
        return corners;
    }

    /** Points with the ones that stand on a straight run between their neighbours, or on their neighbour, left out. */
    public static List<Point> withoutStraightPoints(List<Point> points) {
        List<Point> kept = new ArrayList<>();
        for (Point point : points) {
            if (kept.isEmpty() || !samePoint(kept.getLast(), point)) {
                kept.add(point);
            }
        }
        return cornersOf(kept);
    }

    /**
     * The points a track is carried through from {@code from} toward {@code to}, the last of them where it stops. At 90° it
     * goes across then down; at 45° straight along the longer way, then diagonally — both reach the pointer exactly, as a
     * router lays copper. At any other step the one segment is turned to the nearest allowed direction and runs as far as the
     * pointer reaches along it; with no step it goes straight there.
     */
    public static List<Point> trackStep(Point from, Point to, double step) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        if (step == 0 || (dx == 0 && dy == 0)) {
            return List.of(to);
        }
        if (step == 90) {
            return dx == 0 || dy == 0 ? List.of(to) : List.of(new Point(to.x(), from.y()), to);
        }
        if (step == 45) {
            double diagonal = Math.min(Math.abs(dx), Math.abs(dy));
            Point corner = Math.abs(dx) > Math.abs(dy)
                    ? new Point(from.x() + Math.signum(dx) * (Math.abs(dx) - diagonal), from.y())
                    : new Point(from.x(), from.y() + Math.signum(dy) * (Math.abs(dy) - diagonal));
            boolean atAnEnd = (corner.x() == from.x() && corner.y() == from.y()) || (corner.x() == to.x() && corner.y() == to.y());
            return atAnEnd ? List.of(to) : List.of(corner, to);
        }
        double radians = Math.toRadians(step);
        double direction = Math.round(Math.atan2(dy, dx) / radians) * radians;
        double along = Math.max(0, dx * Math.cos(direction) + dy * Math.sin(direction));
        return List.of(new Point(from.x() + along * Math.cos(direction), from.y() + along * Math.sin(direction)));
    }

    /** The turn at a line's inner corner: whether the stretch going on turns back sharper than a right angle. */
    private static boolean foldsAt(List<Point> line, int index) {
        if (index == 0 || index == line.size() - 1) {
            return false;
        }
        Optional<Point> coming = wayBetween(line.get(index - 1), line.get(index));
        Optional<Point> going = wayBetween(line.get(index), line.get(index + 1));
        return coming.isPresent() && going.isPresent() && dot(coming.get(), going.get()) < -SQUARE;
    }

    /**
     * A line with no corner sharper than a right angle: what a neighbour's far corner does when what it leads to has been
     * carried past it. The stretch before stops where one straight run at a step's angle, turning no sharper, reaches the
     * corner after: the farthest such point along it, so the line is changed as little as it can be. Where there is none, the
     * two are joined by a plain step.
     */
    public static List<Point> withoutFolds(List<Point> points) {
        List<Point> line = withoutStraightPoints(points);
        for (int round = points.size() * 2; round > 0; round--) {
            int at = -1;
            for (int index = 0; index < line.size(); index++) {
                if (foldsAt(line, index)) {
                    at = index;
                    break;
                }
            }
            if (at < 0) {
                break;
            }
            Point previous = line.get(at - 1);
            Point corner = line.get(at);
            Point next = line.get(at + 1);
            Point coming = wayBetween(previous, corner).orElseThrow();
            Point best = null;
            double bestReach = 0;
            for (Point way : OCTILINEAR_WAYS) {
                Optional<Point> met = crossingOf(previous, coming, next, way);
                if (met.isEmpty()) {
                    continue;
                }
                double reach = reachAlong(previous, coming, met.get());
                Optional<Point> onward = wayBetween(met.get(), next);
                if (reach > 0 && reach < lengthBetween(previous, corner) && onward.isPresent() && dot(coming, onward.get()) >= -SQUARE
                    && (best == null || reach > bestReach)) {
                    best = settled(met.get());
                    bestReach = reach;
                }
            }
            List<Point> changedLine = new ArrayList<>(line.subList(0, at));
            if (best != null) {
                changedLine.add(best);
            } else {
                List<Point> step = trackStep(previous, next, 45);
                changedLine.addAll(step.subList(0, step.size() - 1));
            }
            changedLine.addAll(line.subList(at + 1, line.size()));
            line = withoutStraightPoints(changedLine);
        }
        return line;
    }

    /** A line that crosses itself cut at the crossing: the loop beyond it gone. */
    public static List<Point> withoutLoops(List<Point> points) {
        List<Point> line = List.copyOf(points);
        for (int first = 1; first < line.size(); first++) {
            for (int second = line.size() - 1; second > first + 1; second--) {
                Point a = line.get(first - 1);
                Point b = line.get(first);
                Point c = line.get(second - 1);
                Point d = line.get(second);
                Optional<Point> away = wayBetween(a, b);
                Optional<Point> onward = wayBetween(c, d);
                if (away.isEmpty() || onward.isEmpty()) {
                    continue;
                }
                Optional<Point> met = crossingOf(a, away.get(), c, onward.get());
                if (met.isPresent() && reachAlong(a, away.get(), met.get()) > 0 && lengthBetween(a, met.get()) < lengthBetween(a, b)
                    && reachAlong(c, onward.get(), met.get()) > 0 && lengthBetween(c, met.get()) < lengthBetween(c, d)) {
                    List<Point> cut = new ArrayList<>(line.subList(0, first));
                    cut.add(settled(met.get()));
                    cut.addAll(line.subList(second, line.size()));
                    return withoutLoops(cut);
                }
            }
        }
        return line;
    }

    /** Whether a line crosses itself or turns back sharper than a right angle. */
    public static boolean isTangled(List<Point> points) {
        if (withoutLoops(points).size() != points.size()) {
            return true;
        }
        List<Point> unfolded = withoutFolds(points);
        for (int index = 0; index < unfolded.size(); index++) {
            if (index >= points.size() || !samePoint(unfolded.get(index), points.get(index))) {
                return true;
            }
        }
        return false;
    }

    /** A line of these parts, one after another. */
    @SafeVarargs
    public static List<Point> joined(List<Point>... parts) {
        List<Point> line = new ArrayList<>();
        for (List<Point> part : parts) {
            line.addAll(part);
        }
        return line;
    }

    /** A line run the other way. */
    public static List<Point> reversed(List<Point> line) {
        return new ArrayList<>(line).reversed();
    }
}
