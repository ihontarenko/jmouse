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
