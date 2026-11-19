package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Two tracks of one net drawn along each other — on one layer, nearer than a gap — are one conductor drawn twice: the chosen one
 * runs into the other's line and ends there, a junction, and its own copper along it is gone. Where it leaves the other line again
 * it goes on from there, a track of its own. What lies along is said by the geometry alone: the part of a stretch both ends of which
 * stand within the gap of the other track's stretch.
 *
 * @param gap how far apart two lines may run and still be one, millimetres
 */
record CoincidentStretches(RoutingWorld world, double gap) {

    /** A stretch of another track of the net. */
    private record Stretch(Point from, Point to) {
    }

    /** Where along a track's line it runs along other copper: from and to, as lengths along the line. */
    private record Along(double from, double to) {
    }

    /** The pcb with each chosen track merged into the copper of its net it runs along; a held track, or one along none, stays. */
    PcbDrawing merged(PcbDrawing pcb, List<Integer> chosen) {
        List<Track> tracks = new ArrayList<>(pcb.tracks());
        List<Track> added = new ArrayList<>();
        // The last chosen first: a track taken away moves none of those still to come.
        for (int index : chosen.stream().sorted(Comparator.reverseOrder()).toList()) {
            if (index < 0 || index >= tracks.size() || RoutingEngine.held(pcb, index)) {
                continue;
            }
            Optional<List<Track>> pieces = piecesOf(pcb, index);
            if (pieces.isEmpty()) {
                continue;
            }
            if (pieces.get().isEmpty()) {
                tracks.remove(index);
            } else {
                tracks.set(index, pieces.get().getFirst());
                added.addAll(pieces.get().subList(1, pieces.get().size()));
            }
        }
        tracks.addAll(added);
        return pcb.withTracks(tracks);
    }

    /** What is left of a track once what runs along its net's other copper is gone, in its order; empty where nothing runs along. */
    private Optional<List<Track>> piecesOf(PcbDrawing pcb, int index) {
        Track track = pcb.tracks().get(index);
        Optional<String> net = world.netOfTrack(index);
        if (net.isEmpty()) {
            return Optional.empty();
        }
        List<Stretch> others = othersOfNet(pcb, index, track.layer(), net.get());
        List<Point> line = track.points();
        List<Along> alongs = new ArrayList<>();
        double walked = 0;
        for (int at = 1; at < line.size(); at++) {
            Point from = line.get(at - 1);
            Point to = line.get(at);
            double length = from.distanceTo(to);
            for (Stretch other : others) {
                Optional<double[]> shares = alongOf(from, to, other);
                if (shares.isPresent()) {
                    alongs.add(new Along(walked + shares.get()[0] * length, walked + shares.get()[1] * length));
                }
            }
            walked += length;
        }
        if (alongs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(piecesOutside(track, joined(alongs), walked, others));
    }

    /** The stretches of every other track of the net on the layer. */
    private List<Stretch> othersOfNet(PcbDrawing pcb, int index, String layer, String net) {
        List<Stretch> others = new ArrayList<>();
        for (int other = 0; other < pcb.tracks().size(); other++) {
            Track candidate = pcb.tracks().get(other);
            if (other == index || !candidate.layer().equals(layer) || !world.netOfTrack(other).equals(Optional.of(net))) {
                continue;
            }
            for (int at = 1; at < candidate.points().size(); at++) {
                others.add(new Stretch(candidate.points().get(at - 1), candidate.points().get(at)));
            }
        }
        return others;
    }

    /**
     * The part of a stretch that runs along another, as shares of the stretch from its start: the other's ends projected on it,
     * kept where both ends of what is between stand within the gap of the other, and it is longer than the gap. None where nothing
     * runs along.
     */
    private Optional<double[]> alongOf(Point from, Point to, Stretch other) {
        double length = from.distanceTo(to);
        if (length == 0) {
            return Optional.empty();
        }
        double first = clamped(shareOn(other.from(), from, to));
        double last = clamped(shareOn(other.to(), from, to));
        double start = Math.min(first, last);
        double end = Math.max(first, last);
        if ((end - start) * length <= gap) {
            return Optional.empty();
        }
        if (distanceTo(from.toward(to, start), other) > gap || distanceTo(from.toward(to, end), other) > gap) {
            return Optional.empty();
        }
        return Optional.of(new double[] {start, end});
    }

    /** The lengths along the line that run along other copper, overlapping ones made one, in order. */
    private static List<Along> joined(List<Along> alongs) {
        List<Along> sorted = alongs.stream().sorted(Comparator.comparingDouble(Along::from)).toList();
        List<Along> joined = new ArrayList<>();
        for (Along along : sorted) {
            if (!joined.isEmpty() && along.from() <= joined.getLast().to()) {
                Along last = joined.removeLast();
                joined.add(new Along(last.from(), Math.max(last.to(), along.to())));
            } else {
                joined.add(along);
            }
        }
        return joined;
    }

    /**
     * The track's pieces between what runs along: each its own track, its end at a run moved onto the other copper's line — the
     * junction. A piece no longer than the gap is none.
     */
    private List<Track> piecesOutside(Track track, List<Along> alongs, double whole, List<Stretch> others) {
        List<Track> pieces = new ArrayList<>();
        double start = 0;
        for (Along along : alongs) {
            pieceBetween(track, start, along.from(), start > 0, true, others).ifPresent(pieces::add);
            start = along.to();
        }
        pieceBetween(track, start, whole, start > 0, false, others).ifPresent(pieces::add);
        return pieces;
    }

    private Optional<Track> pieceBetween(Track track, double from, double to, boolean startJoins, boolean endJoins, List<Stretch> others) {
        if (to - from <= gap) {
            return Optional.empty();
        }
        List<Point> points = new ArrayList<>(Polylines.between(track.points(), from, to));
        if (startJoins) {
            points.set(0, onNearest(points.getFirst(), others));
        }
        if (endJoins) {
            points.set(points.size() - 1, onNearest(points.getLast(), others));
        }
        return Optional.of(track.withPoints(LineCleaning.withoutStraightPoints(points)));
    }

    // ── Geometry ─────────────────────────────────────────────────────────────────────────────────────────────

    /** The point of the nearest other stretch nearest to a point: where a piece ends on the copper it runs into. */
    private static Point onNearest(Point point, List<Stretch> others) {
        return others.stream().map(other -> nearestOn(point, other))
                .min(Comparator.comparingDouble(point::distanceTo)).map(Precision::settled).orElse(point);
    }

    private static Point nearestOn(Point point, Stretch stretch) {
        return stretch.from().toward(stretch.to(), clamped(shareOn(point, stretch.from(), stretch.to())));
    }

    private static double distanceTo(Point point, Stretch stretch) {
        return point.distanceTo(nearestOn(point, stretch));
    }

    /** How far along a stretch a point stands square to it, as a share of the stretch; outside 0 … 1 past its ends. */
    private static double shareOn(Point point, Point from, Point to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double squared = dx * dx + dy * dy;
        return squared == 0 ? 0 : ((point.x() - from.x()) * dx + (point.y() - from.y()) * dy) / squared;
    }

    private static double clamped(double share) {
        return Math.max(0, Math.min(1, share));
    }
}
