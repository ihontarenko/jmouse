package org.jmouse.geometry;

/**
 * Straight segments, each from one point to another, and where along them other things come nearest: a share of the length, 0 at its
 * start, 1 at its end.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Segments {

    /** How near a cross product of two segments may come to 0 and they still be taken to run side by side. */
    private static final double SIDE_BY_SIDE = 1e-12;

    /** The shares along two segments of the points where they come nearest each other. */
    public record Shares(double first, double second) {
    }

    private Segments() {
    }

    /** The share along a segment of its point nearest a point; 0 for a segment of no length. */
    public static double shareNearest(double fromX, double fromY, double toX, double toY, double x, double y) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double squared = dx * dx + dy * dy;
        if (squared == 0) {
            return 0;
        }
        return clamped(((x - fromX) * dx + (y - fromY) * dy) / squared);
    }

    /**
     * The shares along two segments of their nearest points: the closest points of the two. Segments that run side by side come
     * nearest first at the first's start.
     */
    public static Shares nearestShares(double firstFromX, double firstFromY, double firstToX, double firstToY,
                                       double secondFromX, double secondFromY, double secondToX, double secondToY) {
        double firstX = firstToX - firstFromX;
        double firstY = firstToY - firstFromY;
        double secondX = secondToX - secondFromX;
        double secondY = secondToY - secondFromY;
        double apartX = firstFromX - secondFromX;
        double apartY = firstFromY - secondFromY;
        double firstSquared = firstX * firstX + firstY * firstY;
        double together = firstX * secondX + firstY * secondY;
        double secondSquared = secondX * secondX + secondY * secondY;
        double firstApart = firstX * apartX + firstY * apartY;
        double secondApart = secondX * apartX + secondY * apartY;
        if (firstSquared == 0 && secondSquared == 0) {
            return new Shares(0, 0);
        }
        if (firstSquared == 0) {
            return new Shares(0, clamped(secondApart / secondSquared));
        }
        if (secondSquared == 0) {
            return new Shares(clamped(-firstApart / firstSquared), 0);
        }
        double denominator = firstSquared * secondSquared - together * together;
        double first = denominator > SIDE_BY_SIDE ? clamped((together * secondApart - secondSquared * firstApart) / denominator) : 0;
        double second = (together * first + secondApart) / secondSquared;
        if (second < 0) {
            return new Shares(clamped(-firstApart / firstSquared), 0);
        }
        if (second > 1) {
            return new Shares(clamped((together - firstApart) / firstSquared), 1);
        }
        return new Shares(first, second);
    }

    private static double clamped(double share) {
        return Math.min(1, Math.max(0, share));
    }
}
