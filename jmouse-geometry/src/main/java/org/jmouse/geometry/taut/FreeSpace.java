package org.jmouse.geometry.taut;

import java.util.Arrays;

import org.jmouse.geometry.index.BoxTree;

import org.jmouse.geometry.index.BoxIndex;

import org.jmouse.geometry.index.Box;

import org.jmouse.geometry.Areas;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.triangulate.polygon.ConstrainedDelaunayTriangulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The room between obstacles within a window — the window with the obstacles cut out of it — cut into triangles whose sides never
 * cross an obstacle's edge — a constrained Delaunay triangulation —, each knowing its neighbours. A path through it is told by the sides it crosses: that is the way it goes round
 * the obstacles, whatever its shape.
 */
final class FreeSpace {

    /**
     * One triangle: its corners counter-clockwise, and across each side — the side from corner {@code n} to the next — the
     * neighbour there, or none: a side on an obstacle's edge or the window's.
     */
    record Triangle(Coordinate[] corners, int[] neighbours) {

        Coordinate sideFrom(int side) {
            return corners[side];
        }

        Coordinate sideTo(int side) {
            return corners[(side + 1) % 3];
        }

        /** Whether a point is inside or on the triangle. */
        boolean holds(Coordinate point) {
            for (int side = 0; side < 3; side++) {
                if (Orientation.index(sideFrom(side), sideTo(side), point) == Orientation.CLOCKWISE) {
                    return false;
                }
            }
            return true;
        }
    }

    /** A side, as its two ends whichever way round. */
    private record Side(double firstX, double firstY, double secondX, double secondY) {

        static Side of(Coordinate one, Coordinate other) {
            boolean ordered = one.x < other.x || one.x == other.x && one.y <= other.y;
            Coordinate first = ordered ? one : other;
            Coordinate second = ordered ? other : one;
            return new Side(first.x, first.y, second.x, second.y);
        }
    }

    /** How near the window's edge a corner lies on it, as a share of the window's size: the arithmetic of cutting it leaves no more. */
    private static final double EDGE_NEAR = 1e-9;

    private final List<Triangle> triangles;
    private final BoxIndex       index;

    private FreeSpace(List<Triangle> triangles) {
        this.triangles = triangles;
        this.index = BoxTree.of(triangles.stream().map(FreeSpace::boxOf).toList());
    }

    /** The box a triangle takes: its corners'. */
    private static Box boxOf(Triangle triangle) {
        Box box = null;
        for (Coordinate corner : triangle.corners()) {
            Box at = Box.point(corner.x, corner.y);
            box = box == null ? at : box.joined(at);
        }
        return box;
    }

    /** The free room of a field within a window; empty where it cannot be cut into triangles. */
    static Optional<FreeSpace> of(ObstacleField field, Envelope window) {
        GeometryFactory factory = field.factory();
        Geometry box = factory.toGeometry(window);
        Geometry room = field.area().isEmpty() ? box : field.grid().difference(box, field.area());
        Geometry cut;
        try {
            cut = ConstrainedDelaunayTriangulator.triangulate(room);
        } catch (RuntimeException uncut) {
            // A room JTS cannot cut — rings it finds invalid —: the room is not known.
            return Optional.empty();
        }
        List<Coordinate[]> free = new ArrayList<>();
        for (int number = 0; number < cut.getNumGeometries(); number++) {
            Polygon triangle = (Polygon) cut.getGeometryN(number);
            free.add(counterClockwise(triangle.getExteriorRing().getCoordinates()));
        }
        return Optional.of(new FreeSpace(joined(free)));
    }

    private static Coordinate[] counterClockwise(Coordinate[] ring) {
        Coordinate[] corners = {ring[0], ring[1], ring[2]};
        if (Orientation.index(corners[0], corners[1], corners[2]) == Orientation.CLOCKWISE) {
            return new Coordinate[] {corners[0], corners[2], corners[1]};
        }
        return corners;
    }

    /** The triangles, each told its neighbours across the sides two of them share. */
    private static List<Triangle> joined(List<Coordinate[]> free) {
        Map<Side, int[]> sides = new HashMap<>();
        List<Triangle> triangles = new ArrayList<>();
        for (int number = 0; number < free.size(); number++) {
            Triangle triangle = new Triangle(free.get(number), new int[] {-1, -1, -1});
            triangles.add(triangle);
            for (int side = 0; side < 3; side++) {
                Side key = Side.of(triangle.sideFrom(side), triangle.sideTo(side));
                int[] other = sides.remove(key);
                if (other == null) {
                    sides.put(key, new int[] {number, side});
                } else {
                    triangle.neighbours()[side] = other[0];
                    triangles.get(other[0]).neighbours()[other[1]] = number;
                }
            }
        }
        return triangles;
    }

    int size() {
        return triangles.size();
    }

    /** Whether a triangle's side lies along the window's edge: room that goes on past it, where a larger window looks. */
    static boolean alongTheEdge(Triangle triangle, int side, Envelope window) {
        Coordinate one = triangle.sideFrom(side);
        Coordinate other = triangle.sideTo(side);
        double near = EDGE_NEAR * Math.max(window.getWidth(), window.getHeight());
        return Math.abs(one.x - window.getMinX()) <= near && Math.abs(other.x - window.getMinX()) <= near
               || Math.abs(one.x - window.getMaxX()) <= near && Math.abs(other.x - window.getMaxX()) <= near
               || Math.abs(one.y - window.getMinY()) <= near && Math.abs(other.y - window.getMinY()) <= near
               || Math.abs(one.y - window.getMaxY()) <= near && Math.abs(other.y - window.getMaxY()) <= near;
    }

    Triangle triangle(int number) {
        return triangles.get(number);
    }

    /** The triangle a point is in; empty where it is in none — outside the window, or in an obstacle. */
    Optional<Integer> triangleAt(Coordinate point) {
        return Arrays.stream(index.near(Box.point(point.x, point.y))).boxed().filter(number -> triangles.get(number).holds(point)).findFirst();
    }
}
