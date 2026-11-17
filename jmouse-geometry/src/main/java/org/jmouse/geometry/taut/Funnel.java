package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * The shortest way through a sleeve: a funnel from the last corner turned, its two sides closing in portal by portal; where one
 * side crosses over the other, the path turns at that side's end and the funnel opens again from there. The corners it turns are
 * the obstacles' corners the way has to go round, and nothing else.
 */
final class Funnel {

    private Funnel() {
    }

    /** The shortest path from a start to an end through these portals, in order. */
    static List<Coordinate> pulled(Coordinate start, List<Sleeve.Portal> crossed, Coordinate end) {
        List<Sleeve.Portal> portals = new ArrayList<>();
        portals.add(new Sleeve.Portal(start, start));
        portals.addAll(crossed);
        portals.add(new Sleeve.Portal(end, end));
        List<Coordinate> path = new ArrayList<>(List.of(start));
        Coordinate apex = start;
        Coordinate left = start;
        Coordinate right = start;
        int apexAt = 0;
        int leftAt = 0;
        int rightAt = 0;
        for (int at = 1; at < portals.size(); at++) {
            Coordinate nextLeft = portals.get(at).left();
            Coordinate nextRight = portals.get(at).right();
            // The right side closing in: the new right end left of the right side, or on it.
            if (cross(apex, right, nextRight) >= 0) {
                if (apex.equals2D(right) || cross(apex, left, nextRight) < 0) {
                    right = nextRight;
                    rightAt = at;
                } else {
                    path.add(left);
                    apex = left;
                    apexAt = leftAt;
                    right = apex;
                    rightAt = apexAt;
                    at = apexAt;
                    continue;
                }
            }
            // The left side closing in: the new left end right of the left side, or on it.
            if (cross(apex, left, nextLeft) <= 0) {
                if (apex.equals2D(left) || cross(apex, right, nextLeft) > 0) {
                    left = nextLeft;
                    leftAt = at;
                } else {
                    path.add(right);
                    apex = right;
                    apexAt = rightAt;
                    left = apex;
                    leftAt = apexAt;
                    at = apexAt;
                }
            }
        }
        if (!path.getLast().equals2D(end)) {
            path.add(end);
        }
        return Paths.withoutRepeats(path);
    }

    /** Positive where {@code point} is left of the way from {@code from} through {@code to}, negative right of it, 0 on it. */
    private static double cross(Coordinate from, Coordinate to, Coordinate point) {
        return (to.x - from.x) * (point.y - from.y) - (to.y - from.y) * (point.x - from.x);
    }
}
