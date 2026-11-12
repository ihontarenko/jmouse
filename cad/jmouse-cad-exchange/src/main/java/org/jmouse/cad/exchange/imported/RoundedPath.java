package org.jmouse.cad.exchange.imported;

import org.jmouse.svg.path.node.CircularArc;
import org.jmouse.svg.path.node.PathBuilder;
import org.jmouse.svg.path.node.PathData;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A path another product's drawing becomes, every number rounded to what the drawing shows: KiCad writes nanometres,
 * far below a hundredth of a symbol's unit or a thousandth of a millimetre on a pcb. Shapes are drawn one after another
 * into one path.
 */
public final class RoundedPath {

    private final PathBuilder path = PathBuilder.path();
    private final int places;
    private boolean empty = true;
    /** Where the pen stands, rounded, and where its subpath began: a shape that starts there goes on without lifting it. */
    private double penX;
    private double penY;
    private double subpathX;
    private double subpathY;

    /** @param places the decimal places every number keeps */
    public RoundedPath(int places) {
        this.places = places;
    }

    public double rounded(double value) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_EVEN).doubleValue() + 0.0;
    }

    public RoundedPath moveTo(double x, double y) {
        empty = false;
        path.moveTo(rounded(x), rounded(y));
        subpathX = rounded(x);
        subpathY = rounded(y);
        return pen(x, y);
    }

    public RoundedPath lineTo(double x, double y) {
        path.lineTo(rounded(x), rounded(y));
        return pen(x, y);
    }

    public RoundedPath horizontalTo(double x) {
        path.horizontalTo(rounded(x));
        return pen(x, penY);
    }

    public RoundedPath verticalTo(double y) {
        path.verticalTo(rounded(y));
        return pen(penX, y);
    }

    public RoundedPath cubicTo(double firstX, double firstY, double secondX, double secondY, double x, double y) {
        path.cubicTo(rounded(firstX), rounded(firstY), rounded(secondX), rounded(secondY), rounded(x), rounded(y));
        return pen(x, y);
    }

    public RoundedPath close() {
        path.close();
        return pen(subpathX, subpathY);
    }

    /** A straight stretch from a point to a point; where the pen stands on the first — an outline going on —, not lifted. */
    public RoundedPath lineFrom(double startX, double startY, double endX, double endY) {
        reach(startX, startY);
        return lineTo(endX, endY);
    }

    /** A cubic curve from a point, pulled by two handles; where the pen stands on its start, not lifted. */
    public RoundedPath curveFrom(double startX, double startY, double firstX, double firstY, double secondX, double secondY,
                                 double endX, double endY) {
        reach(startX, startY);
        return cubicTo(firstX, firstY, secondX, secondY, endX, endY);
    }

    /**
     * From the start through the middle to the end: an arc of the circle through the three, a line when they lie on one.
     * Where the pen already stands on the start — the arc goes on an outline —, it is not lifted.
     */
    public RoundedPath arcThrough(double startX, double startY, double middleX, double middleY, double endX, double endY) {
        reach(startX, startY);
        // The circle through the points as the path keeps them, its radius whole: near half a turn, an arc's bulge follows
        // its radius so closely that a radius rounded like the points would carry its middle off the point it was drawn through.
        CircularArc.through(rounded(startX), rounded(startY), rounded(middleX), rounded(middleY), rounded(endX), rounded(endY)).ifPresentOrElse(arc -> {
            double radius = arc.radius();
            path.arcTo(radius, radius, 0, arc.largeArc(), arc.sweep(), rounded(endX), rounded(endY));
            pen(endX, endY);
        }, () -> lineTo(endX, endY));
        return this;
    }

    /** The pen on a point: lifted to it unless it already stands there. */
    private void reach(double x, double y) {
        if (empty || penX != rounded(x) || penY != rounded(y)) {
            moveTo(x, y);
        }
    }

    private RoundedPath pen(double x, double y) {
        penX = rounded(x);
        penY = rounded(y);
        return this;
    }

    /** A circle as two half arcs, from its leftmost point. */
    public RoundedPath circle(double x, double y, double radius) {
        double r = rounded(radius);
        moveTo(x - radius, y);
        path.arcTo(r, r, 0, true, false, rounded(x + radius), rounded(y));
        path.arcTo(r, r, 0, true, false, rounded(x - radius), rounded(y));
        return close();
    }

    public RoundedPath append(PathData other) {
        empty = empty && other.commands().isEmpty();
        path.append(other);
        // Where another path leaves the pen is not followed here: the next arc starts a subpath of its own.
        penX = Double.NaN;
        penY = Double.NaN;
        return this;
    }

    public boolean isEmpty() {
        return empty;
    }

    public PathData build() {
        return path.build();
    }
}
