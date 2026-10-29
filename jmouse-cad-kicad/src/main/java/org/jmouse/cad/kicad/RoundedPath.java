package org.jmouse.cad.kicad;

import org.jmouse.svg.path.node.CircularArc;
import org.jmouse.svg.path.node.PathBuilder;
import org.jmouse.svg.path.node.PathData;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A path a KiCad drawing becomes, every number rounded to what the drawing shows: KiCad writes nanometres, far below a
 * hundredth of a symbol's unit or a thousandth of a millimetre on a board. Shapes are drawn one after another into one
 * path.
 */
final class RoundedPath {

    private final PathBuilder path = PathBuilder.path();
    private final int places;
    private boolean empty = true;

    /** @param places the decimal places every number keeps */
    RoundedPath(int places) {
        this.places = places;
    }

    double rounded(double value) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_EVEN).doubleValue() + 0.0;
    }

    RoundedPath moveTo(double x, double y) {
        empty = false;
        path.moveTo(rounded(x), rounded(y));
        return this;
    }

    RoundedPath lineTo(double x, double y) {
        path.lineTo(rounded(x), rounded(y));
        return this;
    }

    RoundedPath horizontalTo(double x) {
        path.horizontalTo(rounded(x));
        return this;
    }

    RoundedPath verticalTo(double y) {
        path.verticalTo(rounded(y));
        return this;
    }

    RoundedPath cubicTo(double firstX, double firstY, double secondX, double secondY, double x, double y) {
        path.cubicTo(rounded(firstX), rounded(firstY), rounded(secondX), rounded(secondY), rounded(x), rounded(y));
        return this;
    }

    RoundedPath close() {
        path.close();
        return this;
    }

    /** From the start through the middle to the end: an arc of the circle through the three, a line when they lie on one. */
    RoundedPath arcThrough(double startX, double startY, double middleX, double middleY, double endX, double endY) {
        moveTo(startX, startY);
        CircularArc.through(startX, startY, middleX, middleY, endX, endY).ifPresentOrElse(arc -> {
            double radius = rounded(arc.radius());
            path.arcTo(radius, radius, 0, arc.largeArc(), arc.sweep(), rounded(endX), rounded(endY));
        }, () -> lineTo(endX, endY));
        return this;
    }

    /** A circle as two half arcs, from its leftmost point. */
    RoundedPath circle(double x, double y, double radius) {
        double r = rounded(radius);
        moveTo(x - radius, y);
        path.arcTo(r, r, 0, true, false, rounded(x + radius), rounded(y));
        path.arcTo(r, r, 0, true, false, rounded(x - radius), rounded(y));
        return close();
    }

    RoundedPath append(PathData other) {
        empty = empty && other.commands().isEmpty();
        path.append(other);
        return this;
    }

    boolean isEmpty() {
        return empty;
    }

    PathData build() {
        return path.build();
    }
}
