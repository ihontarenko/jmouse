package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import java.util.List;
import java.util.Optional;

/**
 * What makes a track look laid by a person. Two things, and which one a line gets is what keeps it steady: a line a person is
 * shaping is only cleaned ({@link #untangled}), for its way is theirs; a line the router found is pulled tight as well
 * ({@link #tightened}). A jog — a step shorter than {@code shortestRun} track widths — goes from a line the router found
 * always, and from one a person shapes only when they ask for it; a step shorter than {@code leastStep} widths, which nobody
 * can mean, goes from every line.
 *
 * @param held corners that may not go: what the reader holds, where the track was before; a line's two ends always stay
 */
public record Tightening(TrackFence fence, double angleStep, List<Point> held, RoutingSettings.Routing settings) {

    /** What becomes of a jog in a line a person shapes: kept as drawn, or merged into one corner where its gaps allow. */
    public enum JogHandling {
        KEPT, MERGED
    }

    public Tightening {
        held = List.copyOf(held);
    }

    private boolean isHeld(Point point) {
        return held.stream().anyMatch(candidate -> Precision.samePoint(candidate, point));
    }

    /**
     * Every jog of a line merged, one after another, where the stretches either side can meet and keep their gaps: a step
     * shorter than {@code shortestRun} track widths, or — {@code least} — only one shorter than {@code leastStep}.
     */
    public List<Point> withoutShortJogs(List<Point> points, boolean least) {
        List<Point> line = LineCleaning.withoutStraightPoints(points);
        for (int round = points.size(); round > 0; round--) {
            Optional<List<Point>> next = withoutShortJog(line, least);
            if (next.isEmpty()) {
                break;
            }
            line = LineCleaning.withoutStraightPoints(next.get());
        }
        return line;
    }

    /** A jog too short to be meant gone, where the stretches either side of it can meet. */
    private Optional<List<Point>> withoutShortJog(List<Point> line, boolean least) {
        double shortest = fence.width() * (least ? settings.leastStep() : settings.shortestRun());
        for (int at = 1; at + 2 < line.size(); at++) {
            Point previous = line.get(at - 1);
            Point first = line.get(at);
            Point second = line.get(at + 1);
            Point next = line.get(at + 2);
            // A point the reader holds on the step itself is theirs, and the step stays. Its corners held — the ends of a stretch the hand
            // carries — the step is one nobody means between what they carry and the line beside it: merging, what they carry lands on
            // that line, a held side kept where only one is. A step nobody can mean at all goes whatever is held.
            boolean firstHeld = !least && isHeld(first);
            boolean secondHeld = !least && isHeld(second);
            boolean stepHeld = !least && held.stream().anyMatch(point -> Stretches.liesBetween(point, first, second)
                    && !Precision.samePoint(point, first) && !Precision.samePoint(point, second));
            if (first.distanceTo(second) >= shortest || stepHeld) {
                continue;
            }
            Optional<Point> inWay = Ways.wayBetween(previous, first);
            Optional<Point> outWay = Ways.wayBetween(next, second);
            if (inWay.isEmpty() || outWay.isEmpty()) {
                continue;
            }
            // Where the runs either side turn into each other; runs a hair from one direction run on beside each other: a step out of line.
            Optional<Point> met = Ways.turnAt(first, inWay.get(), second, outWay.get());
            if (met.isPresent()) {
                Point settled = Precision.settled(met.get());
                List<Point> replaced = Polylines.joined(line.subList(0, at), List.of(settled), line.subList(at + 2, line.size()));
                if (fence.lineClear(List.of(previous, settled, next)) && !LineCleaning.isTangled(replaced)) {
                    return Optional.of(replaced);
                }
                continue;
            }
            // Two runs one direction, side by side: a step out of line. One run goes, the other going on until it meets what came after
            // the gone one: the run not held is the one that goes where one side is held; else the shorter first, then the longer where
            // the shorter cannot — its going would turn the line back on itself.
            boolean heldGives = firstHeld || secondHeld;
            List<Boolean> choices = heldGives ? List.of(secondHeld) : previous.distanceTo(first) >= second.distanceTo(next) ? List.of(true, false) : List.of(false, true);
            for (boolean firstLonger : choices) {
                Optional<List<Point>> stepped = withoutStepOutOfLine(line, at, firstLonger, least, heldGives, firstLonger ? inWay.get() : outWay.get());
                if (stepped.isPresent()) {
                    return stepped;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * A step out of line taken away: one of the two runs beside it goes — the second, `firstLonger`, else the first — and the other
     * goes on until it meets what came after the gone one; where the gone run ends the line, the step is taken to that end instead.
     */
    private Optional<List<Point>> withoutStepOutOfLine(List<Point> line, int at, boolean firstLonger, boolean least, boolean heldGives, Point way) {
        Point first = line.get(at);
        Point second = line.get(at + 1);
        int goneAt = firstLonger ? at + 2 : at - 1;
        int beyond = firstLonger ? at + 3 : at - 2;
        Point gone = line.get(goneAt);
        Point after = beyond >= 0 && beyond < line.size() ? line.get(beyond) : null;
        Point kept = firstLonger ? first : second;
        Optional<Point> onward = after != null && (least || heldGives || !isHeld(gone)) ? Ways.wayBetween(after, gone) : Optional.empty();
        Optional<Point> meeting = onward.flatMap(direction -> Ways.crossingOf(kept, way, after, direction));
        if (meeting.isPresent()) {
            Point settled = Precision.settled(meeting.get());
            List<Point> replaced = firstLonger
                    ? Polylines.joined(line.subList(0, at), List.of(settled), line.subList(beyond, line.size()))
                    : Polylines.joined(line.subList(0, beyond + 1), List.of(settled), line.subList(at + 2, line.size()));
            if (fence.lineClear(replaced) && !LineCleaning.isTangled(replaced)) {
                return Optional.of(replaced);
            }
        } else if (goneAt == 0 || goneAt == line.size() - 1) {
            // The shorter run ends the line: its end does not move, so the step cannot go. It goes to the end instead — the
            // longer run straight on into the end's copper, and the step taken there, where the pad it stands on hides it.
            Point jog = new Point(second.x() - first.x(), second.y() - first.y());
            List<Point> replaced = firstLonger
                    ? Polylines.joined(line.subList(0, at), List.of(Precision.settled(new Point(gone.x() - jog.x(), gone.y() - jog.y())), gone))
                    : Polylines.joined(List.of(gone, Precision.settled(new Point(gone.x() + jog.x(), gone.y() + jog.y()))), line.subList(at + 2, line.size()));
            if (fence.lineClear(replaced) && !LineCleaning.isTangled(replaced) && LineCleaning.withoutStraightPoints(replaced).size() < line.size()) {
                return Optional.of(replaced);
            }
        }
        return Optional.empty();
    }

    /**
     * The widest stretch of the line laid again as one step between its two ends, where that is shorter, or as short with fewer
     * corners, and keeps clear; no corner held inside it. This is what pulls a line tight: a way round what no longer stands
     * there goes, however many corners it has, and one round what still does stays.
     */
    private Optional<List<Point>> withoutDetour(List<Point> line) {
        for (int span = line.size() - 1; span >= 2; span--) {
            for (int from = 0; from + span < line.size(); from++) {
                int to = from + span;
                List<Point> inside = line.subList(from + 1, to);
                if (inside.stream().anyMatch(this::isHeld)) {
                    continue;
                }
                List<Point> direct = AngleSteps.trackStep(line.get(from), line.get(to), angleStep);
                List<Point> laid = Polylines.joined(List.of(line.get(from)), direct);
                double was = Polylines.lengthOf(line.subList(from, to + 1));
                double now = Polylines.lengthOf(laid);
                boolean shorter = now < was - settings.worthShortening();
                if ((shorter || direct.size() < inside.size() + 1) && now <= was + settings.worthShortening() && fence.lineClear(laid)) {
                    return Optional.of(Polylines.joined(line.subList(0, from + 1), direct.subList(0, direct.size() - 1), line.subList(to, line.size())));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * A line a person shaped, cleaned and no more: no loop, nothing turned back on itself, and its jogs merged where
     * {@code jogs} asks for it. Its way is theirs: what they pull changes as far as they pull it.
     */
    public List<Point> untangled(List<Point> points, JogHandling jogs) {
        List<Point> clean = LineCleaning.withoutFolds(LineCleaning.withoutLoops(LineCleaning.withoutStraightPoints(points)));
        List<Point> line = fence.lineClear(clean) ? clean : LineCleaning.withoutStraightPoints(points);
        // A step too short to be meant goes whatever the reader keeps; a jog, only when they ask for it.
        return withoutShortJogs(line, jogs != JogHandling.MERGED);
    }

    /**
     * A line the router found, pulled tight: untangled, and every detour taken out for as long as one more can be. What a search
     * leaves on its grid, and what a part's old place leaves of a way, have no reason to stay.
     */
    public List<Point> tightened(List<Point> points) {
        List<Point> line = untangled(points, JogHandling.MERGED);
        for (int round = points.size() * 2; round > 0; round--) {
            Optional<List<Point>> next = withoutDetour(line);
            if (next.isEmpty()) {
                break;
            }
            line = untangled(next.get(), JogHandling.MERGED);
        }
        return line;
    }

    /** The same tightening with other corners held. */
    public Tightening holding(List<Point> others) {
        return new Tightening(fence, angleStep, others, settings);
    }
}
