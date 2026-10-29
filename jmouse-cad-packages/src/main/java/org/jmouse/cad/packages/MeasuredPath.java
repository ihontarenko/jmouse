package org.jmouse.cad.packages;

import org.jmouse.svg.path.node.PathBuilder;
import org.jmouse.svg.path.node.PathData;

import static org.jmouse.cad.packages.Measures.exact;

/**
 * A generated path as its tree, every coordinate to the thousandth: the strokes a drawing is made of — a move, a line
 * along an axis, a circular arc — and the figures generated drawings share, the box and the circle.
 */
public final class MeasuredPath {

    private final PathBuilder path  = PathBuilder.path();
    private boolean           empty = true;

    private MeasuredPath() {
    }

    public static MeasuredPath path() {
        return new MeasuredPath();
    }

    public MeasuredPath moveTo(double x, double y) {
        empty = false;
        path.moveTo(exact(x), exact(y));
        return this;
    }

    public MeasuredPath lineTo(double x, double y) {
        path.lineTo(exact(x), exact(y));
        return this;
    }

    public MeasuredPath horizontalTo(double x) {
        path.horizontalTo(exact(x));
        return this;
    }

    public MeasuredPath verticalTo(double y) {
        path.verticalTo(exact(y));
        return this;
    }

    /** A circular arc of this radius to (x, y). */
    public MeasuredPath arcTo(double radius, boolean largeArc, boolean sweep, double x, double y) {
        path.arcTo(exact(radius), exact(radius), 0, largeArc, sweep, exact(x), exact(y));
        return this;
    }

    public MeasuredPath close() {
        path.close();
        return this;
    }

    /** A rectangle centred on the origin, drawn from its top left corner clockwise. */
    public MeasuredPath rectangle(double halfWidth, double halfLength) {
        return box(-halfWidth, -halfLength, halfWidth, halfLength);
    }

    /** A box that need not be centred: a TO body stands in front of its leads. */
    public MeasuredPath box(double left, double top, double right, double bottom) {
        return moveTo(left, top).horizontalTo(right).verticalTo(bottom).horizontalTo(left).close();
    }

    /** A circle as two half arcs. */
    public MeasuredPath circle(double x, double y, double radius) {
        return moveTo(x - radius, y).arcTo(radius, true, false, x + radius, y).arcTo(radius, true, false, x - radius, y).close();
    }

    /** Whether nothing has been drawn yet: a mark too short to draw is left out. */
    public boolean isEmpty() {
        return empty;
    }

    public PathData build() {
        return path.build();
    }
}
