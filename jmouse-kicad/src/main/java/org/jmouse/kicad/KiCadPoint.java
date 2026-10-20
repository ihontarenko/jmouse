package org.jmouse.kicad;

import org.jmouse.kicad.parser.node.KiCadList;

/**
 * A point of a KiCad drawing, in millimetres. On a sheet Y grows downwards; inside a library symbol it grows upwards —
 * {@link KiCadPlacement} turns the one into the other.
 */
public record KiCadPoint(double x, double y) {

    /** KiCad's internal unit is a nanometre: two points closer than this are one point. */
    private static final double SAME = 1e-6;

    /** The point an {@code (at x y …)} or an {@code (xy x y)} list writes. */
    public static KiCadPoint of(KiCadList list) {
        return new KiCadPoint(list.number(1), list.number(2));
    }

    public KiCadPoint plus(KiCadPoint other) {
        return new KiCadPoint(x + other.x, y + other.y);
    }

    /** Whether it is the same point, to KiCad's nanometre. */
    public boolean sameAs(KiCadPoint other) {
        return Math.abs(x - other.x) < SAME && Math.abs(y - other.y) < SAME;
    }

    /** Whether it lies on the segment between two points — on one of its ends or anywhere between. */
    public boolean liesOn(KiCadPoint start, KiCadPoint end) {
        double across = (end.x - start.x) * (y - start.y) - (end.y - start.y) * (x - start.x);
        double length = Math.hypot(end.x - start.x, end.y - start.y);
        if (length < SAME) {
            return sameAs(start);
        }
        if (Math.abs(across) / length > SAME) {
            return false;
        }
        double along = (x - start.x) * (end.x - start.x) + (y - start.y) * (end.y - start.y);
        return along >= -SAME * length && along <= length * length + SAME * length;
    }

    /** The point as a key two equal points share, to KiCad's nanometre. */
    public String key() {
        return Math.round(x / SAME) + "," + Math.round(y / SAME);
    }
}
