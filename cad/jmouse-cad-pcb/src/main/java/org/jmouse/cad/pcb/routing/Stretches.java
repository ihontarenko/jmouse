package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;

/** One straight stretch between two points: whether a point lies on it, and the point of it nearest another. */
public final class Stretches {

    /** How far off its straight stretch a point may stand and still lie on it: what a few written coordinates lose between them. */
    private static final double ON_STRETCH = 1e-3;

    private Stretches() {
    }

    /** Whether a point stands on the straight stretch between two others. */
    public static boolean liesBetween(Point point, Point from, Point to) {
        return Math.abs(from.distanceTo(point) + point.distanceTo(to) - from.distanceTo(to)) < ON_STRETCH;
    }

    /** The point of the stretch between two others nearest a point. */
    public static Point nearestOnStretch(Point point, Point from, Point to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double square = dx * dx + dy * dy;
        double share = square == 0 ? 0 : Math.max(0, Math.min(1, ((point.x() - from.x()) * dx + (point.y() - from.y()) * dy) / square));
        return from.toward(to, share);
    }

    /** How far a point stands from the stretch between two others. */
    public static double distanceToStretch(Point point, Point from, Point to) {
        return point.distanceTo(nearestOnStretch(point, from, to));
    }
}
