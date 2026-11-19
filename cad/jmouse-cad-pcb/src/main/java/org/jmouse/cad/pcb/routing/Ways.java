package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import java.util.List;
import java.util.Optional;

/** Ways a track runs — unit lengths —: the eight a track may take at 45°, the way from one point to another, and where two lines meet. */
public final class Ways {

    /** The sine of the angle below which two ways are taken to run together: lines nearer parallel than that meet nowhere worth the name. */
    private static final double PARALLEL = 1e-6;

    /** The eight ways a track may run at 45°, each a unit length. */
    static final List<Point> OCTILINEAR_WAYS = List.of(new Point(1, 0), diagonal(1, 1), new Point(0, 1), diagonal(-1, 1),
            new Point(-1, 0), diagonal(-1, -1), new Point(0, -1), diagonal(1, -1));

    /** The same eight ways, the four straight ones first: where two ways reach as far, the straight one is taken. */
    static final List<Point> STRAIGHT_FIRST_WAYS = List.of(new Point(1, 0), new Point(0, 1), new Point(-1, 0), new Point(0, -1),
            diagonal(1, 1), diagonal(-1, 1), diagonal(-1, -1), diagonal(1, -1));

    private Ways() {
    }

    private static Point diagonal(double x, double y) {
        return new Point(x * Math.sqrt(0.5), y * Math.sqrt(0.5));
    }

    /** The unit length from a point towards another; empty for one point. */
    public static Optional<Point> wayBetween(Point from, Point to) {
        double length = from.distanceTo(to);
        if (length < Precision.SAME_POINT) {
            return Optional.empty();
        }
        return Optional.of(new Point((to.x() - from.x()) / length, (to.y() - from.y()) / length));
    }

    /** How far a point stands along a way from another: positive ahead of it. */
    public static double reachAlong(Point from, Point way, Point point) {
        return (point.x() - from.x()) * way.x() + (point.y() - from.y()) * way.y();
    }

    /** How far from a step, as a share of its length, two lines may meet and still be turning there rather than running on beside each other. */
    private static final double FARTHEST_MEETING = 4;

    /** The least distance a meeting is measured against, in millimetres. */
    private static final double NEAREST_MEETING = 1e-3;

    /**
     * Where the lines through a step's two ends, each going its way, meet as a turn of the line: near the step. Lines meeting far from
     * it — nearly parallel, as an import leaves two runs that are one direction a hair apart — run on beside each other; empty.
     */
    public static Optional<Point> turnAt(Point end, Point endWay, Point start, Point startWay) {
        Point halfway = end.halfwayTo(start);
        double farthest = Math.max(end.distanceTo(start) * FARTHEST_MEETING, NEAREST_MEETING);
        return crossingOf(end, endWay, start, startWay).filter(point -> point.distanceTo(halfway) <= farthest);
    }

    /** Where the lines through two points, each going a way, cross; empty for lines that run together. */
    public static Optional<Point> crossingOf(Point first, Point firstWay, Point second, Point secondWay) {
        // Ways are unit lengths, so this is the sine of the angle between them.
        double across = firstWay.cross(secondWay);
        if (Math.abs(across) < PARALLEL) {
            return Optional.empty();
        }
        double reach = ((second.x() - first.x()) * secondWay.y() - (second.y() - first.y()) * secondWay.x()) / across;
        return Optional.of(first.moved(firstWay, reach));
    }
}
