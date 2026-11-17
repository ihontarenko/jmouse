package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * The hull that holds a stretch grown round by a distance — a capsule —: an octagon round each end, its sides at the eight ways a
 * line at 45° steps runs, and the two sides along the stretch that join them, as far off it as the distance. It holds the capsule,
 * so what keeps out of it keeps clear of the capsule; it runs along a slanting stretch as the stretch does — an octagon round the
 * whole stretch would be as wide as the stretch's whole reach across, and close what is open beside it —; and a stretch at one of
 * the eight ways gets the octagon round it, whose corners an octilinear line can be laid through.
 */
public final class StretchHull {

    /** Half the diagonal of a unit square: a 45° way's either step, exactly. */
    private static final double HALF_DIAGONAL = Math.sqrt(0.5);

    /**
     * The eight ways, each a unit step: east, north-east, north, and on round — written exactly, for a sine that should be nothing
     * but comes out a hair off it would leave a side's two corners not quite in line, and a hull with a corner too many.
     */
    private static final double[][] WAYS = {
            {1, 0}, {HALF_DIAGONAL, HALF_DIAGONAL}, {0, 1}, {-HALF_DIAGONAL, HALF_DIAGONAL},
            {-1, 0}, {-HALF_DIAGONAL, -HALF_DIAGONAL}, {0, -1}, {HALF_DIAGONAL, -HALF_DIAGONAL}};

    private StretchHull() {
    }

    /** The hull round the stretch from one point to another, grown by a distance greater than nothing. */
    public static Polygon around(Coordinate from, Coordinate to, double grow, GeometryFactory factory) {
        if (!(grow > 0)) {
            throw new IllegalArgumentException("A stretch is grown by more than nothing, not " + grow);
        }
        List<Coordinate> corners = new ArrayList<>(octagonRound(from, grow));
        corners.addAll(octagonRound(to, grow));
        return (Polygon) factory.createMultiPointFromCoords(corners.toArray(Coordinate[]::new)).convexHull();
    }

    /** The corners of the octagon round a point. */
    private static List<Coordinate> octagonRound(Coordinate point, double grow) {
        double[] reach = new double[WAYS.length];
        for (int way = 0; way < WAYS.length; way++) {
            reach[way] = along(point, way) + grow;
        }
        List<Coordinate> corners = new ArrayList<>();
        for (int way = 0; way < WAYS.length; way++) {
            corners.add(meeting(way, (way + 1) % WAYS.length, reach));
        }
        return corners;
    }

    private static double along(Coordinate point, int way) {
        return point.x * WAYS[way][0] + point.y * WAYS[way][1];
    }

    /** Where the sides facing two neighbouring ways meet. */
    private static Coordinate meeting(int first, int second, double[] reach) {
        double[] one = WAYS[first];
        double[] other = WAYS[second];
        double determinant = one[0] * other[1] - other[0] * one[1];
        return new Coordinate((reach[first] * other[1] - reach[second] * one[1]) / determinant,
                              (one[0] * reach[second] - other[0] * reach[first]) / determinant);
    }
}
