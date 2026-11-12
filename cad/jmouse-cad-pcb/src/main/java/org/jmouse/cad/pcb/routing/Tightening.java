package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

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
        return held.stream().anyMatch(candidate -> Lines.samePoint(candidate, point));
    }

    /**
     * Every jog of a line merged, one after another, where the stretches either side can meet and keep their gaps: a step
     * shorter than {@code shortestRun} track widths, or — {@code least} — only one shorter than {@code leastStep}.
     */
    public List<Point> withoutShortJogs(List<Point> points, boolean least) {
        List<Point> line = Lines.withoutStraightPoints(points);
        for (int round = points.size(); round > 0; round--) {
            Optional<List<Point>> next = withoutShortJog(line, least);
            if (next.isEmpty()) {
                break;
            }
            line = Lines.withoutStraightPoints(next.get());
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
            // A corner the reader holds keeps a jog of theirs; a step nobody can mean moves less than anyone could see, and goes whatever is held.
            boolean holds = !least && (isHeld(first) || isHeld(second) || held.stream().anyMatch(point -> Lines.liesBetween(point, first, second)));
            if (Lines.lengthBetween(first, second) >= shortest || holds) {
                continue;
            }
            Optional<Point> inWay = Lines.wayBetween(previous, first);
            Optional<Point> outWay = Lines.wayBetween(next, second);
            if (inWay.isEmpty() || outWay.isEmpty()) {
                continue;
            }
            Optional<Point> met = Lines.crossingOf(previous, inWay.get(), next, outWay.get());
            if (met.isPresent()) {
                Point settled = Lines.settled(met.get());
                List<Point> replaced = Lines.joined(line.subList(0, at), List.of(settled), line.subList(at + 2, line.size()));
                if (fence.lineClear(List.of(previous, settled, next)) && !Lines.isTangled(replaced)) {
                    return Optional.of(replaced);
                }
                continue;
            }
            // Two runs one direction, side by side: a step out of line. The shorter run goes, the longer going on until it meets what came after.
            boolean firstLonger = Lines.lengthBetween(previous, first) >= Lines.lengthBetween(second, next);
            int goneAt = firstLonger ? at + 2 : at - 1;
            int beyond = firstLonger ? at + 3 : at - 2;
            Point gone = line.get(goneAt);
            Point after = beyond >= 0 && beyond < line.size() ? line.get(beyond) : null;
            Point kept = firstLonger ? first : second;
            Point way = firstLonger ? inWay.get() : outWay.get();
            Optional<Point> onward = after != null && (least || !isHeld(gone)) ? Lines.wayBetween(after, gone) : Optional.empty();
            Optional<Point> meeting = onward.flatMap(direction -> Lines.crossingOf(kept, way, after, direction));
            if (meeting.isPresent()) {
                Point settled = Lines.settled(meeting.get());
                List<Point> replaced = firstLonger
                        ? Lines.joined(line.subList(0, at), List.of(settled), line.subList(beyond, line.size()))
                        : Lines.joined(line.subList(0, beyond + 1), List.of(settled), line.subList(at + 2, line.size()));
                if (fence.lineClear(replaced) && !Lines.isTangled(replaced)) {
                    return Optional.of(replaced);
                }
            } else if (goneAt == 0 || goneAt == line.size() - 1) {
                // The shorter run ends the line: its end does not move, so the step cannot go. It goes to the end instead — the
                // longer run straight on into the end's copper, and the step taken there, where the pad it stands on hides it.
                Point jog = new Point(second.x() - first.x(), second.y() - first.y());
                List<Point> replaced = firstLonger
                        ? Lines.joined(line.subList(0, at), List.of(Lines.settled(new Point(gone.x() - jog.x(), gone.y() - jog.y())), gone))
                        : Lines.joined(List.of(gone, Lines.settled(new Point(gone.x() + jog.x(), gone.y() + jog.y()))), line.subList(at + 2, line.size()));
                if (fence.lineClear(replaced) && !Lines.isTangled(replaced) && Lines.withoutStraightPoints(replaced).size() < line.size()) {
                    return Optional.of(replaced);
                }
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
                List<Point> direct = Lines.trackStep(line.get(from), line.get(to), angleStep);
                List<Point> laid = Lines.joined(List.of(line.get(from)), direct);
                double was = Lines.lengthOf(line.subList(from, to + 1));
                double now = Lines.lengthOf(laid);
                boolean shorter = now < was - settings.worthShortening();
                if ((shorter || direct.size() < inside.size() + 1) && now <= was + settings.worthShortening() && fence.lineClear(laid)) {
                    return Optional.of(Lines.joined(line.subList(0, from + 1), direct.subList(0, direct.size() - 1), line.subList(to, line.size())));
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
        List<Point> clean = Lines.withoutFolds(Lines.withoutLoops(Lines.withoutStraightPoints(points)));
        List<Point> line = fence.lineClear(clean) ? clean : Lines.withoutStraightPoints(points);
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

    /** The same tightening against another fence. */
    public Tightening against(TrackFence other) {
        return new Tightening(other, angleStep, held, settings);
    }
}
