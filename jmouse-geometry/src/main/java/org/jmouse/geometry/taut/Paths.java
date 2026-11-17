package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.List;

/** Measures of a path as a list of points: where a place along it stands, how long it is, the box it covers. */
final class Paths {

    private Paths() {
    }

    /** The point a place along a path stands at: its whole part the stretch, the rest how far along that stretch. */
    static Coordinate pointAt(List<Coordinate> path, double along) {
        int stretch = Math.min((int) Math.floor(along), path.size() - 2);
        double share = along - stretch;
        Coordinate from = path.get(stretch);
        Coordinate to = path.get(stretch + 1);
        return new Coordinate(from.x + (to.x - from.x) * share, from.y + (to.y - from.y) * share);
    }

    /** How far along a stretch a point of it stands, as a share of the stretch. */
    static double shareAlong(Coordinate from, Coordinate to, Coordinate at) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double square = dx * dx + dy * dy;
        if (square == 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, ((at.x - from.x) * dx + (at.y - from.y) * dy) / square));
    }

    static double lengthOf(List<Coordinate> path) {
        double length = 0;
        for (int point = 1; point < path.size(); point++) {
            length += path.get(point - 1).distance(path.get(point));
        }
        return length;
    }

    static Envelope envelopeOf(List<Coordinate> path) {
        Envelope envelope = new Envelope();
        path.forEach(envelope::expandToInclude);
        return envelope;
    }

    /** The path without a point standing where the one before it does. */
    static List<Coordinate> withoutRepeats(List<Coordinate> path) {
        List<Coordinate> kept = new ArrayList<>();
        for (Coordinate point : path) {
            if (kept.isEmpty() || !kept.getLast().equals2D(point)) {
                kept.add(point);
            }
        }
        return kept;
    }
}
