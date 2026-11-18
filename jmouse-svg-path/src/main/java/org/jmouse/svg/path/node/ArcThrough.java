package org.jmouse.svg.path.node;

import java.util.Optional;

/**
 * The arc of a circle from a start through a middle point to an end, as a drawing writes it with three points — KiCad's arcs —: its
 * circle ({@link CircularArc}), the angle it starts at and the angle it sweeps, the way round that passes the middle point. What its
 * length is, and the points it passes.
 *
 * @param circle     the circle it is an arc of
 * @param startAngle the angle of its start about the centre, radians
 * @param sweep      the angle it sweeps from its start to its end, radians: positive the way the angle grows
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ArcThrough(CircularArc circle, double startAngle, double sweep) {

    private static final double FULL_TURN = 2 * Math.PI;

    /** The arc through three points; empty when they lie on a line. */
    public static Optional<ArcThrough> of(double startX, double startY, double middleX, double middleY, double endX, double endY) {
        return CircularArc.through(startX, startY, middleX, middleY, endX, endY).map(circle -> {
            double start = Math.atan2(startY - circle.centerY(), startX - circle.centerX());
            double middle = Math.atan2(middleY - circle.centerY(), middleX - circle.centerX());
            double end = Math.atan2(endY - circle.centerY(), endX - circle.centerX());
            double sweep = turned(start, end);
            // The way round that passes the middle point: the other way when going the short way would miss it.
            if (turned(start, middle) > sweep) {
                sweep -= FULL_TURN;
            }
            return new ArcThrough(circle, start, sweep);
        });
    }

    /** How long it is along the circle. */
    public double length() {
        return circle.radius() * Math.abs(sweep);
    }

    /** Its point at a step of so many even steps along it: step 0 its start, the last its end — {x, y}. */
    public double[] pointAt(int step, int steps) {
        double angle = startAngle + sweep * step / steps;
        return new double[] {circle.centerX() + circle.radius() * Math.cos(angle), circle.centerY() + circle.radius() * Math.sin(angle)};
    }

    /** How far round from one angle another is, the way the angle grows, in [0, 2π). */
    private static double turned(double from, double to) {
        double turn = (to - from) % FULL_TURN;
        return turn < 0 ? turn + FULL_TURN : turn;
    }
}
