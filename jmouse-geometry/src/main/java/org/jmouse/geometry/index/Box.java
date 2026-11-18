package org.jmouse.geometry.index;

/**
 * A box with its sides along the axes: left, top, right, bottom — y growing downwards, as a drawing's does. What an index knows of an
 * item, and what it is asked by.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record Box(double left, double top, double right, double bottom) {

    /** A box of no size at a point. */
    public static Box point(double x, double y) {
        return new Box(x, y, x, y);
    }

    /** The square reaching a distance every way from a point. */
    public static Box around(double x, double y, double reach) {
        return new Box(x - reach, y - reach, x + reach, y + reach);
    }

    /** This box grown a distance every way — shrunk where it is less than 0. */
    public Box grown(double distance) {
        return new Box(left - distance, top - distance, right + distance, bottom + distance);
    }

    /** The box holding this one and another. */
    public Box joined(Box other) {
        return new Box(Math.min(left, other.left), Math.min(top, other.top), Math.max(right, other.right), Math.max(bottom, other.bottom));
    }

    public double width() {
        return right - left;
    }

    public double height() {
        return bottom - top;
    }

    /** Whether another box comes within a distance of this one: touching is within 0. */
    public boolean within(Box other, double distance) {
        return left - distance <= other.right && other.left - distance <= right && top - distance <= other.bottom
               && other.top - distance <= bottom;
    }

    /** Whether it overlaps another box, not only touches it. */
    public boolean overlaps(Box other) {
        return left < other.right && other.left < right && top < other.bottom && other.top < bottom;
    }

    /** Whether a point is in it, its sides included. */
    public boolean holds(double x, double y) {
        return left <= x && x <= right && top <= y && y <= bottom;
    }
}
