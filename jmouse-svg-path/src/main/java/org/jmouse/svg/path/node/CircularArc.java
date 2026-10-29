package org.jmouse.svg.path.node;

import java.util.Optional;

/**
 * The arc of a circle from a start through a middle to an end, as path data says it: its radius, whether it is the
 * longer of the two arcs, and which way it turns. What a drawing written as three points — KiCad's arcs — is drawn with.
 *
 * @param largeArc whether it spans more than half the circle
 * @param sweep    whether it turns the way the angle grows: clockwise on a screen whose Y points down
 */
public record CircularArc(double centerX, double centerY, double radius, boolean largeArc, boolean sweep) {

    /** How near three points may be to a line and still be one: then no circle passes through them. */
    private static final double COLLINEAR = 1e-9;

    /** The arc through the three points; empty when they lie on a line. */
    public static Optional<CircularArc> through(double startX, double startY, double middleX, double middleY, double endX,
                                                double endY) {
        double determinant = 2 * (startX * (middleY - endY) + middleX * (endY - startY) + endX * (startY - middleY));
        if (Math.abs(determinant) < COLLINEAR) {
            return Optional.empty();
        }
        double startSquare = startX * startX + startY * startY;
        double middleSquare = middleX * middleX + middleY * middleY;
        double endSquare = endX * endX + endY * endY;
        double centerX = (startSquare * (middleY - endY) + middleSquare * (endY - startY) + endSquare * (startY - middleY))
                / determinant;
        double centerY = (startSquare * (endX - middleX) + middleSquare * (startX - endX) + endSquare * (middleX - startX))
                / determinant;
        double radius = Math.hypot(startX - centerX, startY - centerY);
        double turn = (middleX - startX) * (endY - startY) - (middleY - startY) * (endX - startX);
        boolean sweep = turn > 0;
        double from = Math.atan2(startY - centerY, startX - centerX);
        double to = Math.atan2(endY - centerY, endX - centerX);
        double span = sweep ? to - from : from - to;
        span = ((span % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        return Optional.of(new CircularArc(centerX, centerY, radius, span > Math.PI, sweep));
    }
}
