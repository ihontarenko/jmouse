package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import java.util.List;
import java.util.Optional;

/**
 * The steps a track is laid by from one point to another at an angle step — every 90°, 45°, any other step, or any way —: the step
 * itself, the posture of it that turns least, and the step that keeps the run a corner had.
 */
public final class AngleSteps {

    /** How far off a run a point may stand and still be on it, and how near a turn may come to the anchor and still be none. */
    private static final double ON_RUN = 1e-6;

    /** How near a dot product of two unit ways may come to a bound and still be past it: what arithmetic in binary loses. */
    private static final double ARITHMETIC = 1e-9;

    private AngleSteps() {
    }

    /**
     * The points a track is carried through from {@code from} toward {@code to}, the last of them where it stops. At 90° it
     * goes across then down; at 45° straight along the longer way, then diagonally — both reach the pointer exactly, as a
     * router lays copper. At any other step the one segment is turned to the nearest allowed direction and runs as far as the
     * pointer reaches along it; with no step it goes straight there.
     */
    public static List<Point> trackStep(Point from, Point to, double step) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        if (step == 0 || (dx == 0 && dy == 0)) {
            return List.of(to);
        }
        if (step == 90) {
            return dx == 0 || dy == 0 ? List.of(to) : List.of(new Point(to.x(), from.y()), to);
        }
        if (step == 45) {
            double diagonal = Math.min(Math.abs(dx), Math.abs(dy));
            Point corner = Math.abs(dx) > Math.abs(dy)
                    ? new Point(from.x() + Math.signum(dx) * (Math.abs(dx) - diagonal), from.y())
                    : new Point(from.x(), from.y() + Math.signum(dy) * (Math.abs(dy) - diagonal));
            boolean atAnEnd = (corner.x() == from.x() && corner.y() == from.y()) || (corner.x() == to.x() && corner.y() == to.y());
            return atAnEnd ? List.of(to) : List.of(corner, to);
        }
        double radians = Math.toRadians(step);
        double direction = Math.round(Math.atan2(dy, dx) / radians) * radians;
        double along = Math.max(0, dx * Math.cos(direction) + dy * Math.sin(direction));
        return List.of(new Point(from.x() + along * Math.cos(direction), from.y() + along * Math.sin(direction)));
    }

    /**
     * A 45° step from a point to another, of its two postures — along the longer way first, or diagonally first — the one that turns
     * least from the way the line went on at {@code from}, as KiCad weighs corners: no turn costs nothing, an obtuse one less than a
     * right one, a right one less than an acute one; even, the longer way first. Any other step is {@link #trackStep}.
     *
     * @param going the way the line went on at {@code from}, as a unit
     */
    public static List<Point> trackStepGoingOn(Point going, Point from, Point to, double step) {
        List<Point> longerFirst = trackStep(from, to, step);
        if (step != 45 || longerFirst.size() < 2) {
            return longerFirst;
        }
        Point corner = longerFirst.getFirst();
        List<Point> diagonalFirst = List.of(Precision.settled(new Point(from.x() + to.x() - corner.x(), from.y() + to.y() - corner.y())), to);
        return turnsCost(going, from, diagonalFirst) < turnsCost(going, from, longerFirst) ? diagonalFirst : longerFirst;
    }

    /** What the turns of a step cost, from the way the line went on: at where it leaves, and at its corner. */
    private static double turnsCost(Point going, Point from, List<Point> step) {
        Optional<Point> first = Ways.wayBetween(from, step.getFirst());
        Optional<Point> second = Ways.wayBetween(step.getFirst(), step.getLast());
        if (first.isEmpty() || second.isEmpty()) {
            return 0;
        }
        return TurnKind.of(going, first.get()).cost() + TurnKind.of(first.get(), second.get()).cost();
    }

    /** The kinds of turn between two ways, by what each costs a step — KiCad's weights: none 0, obtuse 10, right 30, acute 50. */
    private enum TurnKind {
        NONE(0), OBTUSE(10), RIGHT(30), ACUTE(50);

        private final double cost;

        TurnKind(double cost) {
            this.cost = cost;
        }

        double cost() {
            return cost;
        }

        /** The turn from one way to another, each a unit. */
        static TurnKind of(Point coming, Point going) {
            double along = coming.dot(going);
            if (along > 1 - ARITHMETIC) {
                return NONE;
            }
            if (along > ARITHMETIC) {
                return OBTUSE;
            }
            return along > -ARITHMETIC ? RIGHT : ACUTE;
        }
    }

    /**
     * The way from a corner that stays to one carried, keeping the run it had: on along the line it ran, then in at a step's angle
     * to the corner, as copper is pulled aside rather than laid anew. So pulling a corner bends the track near it, and a long run
     * does not swing; a corner pulled out past where it stood stands out as a blunt bump, for the line goes on from it.
     */
    public static List<Point> stepKeepingRun(Point anchor, Point was, Point to, double angleStep) {
        Optional<Point> way = Ways.wayBetween(anchor, was);
        if (angleStep != 45 || way.isEmpty()) {
            return trackStep(anchor, to, angleStep);
        }
        Point along = way.get();
        double offLine = (to.x() - anchor.x()) * along.y() - (to.y() - anchor.y()) * along.x();
        if (Math.abs(offLine) < ON_RUN) {
            return List.of(to);
        }
        double side = offLine > 0 ? -1 : 1;
        double half = Math.sqrt(0.5);
        Point inward = new Point((along.x() - side * along.y()) * half, (along.y() + side * along.x()) * half);
        Optional<Point> turn = Ways.crossingOf(anchor, along, to, inward);
        if (turn.isEmpty() || Ways.reachAlong(anchor, along, turn.get()) < 0) {
            // No turn ahead on the run: a plain step, in the posture that turns least from the way the run went.
            return trackStepGoingOn(along, anchor, to, angleStep);
        }
        // Pulled out past the corner it had, the run would go by it: it turns off that much before the corner instead, and comes
        // along beside where it ran. Both sides doing so, the corner stands out as a blunt bump, and nothing crosses.
        if (anchor.distanceTo(turn.get()) >= anchor.distanceTo(was) - ON_RUN) {
            double off = Math.abs(offLine);
            Point leaving = was.moved(along, -off);
            Point beside = leaving.moved(inward, off * Math.sqrt(2));
            if (Ways.reachAlong(anchor, along, leaving) > 0) {
                return List.of(Precision.settled(leaving), Precision.settled(beside), to);
            }
        }
        return anchor.distanceTo(turn.get()) < ON_RUN ? List.of(to) : List.of(Precision.settled(turn.get()), to);
    }

    /**
     * A stretch's postures at an angle step: one straight where it already runs at an allowed angle, to the precision a point is
     * kept to — or where any angle is —, else the two ways round its corner.
     */
    public static List<List<Point>> posturesOf(Point from, Point to, double angleStep) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        // A difference finer than a point is kept to is none: the stretch already runs at the angle.
        boolean straight = Math.abs(dx) < Precision.SAME_POINT || Math.abs(dy) < Precision.SAME_POINT
                           || angleStep == 45 && Math.abs(Math.abs(dx) - Math.abs(dy)) < Precision.SAME_POINT;
        if (straight || angleStep != 45 && angleStep != 90) {
            return List.of(List.of(to));
        }
        Point first;
        Point second;
        if (angleStep == 45) {
            double diagonal = Math.min(Math.abs(dx), Math.abs(dy));
            first = Precision.settled(new Point(from.x() + Math.signum(dx) * diagonal, from.y() + Math.signum(dy) * diagonal));
            second = Precision.settled(new Point(to.x() - Math.signum(dx) * diagonal, to.y() - Math.signum(dy) * diagonal));
        } else {
            first = new Point(to.x(), from.y());
            second = new Point(from.x(), to.y());
        }
        return List.of(List.of(first, to), List.of(second, to));
    }
}
