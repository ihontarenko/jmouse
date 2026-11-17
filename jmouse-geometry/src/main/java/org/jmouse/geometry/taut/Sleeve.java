package org.jmouse.geometry.taut;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * The triangles a path runs through, as the sides it crosses — each a portal, its ends on the path's left and right —, a side
 * crossed and crossed straight back over cancelled: what is left says only which way round the obstacles the path goes, and every
 * path that goes the same way has the same sleeve.
 */
final class Sleeve {

    /** A side crossed: its end on the left of the way the path goes over it, and its end on the right. */
    record Portal(Coordinate left, Coordinate right) {

        boolean sameSideAs(Portal other) {
            return left.equals2D(other.right) && right.equals2D(other.left) || left.equals2D(other.left) && right.equals2D(other.right);
        }
    }

    /** How many sides a path may cross per triangle there is before it is taken to be lost in rounding. */
    private static final int CROSSINGS_PER_TRIANGLE = 4;

    private Sleeve() {
    }

    /** The portals a path crosses, cancelled down; empty where it leaves the free room or is lost in it. */
    static Optional<List<Portal>> through(FreeSpace space, List<Coordinate> path, int triangles) {
        Optional<Integer> start = space.triangleAt(path.getFirst());
        if (start.isEmpty()) {
            return Optional.empty();
        }
        int current = start.get();
        int entered = -1;
        Deque<Portal> crossed = new ArrayDeque<>();
        int allowed = Math.max(64, triangles * CROSSINGS_PER_TRIANGLE);
        RobustLineIntersector intersector = new RobustLineIntersector();
        for (int stretch = 0; stretch + 1 < path.size(); stretch++) {
            Coordinate from = path.get(stretch);
            Coordinate to = path.get(stretch + 1);
            while (!space.triangle(current).holds(to)) {
                if (--allowed < 0) {
                    return Optional.empty();
                }
                FreeSpace.Triangle triangle = space.triangle(current);
                int exit = exitSide(triangle, entered, from, to, intersector);
                if (exit < 0 || triangle.neighbours()[exit] < 0) {
                    return Optional.empty();
                }
                Coordinate one = triangle.sideFrom(exit);
                Coordinate other = triangle.sideTo(exit);
                boolean oneOnLeft = Orientation.index(from, to, one) == Orientation.COUNTERCLOCKWISE
                                    || Orientation.index(from, to, other) == Orientation.CLOCKWISE;
                Portal portal = oneOnLeft ? new Portal(one, other) : new Portal(other, one);
                int next = triangle.neighbours()[exit];
                // A side has two triangles: crossed straight back over, it is as though it never was.
                if (!crossed.isEmpty() && crossed.peek().sameSideAs(portal)) {
                    crossed.pop();
                } else {
                    crossed.push(portal);
                }
                entered = sideTowards(space.triangle(next), current);
                current = next;
            }
            entered = -1;
        }
        List<Portal> portals = new ArrayList<>();
        crossed.descendingIterator().forEachRemaining(portals::add);
        return Optional.of(portals);
    }

    /** The side of a triangle that a stretch leaves it by, not the one it came in by; -1 where none is found. */
    private static int exitSide(FreeSpace.Triangle triangle, int entered, Coordinate from, Coordinate to, RobustLineIntersector intersector) {
        int best = -1;
        double farthest = -1;
        for (int side = 0; side < 3; side++) {
            if (side == entered) {
                continue;
            }
            intersector.computeIntersection(from, to, triangle.sideFrom(side), triangle.sideTo(side));
            if (!intersector.hasIntersection()) {
                continue;
            }
            double along = Paths.shareAlong(from, to, intersector.getIntersection(0));
            if (along > farthest) {
                farthest = along;
                best = side;
            }
        }
        return best;
    }

    /** The side of a triangle across which a neighbour stands. */
    private static int sideTowards(FreeSpace.Triangle triangle, int neighbour) {
        for (int side = 0; side < 3; side++) {
            if (triangle.neighbours()[side] == neighbour) {
                return side;
            }
        }
        return -1;
    }
}
