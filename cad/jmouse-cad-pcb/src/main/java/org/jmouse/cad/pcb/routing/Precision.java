package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;

import java.util.ArrayList;
import java.util.List;

/**
 * The precision a pcb keeps its places to: a point written to it, two points that are one within it, and the kept points round one
 * that is not.
 */
public final class Precision {

    /** How near two points must be to be one: what a coordinate written to the pcb's places loses. */
    public static final double SAME_POINT = Math.pow(10, -PcbVocabulary.DECIMALS);

    /** The scale a point is written to. */
    private static final double SCALE = Math.pow(10, PcbVocabulary.DECIMALS);

    /** How far past a kept place a scaled coordinate may stand and still be on it: what arithmetic in binary loses. */
    private static final double ON_PLACE = 1e-9;

    private Precision() {
    }

    /** Whether two points are one, to the precision kept. */
    public static boolean samePoint(Point first, Point second) {
        return first.distanceTo(second) < SAME_POINT;
    }

    /** A point written to the pcb's places. */
    public static Point settled(Point point) {
        return new Point(settled(point.x()), settled(point.y()));
    }

    /** A coordinate written to the pcb's places. */
    public static double settled(double value) {
        return Math.round(value * SCALE) / SCALE;
    }

    /**
     * A point settled to the precision kept, each of its coordinates rounded the way a direction goes rather than to the nearest —
     * a corner pulled tight round copper settled away from it, never into it.
     */
    public static Point settledTowards(Point point, Point direction) {
        return new Point(settledTowards(point.x(), direction.x()), settledTowards(point.y(), direction.y()));
    }

    private static double settledTowards(double value, double direction) {
        double scaled = value * SCALE;
        if (direction > 0) {
            return Math.ceil(scaled - ON_PLACE) / SCALE;
        }
        if (direction < 0) {
            return Math.floor(scaled + ON_PLACE) / SCALE;
        }
        return Math.round(scaled) / SCALE;
    }

    /** The points kept to the precision round a point: its coordinates each rounded down and up — four, or fewer where it is kept already. */
    public static List<Point> keptPointsAround(Point point) {
        double[] xs = {Math.floor(point.x() * SCALE + ON_PLACE) / SCALE, Math.ceil(point.x() * SCALE - ON_PLACE) / SCALE};
        double[] ys = {Math.floor(point.y() * SCALE + ON_PLACE) / SCALE, Math.ceil(point.y() * SCALE - ON_PLACE) / SCALE};
        List<Point> around = new ArrayList<>();
        for (double x : xs) {
            for (double y : ys) {
                Point kept = new Point(x, y);
                if (!around.contains(kept)) {
                    around.add(kept);
                }
            }
        }
        return around;
    }

    /** A coordinate written to so many places. */
    public static double writtenTo(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}
