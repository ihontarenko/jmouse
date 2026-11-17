package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.geometry.taut.Obstacle;
import org.jmouse.geometry.taut.StretchHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Copper as the room a track's centre line keeps out of: each straight piece grown round by the copper's own half width, the gap the
 * track keeps and half the track's width ({@link StretchHull}), and an area of copper — a pad's — filled as well as edged. The one
 * way copper becomes polygons of the plane, for what is pulled taut among it and what is searched for through it alike; a fence
 * ({@link TrackFence}) is the same copper measured piece by piece.
 */
final class KeptOut {

    /**
     * How much nearer than the rule the room kept out may come: three quarters of the tolerance the router measures gaps with
     * ({@link TrackFence#GAP_TOLERANCE}). Copper laid to its gaps may stand that tolerance near on one side of a way as wide as the
     * rule, and the way must stay open; a line pulled tight against it keeps a quarter of the tolerance to spare.
     */
    static final double EASED = TrackFence.GAP_TOLERANCE * 3 / 4;

    /** How far outside the room kept out a line is laid round it: an eighth of the tolerance, so such a way stays open. */
    static final double SLACK = TrackFence.GAP_TOLERANCE / 8;

    private KeptOut() {
    }

    /** The straight stretches of a line, each as {@code x1, y1, x2, y2}. */
    static List<double[]> stretchesOf(List<Point> path) {
        List<double[]> stretches = new ArrayList<>();
        for (int point = 1; point < path.size(); point++) {
            Point from = path.get(point - 1);
            Point to = path.get(point);
            stretches.add(new double[] {from.x(), from.y(), to.x(), to.y()});
        }
        return stretches;
    }

    /** The pieces that come within a window, each grown round. */
    static List<Polygon> grown(List<double[]> pieces, double grow, Envelope window) {
        List<Polygon> parts = new ArrayList<>();
        for (double[] piece : pieces) {
            Coordinate from = new Coordinate(piece[0], piece[1]);
            Coordinate to = new Coordinate(piece[2], piece[3]);
            Envelope reach = new Envelope(from, to);
            reach.expandBy(grow);
            if (reach.intersects(window)) {
                parts.add(StretchHull.around(from, to, grow, Plane.FACTORY));
            }
        }
        return parts;
    }

    /**
     * A piece of copper as the room a track keeps out of, where it comes within a window: its pieces grown round, an area's inside
     * too, told apart by its first point; none where nothing of it is near.
     */
    static Optional<Obstacle> of(CopperShape shape, double grow, Envelope window) {
        List<Polygon> parts = grown(shape.pieces(), grow, window);
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        if (shape.area()) {
            insideOf(shape).ifPresent(parts::add);
        }
        double[] first = shape.pieces().getFirst();
        return Optional.of(new Obstacle(parts, new Coordinate(first[0], first[1])));
    }

    /** What an area of copper closes round, as a polygon of its pieces' starts; none where they close round nothing. */
    private static Optional<Polygon> insideOf(CopperShape shape) {
        List<Coordinate> ring = new ArrayList<>();
        for (double[] piece : shape.pieces()) {
            ring.add(new Coordinate(piece[0], piece[1]));
        }
        if (ring.size() < 3) {
            return Optional.empty();
        }
        ring.add(ring.getFirst());
        Polygon inside = Plane.FACTORY.createPolygon(ring.toArray(Coordinate[]::new));
        return inside.isValid() && !inside.isEmpty() ? Optional.of(inside) : Optional.empty();
    }
}
