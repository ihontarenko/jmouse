package org.jmouse.geometry.taut;

import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.RayCrossingCounter;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A path that runs into obstacles laid round them instead: each stretch through a piece of the slack replaced by the way round
 * that piece's edge between where it went in and where it came out — on the side whose loop with the stretch holds no obstacle's
 * inside. An obstacle that moved onto a path a little way pushes it ahead: the path passes on the side it was on, never through
 * to the other. Where both ways round hold an inside, the path is squeezed between obstacles and cannot be laid there.
 */
final class Detours {

    /** Where a path crosses a piece's edge: how far along the path, the point, which ring of the piece, and how far round it. */
    private record Crossing(double along, Coordinate at, int ring, double round) {
    }

    private Detours() {
    }

    /** The path laid round every piece of the field's slack it runs into; empty where it is squeezed or one of its ends is inside. */
    static Optional<List<Coordinate>> around(List<Coordinate> path, ObstacleField field) {
        List<Coordinate> current = path;
        for (Polygon piece : field.slackPieces()) {
            Optional<List<Coordinate>> round = aroundPiece(current, piece, field.insides());
            if (round.isEmpty()) {
                return Optional.empty();
            }
            current = round.get();
        }
        return Optional.of(current);
    }

    private static Optional<List<Coordinate>> aroundPiece(List<Coordinate> path, Polygon piece, List<Coordinate> insides) {
        if (!piece.getEnvelopeInternal().intersects(Paths.envelopeOf(path))) {
            return Optional.of(path);
        }
        List<Coordinate[]> rings = ringsOf(piece);
        List<Crossing> crossings = crossingsOf(path, rings);
        if (crossings.isEmpty()) {
            return piece.covers(piece.getFactory().createPoint(path.getFirst())) ? Optional.empty() : Optional.of(path);
        }
        PreparedGeometry prepared = PreparedGeometryFactory.prepare(piece);
        List<Coordinate> laid = new ArrayList<>();
        double last = path.size() - 1;
        Crossing entry = null;
        int copied = 0;
        List<Double> bounds = new ArrayList<>();
        bounds.add(0.0);
        crossings.forEach(crossing -> bounds.add(crossing.along()));
        bounds.add(last);
        for (int span = 0; span + 1 < bounds.size(); span++) {
            double start = bounds.get(span);
            double end = bounds.get(span + 1);
            if (end - start <= 0) {
                continue;
            }
            boolean inside = prepared.contains(piece.getFactory().createPoint(Paths.pointAt(path, (start + end) / 2)));
            if (inside && entry == null) {
                if (span == 0) {
                    return Optional.empty();
                }
                entry = crossings.get(span - 1);
            }
            if (!inside && entry != null) {
                Crossing exit = crossings.get(span - 1);
                Optional<List<Coordinate>> round = wayRound(path, entry, exit, rings, insides);
                if (round.isEmpty()) {
                    return Optional.empty();
                }
                copied = copyUpTo(path, laid, copied, entry.along());
                laid.addAll(round.get());
                copied = firstAfter(exit.along());
                entry = null;
            }
        }
        if (entry != null) {
            return Optional.empty();
        }
        copyUpTo(path, laid, copied, last + 1);
        return Optional.of(Paths.withoutRepeats(laid));
    }

    /** The path's points from {@code copied} up to, not at, a place along it, added; the place of the next point to copy. */
    private static int copyUpTo(List<Coordinate> path, List<Coordinate> laid, int copied, double along) {
        int point = copied;
        while (point < path.size() && point < along) {
            laid.add(path.get(point));
            point++;
        }
        return point;
    }

    private static int firstAfter(double along) {
        return (int) Math.floor(along) + 1;
    }

    /**
     * The way round a piece's ring from where a stretch went in to where it came out, on the side whose loop with the stretch holds
     * no inside — the shorter where neither does —; empty where both do, or the stretch came out through another ring.
     */
    private static Optional<List<Coordinate>> wayRound(List<Coordinate> path, Crossing entry, Crossing exit, List<Coordinate[]> rings,
                                                       List<Coordinate> insides) {
        if (entry.ring() != exit.ring()) {
            return Optional.empty();
        }
        Coordinate[] ring = rings.get(entry.ring());
        List<Coordinate> through = new ArrayList<>();
        through.add(entry.at());
        for (int point = firstAfter(entry.along()); point < exit.along(); point++) {
            through.add(path.get(point));
        }
        through.add(exit.at());
        List<Coordinate> onward = Rings.arc(ring, entry.round(), exit.round());
        List<Coordinate> backward = Rings.arc(ring, exit.round(), entry.round()).reversed();
        int onwardHolds = insidesWithin(through, onward, insides);
        int backwardHolds = insidesWithin(through, backward, insides);
        if (onwardHolds > 0 && backwardHolds > 0) {
            return Optional.empty();
        }
        if (onwardHolds == 0 && backwardHolds == 0) {
            return Optional.of(Paths.lengthOf(onward) <= Paths.lengthOf(backward) ? onward : backward);
        }
        return Optional.of(onwardHolds == 0 ? onward : backward);
    }

    /** How many insides the loop of a stretch and a way round back to its start holds. */
    private static int insidesWithin(List<Coordinate> through, List<Coordinate> round, List<Coordinate> insides) {
        List<Coordinate> loop = new ArrayList<>(through);
        loop.addAll(round.reversed().subList(1, round.size()));
        if (loop.size() < 4) {
            return 0;
        }
        Coordinate[] closed = loop.toArray(Coordinate[]::new);
        closed[closed.length - 1] = closed[0];
        int held = 0;
        for (Coordinate inside : insides) {
            if (RayCrossingCounter.locatePointInRing(inside, closed) == Location.INTERIOR) {
                held++;
            }
        }
        return held;
    }

    private static List<Coordinate[]> ringsOf(Polygon piece) {
        List<Coordinate[]> rings = new ArrayList<>();
        rings.add(piece.getExteriorRing().getCoordinates());
        for (int hole = 0; hole < piece.getNumInteriorRing(); hole++) {
            rings.add(piece.getInteriorRingN(hole).getCoordinates());
        }
        return rings;
    }

    /** Every place the path crosses the rings, in its order. */
    private static List<Crossing> crossingsOf(List<Coordinate> path, List<Coordinate[]> rings) {
        LineIntersector intersector = new RobustLineIntersector();
        List<Crossing> crossings = new ArrayList<>();
        for (int stretch = 0; stretch + 1 < path.size(); stretch++) {
            Coordinate from = path.get(stretch);
            Coordinate to = path.get(stretch + 1);
            for (int number = 0; number < rings.size(); number++) {
                Coordinate[] ring = rings.get(number);
                for (int side = 0; side + 1 < ring.length; side++) {
                    intersector.computeIntersection(from, to, ring[side], ring[side + 1]);
                    for (int found = 0; found < intersector.getIntersectionNum(); found++) {
                        Coordinate at = intersector.getIntersection(found);
                        crossings.add(new Crossing(stretch + Paths.shareAlong(from, to, at), at, number,
                                                   side + Paths.shareAlong(ring[side], ring[side + 1], at)));
                    }
                }
            }
        }
        crossings.sort(Comparator.comparingDouble(Crossing::along));
        return crossings;
    }
}
