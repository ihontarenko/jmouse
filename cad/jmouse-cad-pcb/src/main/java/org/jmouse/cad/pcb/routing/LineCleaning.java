package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A line made clean: its straight points left out, its folds sharper than a right angle opened, its loops cut away. */
public final class LineCleaning {

    /** How far past a right angle a corner may seem to turn and still be one: what points written to four places lose over a short stretch. */
    private static final double SQUARE = 1e-2;

    private LineCleaning() {
    }

    /** Points with the ones that stand on a straight run between their neighbours, or on their neighbour, left out. */
    public static List<Point> withoutStraightPoints(List<Point> points) {
        List<Point> kept = new ArrayList<>();
        for (Point point : points) {
            if (kept.isEmpty() || !Precision.samePoint(kept.getLast(), point)) {
                kept.add(point);
            }
        }
        return Polylines.cornersOf(kept);
    }

    /** The turn at a line's inner corner: whether the stretch going on turns back sharper than a right angle. */
    private static boolean foldsAt(List<Point> line, int index) {
        if (index == 0 || index == line.size() - 1) {
            return false;
        }
        Optional<Point> coming = Ways.wayBetween(line.get(index - 1), line.get(index));
        Optional<Point> going = Ways.wayBetween(line.get(index), line.get(index + 1));
        return coming.isPresent() && going.isPresent() && coming.get().dot(going.get()) < -SQUARE;
    }

    /**
     * A line with no corner sharper than a right angle: what a neighbour's far corner does when what it leads to has been
     * carried past it. The stretch before stops where one straight run at a step's angle, turning no sharper, reaches the
     * corner after: the farthest such point along it, so the line is changed as little as it can be. Where there is none, the
     * two are joined by a plain step.
     */
    public static List<Point> withoutFolds(List<Point> points) {
        List<Point> line = withoutStraightPoints(points);
        for (int round = points.size() * 2; round > 0; round--) {
            int at = -1;
            for (int index = 0; index < line.size(); index++) {
                if (foldsAt(line, index)) {
                    at = index;
                    break;
                }
            }
            if (at < 0) {
                break;
            }
            Point previous = line.get(at - 1);
            Point corner = line.get(at);
            Point next = line.get(at + 1);
            Point coming = Ways.wayBetween(previous, corner).orElseThrow();
            Point best = null;
            double bestReach = 0;
            for (Point way : Ways.OCTILINEAR_WAYS) {
                Optional<Point> met = Ways.crossingOf(previous, coming, next, way);
                if (met.isEmpty()) {
                    continue;
                }
                double reach = Ways.reachAlong(previous, coming, met.get());
                Optional<Point> onward = Ways.wayBetween(met.get(), next);
                if (reach > 0 && reach < previous.distanceTo(corner) && onward.isPresent() && coming.dot(onward.get()) >= -SQUARE
                    && (best == null || reach > bestReach)) {
                    best = Precision.settled(met.get());
                    bestReach = reach;
                }
            }
            List<Point> changedLine = new ArrayList<>(line.subList(0, at));
            if (best != null) {
                changedLine.add(best);
            } else {
                List<Point> step = AngleSteps.trackStep(previous, next, 45);
                changedLine.addAll(step.subList(0, step.size() - 1));
            }
            changedLine.addAll(line.subList(at + 1, line.size()));
            line = withoutStraightPoints(changedLine);
        }
        return line;
    }

    /** A line that crosses itself cut at the crossing: the loop beyond it gone. */
    public static List<Point> withoutLoops(List<Point> points) {
        List<Point> line = List.copyOf(points);
        for (int first = 1; first < line.size(); first++) {
            for (int second = line.size() - 1; second > first + 1; second--) {
                Point firstFrom = line.get(first - 1);
                Point firstTo = line.get(first);
                Point secondFrom = line.get(second - 1);
                Point secondTo = line.get(second);
                Optional<Point> away = Ways.wayBetween(firstFrom, firstTo);
                Optional<Point> onward = Ways.wayBetween(secondFrom, secondTo);
                if (away.isEmpty() || onward.isEmpty()) {
                    continue;
                }
                Optional<Point> met = Ways.crossingOf(firstFrom, away.get(), secondFrom, onward.get());
                if (met.isPresent() && Ways.reachAlong(firstFrom, away.get(), met.get()) > 0
                    && firstFrom.distanceTo(met.get()) < firstFrom.distanceTo(firstTo)
                    && Ways.reachAlong(secondFrom, onward.get(), met.get()) > 0
                    && secondFrom.distanceTo(met.get()) < secondFrom.distanceTo(secondTo)) {
                    List<Point> cut = new ArrayList<>(line.subList(0, first));
                    cut.add(Precision.settled(met.get()));
                    cut.addAll(line.subList(second, line.size()));
                    return withoutLoops(cut);
                }
            }
        }
        return line;
    }

    /** Whether a line crosses itself or turns back sharper than a right angle. */
    public static boolean isTangled(List<Point> points) {
        if (withoutLoops(points).size() != points.size()) {
            return true;
        }
        List<Point> unfolded = withoutFolds(points);
        for (int index = 0; index < unfolded.size(); index++) {
            if (index >= points.size() || !Precision.samePoint(unfolded.get(index), points.get(index))) {
                return true;
            }
        }
        return false;
    }
}
