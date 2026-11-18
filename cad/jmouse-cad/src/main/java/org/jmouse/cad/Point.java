package org.jmouse.cad;

/**
 * A place on a sheet or a pcb, in its units.
 *
 * @param x across, in its drawing's units
 * @param y down, in its drawing's units
 */
public record Point(double x, double y) {

    /** The point a share of the way from this one to another: 0 is this one, 1 the other. */
    public Point toward(Point other, double share) {
        return new Point(x + (other.x - x) * share, y + (other.y - y) * share);
    }

    /** The point halfway between this one and another. */
    public Point halfwayTo(Point other) {
        return new Point((x + other.x) / 2, (y + other.y) / 2);
    }

    /** How far this point stands from another. */
    public double distanceTo(Point other) {
        return Math.hypot(other.x - x, other.y - y);
    }

    /** This point moved a distance along a way — a unit length. */
    public Point moved(Point way, double distance) {
        return new Point(x + way.x * distance, y + way.y * distance);
    }

    /** The dot product of this point and another, each taken as a vector from the origin. */
    public double dot(Point other) {
        return x * other.x + y * other.y;
    }

    /** The cross product of this point and another, each taken as a vector from the origin: positive where the other turns left of it. */
    public double cross(Point other) {
        return x * other.y - y * other.x;
    }
}
