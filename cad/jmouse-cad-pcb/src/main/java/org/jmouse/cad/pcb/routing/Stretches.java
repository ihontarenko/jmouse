package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.geometry.Segments;

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
        return from.toward(to, Segments.shareNearest(from.x(), from.y(), to.x(), to.y(), point.x(), point.y()));
    }

    /** How far a point stands from the stretch between two others. */
    public static double distanceToStretch(Point point, Point from, Point to) {
        return point.distanceTo(nearestOnStretch(point, from, to));
    }
}
