package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A taut path — any angle, its corners where it is pulled round what it keeps clear of — laid as tracks are drawn: each stretch
 * between two corners at the angles the reader lays at — at 45°, a run and a diagonal; at 90°, two runs —, in whichever of its two
 * postures keeps clear, the one nearer where the line ran before first; a stretch neither posture keeps clear on halved and laid half
 * by half. What a rubber band and a way searched for through the free room are both laid by.
 */
final class OctilinearLaying {

    /**
     * Whether a stretch is laid only where it keeps clear, or wherever its posture puts it: a band that cannot be laid clear is still
     * shown at the reader's angles — never at any other —, its fault said for the checks and the release.
     */
    enum Clearance {
        KEPT, OVERLOOKED
    }

    private OctilinearLaying() {
    }

    /**
     * A path laid stretch by stretch at an angle step: its corners settled to the precision kept, away from what they are pulled
     * round, each stretch in the posture that keeps clear — halved where neither does —, and steps nobody can mean merged away.
     * Empty where a stretch could not be laid clear at all.
     *
     * @param coming the way the line comes into the path's start; empty where it starts there
     * @param ran    the line as it ran before, whose postures are kept where they serve; the path itself where there was none
     */
    static Optional<List<Point>> laid(List<Point> taut, Optional<Point> coming, List<Point> ran, TrackFence fence, double angleStep,
                                      RoutingSettings.Routing settings, Clearance clearance) {
        List<Point> path = settledAway(taut, fence);
        List<Point> laid = new ArrayList<>(List.of(path.getFirst()));
        for (int corner = 1; corner < path.size(); corner++) {
            Point from = laid.getLast();
            Point to = path.get(corner);
            Optional<Point> into = laid.size() >= 2 ? Lines.wayBetween(laid.get(laid.size() - 2), from) : coming;
            Optional<List<Point>> stretch = laidStretch(from, to, into, ran, fence, clearance, angleStep, fence.width() * settings.leastStep());
            if (stretch.isEmpty()) {
                return Optional.empty();
            }
            laid.addAll(stretch.get());
        }
        // A step nobody can mean — two stretches' ends settled a hair apart — goes, as it goes from every line.
        return Optional.of(new Tightening(fence, angleStep, List.of(), settings).withoutShortJogs(laid, true));
    }

    /**
     * A stretch between two corners of a band laid at the reader's angles: in one of its postures where one keeps clear; else halved
     * where the band runs — the straight way between two of its corners is clear — and each half laid so, until a half is shorter
     * than any step anyone can mean ({@code shortest}). Empty where even that does not keep clear.
     */
    private static Optional<List<Point>> laidStretch(Point from, Point to, Optional<Point> coming, List<Point> ran, TrackFence fence,
                                                     Clearance clearance, double angleStep, double shortest) {
        Optional<List<Point>> postured = postured(from, to, coming, ran, fence, clearance, angleStep);
        if (postured.isPresent() || Lines.lengthBetween(from, to) < shortest) {
            return postured;
        }
        Point middle = Lines.settled(from.halfwayTo(to));
        Optional<List<Point>> first = laidStretch(from, middle, coming, ran, fence, clearance, angleStep, shortest);
        if (first.isEmpty()) {
            return Optional.empty();
        }
        List<Point> half = Lines.joined(List.of(from), first.get());
        Optional<Point> onward = Lines.wayBetween(half.get(half.size() - 2), half.getLast());
        return laidStretch(middle, to, onward, ran, fence, clearance, angleStep, shortest).map(second -> Lines.joined(first.get(), second));
    }

    /**
     * A band's path settled to the precision kept: its ends to the nearest; each corner it is pulled round to a kept point round it
     * that keeps clear, away from what it is pulled round first — what a corner is pulled round stands inside its turn, on the side its
     * two stretches' ways out of it lean to together —. In a way exactly as wide as the rule allows, away from one side may be into
     * the other: then whichever kept point keeps clear of both.
     */
    private static List<Point> settledAway(List<Point> path, TrackFence fence) {
        List<Point> settled = new ArrayList<>();
        for (int corner = 0; corner < path.size(); corner++) {
            boolean end = corner == 0 || corner == path.size() - 1;
            if (end) {
                settled.add(Lines.settled(path.get(corner)));
                continue;
            }
            Point at = path.get(corner);
            Optional<Point> back = Lines.wayBetween(at, path.get(corner - 1));
            Optional<Point> on = Lines.wayBetween(at, path.get(corner + 1));
            Point away = back.isPresent() && on.isPresent()
                    ? new Point(-back.get().x() - on.get().x(), -back.get().y() - on.get().y()) : new Point(0, 0);
            Point preferred = Lines.settledTowards(at, away);
            Point kept = fence.pointClear(preferred) ? preferred : Lines.keptPointsAround(at).stream()
                    .filter(fence::pointClear)
                    .min(Comparator.comparingDouble(candidate -> Lines.lengthBetween(candidate, preferred)))
                    .orElse(preferred);
            settled.add(kept);
        }
        return settled;
    }

    /**
     * A stretch in one of its postures that keeps clear — the points after its start —: first one that does not break where the line
     * comes into it — a right angle or sharper, as KiCad weighs a corner past an obtuse one —; then the one that runs the way the line
     * ran before the drag at more of its two ends, so a line keeps its posture as the part goes rather than flipping between two of
     * equal length; then the one that runs nearer the line as it ran, by its corner and the middles of its two runs. Empty where
     * neither keeps clear.
     *
     * @param coming the way the line comes into the stretch's start; empty where it starts there
     */
    static Optional<List<Point>> postured(Point from, Point to, Optional<Point> coming, List<Point> ran, TrackFence fence,
                                                  Clearance clearance, double angleStep) {
        List<List<Point>> ordered = Lines.posturesOf(from, to, angleStep).stream()
                .sorted(Comparator.comparing((List<Point> posture) -> breaksInto(coming, from, posture))
                        .thenComparingInt(posture -> -endsAsItRan(ran, from, posture))
                        .thenComparingDouble(posture -> awayFrom(ran, from, posture)))
                .toList();
        for (List<Point> posture : ordered) {
            if (clearance == Clearance.OVERLOOKED || fence.lineClear(Lines.joined(List.of(from), posture))) {
                return Optional.of(posture);
            }
        }
        return Optional.empty();
    }

    /** Whether the line breaks where it comes into a posture's first run — a right angle or sharper —; never where it starts there. */
    private static boolean breaksInto(Optional<Point> coming, Point from, List<Point> posture) {
        Optional<Point> setting = Lines.wayBetween(from, posture.getFirst());
        return coming.isPresent() && setting.isPresent() && Lines.dot(coming.get(), setting.get()) <= Lines.SAME_POINT;
    }

    /** The way the line ran at its stretch nearest a point, as a unit; empty for a line of no length there. */
    private static Optional<Point> wayRanNear(List<Point> ran, Point point) {
        int nearest = 1;
        for (int corner = 2; corner < ran.size(); corner++) {
            if (Lines.distanceToStretch(point, ran.get(corner - 1), ran.get(corner))
                < Lines.distanceToStretch(point, ran.get(nearest - 1), ran.get(nearest))) {
                nearest = corner;
            }
        }
        return ran.size() < 2 ? Optional.empty() : Lines.wayBetween(ran.get(nearest - 1), ran.get(nearest));
    }

    /** At how many of its two ends a posture runs the way the line ran there: its first run where it starts, its last where it ends. */
    private static int endsAsItRan(List<Point> ran, Point from, List<Point> posture) {
        Point to = posture.getLast();
        Point beforeEnd = posture.size() >= 2 ? posture.get(posture.size() - 2) : from;
        return (runsAs(wayRanNear(ran, from), from, posture.getFirst()) ? 1 : 0) + (runsAs(wayRanNear(ran, to), beforeEnd, to) ? 1 : 0);
    }

    /** Whether the run between two points goes the way the line ran, either way along it. */
    private static boolean runsAs(Optional<Point> wayRan, Point from, Point to) {
        return wayRan.isPresent() && Lines.wayBetween(from, to)
                .filter(way -> Lines.samePoint(way, wayRan.get()) || Lines.samePoint(way, new Point(-wayRan.get().x(), -wayRan.get().y())))
                .isPresent();
    }

    /** How far a posture runs from a line: its corner's distance and its two runs' middles' added up. */
    private static double awayFrom(List<Point> ran, Point from, List<Point> posture) {
        if (posture.size() < 2) {
            return 0;
        }
        Point corner = posture.getFirst();
        Point to = posture.getLast();
        Point firstMiddle = from.halfwayTo(corner);
        Point secondMiddle = corner.halfwayTo(to);
        return distanceTo(ran, corner) + distanceTo(ran, firstMiddle) + distanceTo(ran, secondMiddle);
    }

    /** How far a point stands from a line. */
    private static double distanceTo(List<Point> line, Point point) {
        double nearest = Double.POSITIVE_INFINITY;
        for (int corner = 1; corner < line.size(); corner++) {
            nearest = Math.min(nearest, Lines.distanceToStretch(point, line.get(corner - 1), line.get(corner)));
        }
        return line.size() == 1 ? Lines.lengthBetween(point, line.getFirst()) : nearest;
    }

}
