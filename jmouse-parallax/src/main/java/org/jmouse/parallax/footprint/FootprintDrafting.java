package org.jmouse.parallax.footprint;

import org.jmouse.parallax.script.Block.FootprintOutline;
import org.jmouse.parallax.script.Block.FootprintPad;
import org.jmouse.parallax.script.Block.PadShape;
import org.jmouse.parallax.script.Block.PadType;
import org.jmouse.parallax.script.engine.Notation;

/**
 * The drawing every generated footprint shares: pads at a place, a body on the fabrication layer, the courtyard around
 * copper and body, and path data in millimetres with no float noise. Origin at the package's centre, Y down.
 */
final class FootprintDrafting {

    private final LandPatternRules.Drawing drawing;

    FootprintDrafting(LandPatternRules.Drawing drawing) {
        this.drawing = drawing;
    }

    LandPatternRules.Drawing rules() {
        return drawing;
    }

    FootprintPad surfacePad(String number, double x, double y, double width, double height) {
        return new FootprintPad(number, PadType.SURFACE, PadShape.ROUNDED_RECTANGLE, exact(x), exact(y), 0,
                exact(width), exact(height), 0, 0, drawing.roundness());
    }

    /** A through-hole pad; the first is square so pin 1 is found on the bare board. */
    FootprintPad throughHolePad(String number, double x, double y, double size, double drill) {
        PadShape shape = "1".equals(number) ? PadShape.RECTANGLE : PadShape.CIRCLE;
        return new FootprintPad(number, PadType.THROUGH_HOLE, shape, exact(x), exact(y), 0, exact(size), exact(size),
                exact(drill), exact(drill), 0);
    }

    /**
     * A through-hole pad {@code width} by {@code height}: pin 1 square-cornered, the others round — an oval where a
     * neighbour narrowed them.
     */
    FootprintPad throughHolePad(String number, double x, double y, double width, double height, double drill) {
        PadShape shape = "1".equals(number) ? PadShape.RECTANGLE : width == height ? PadShape.CIRCLE : PadShape.OVAL;
        return new FootprintPad(number, PadType.THROUGH_HOLE, shape, exact(x), exact(y), 0, exact(width), exact(height),
                exact(drill), exact(drill), 0);
    }

    /** A plated hole of one shape however it is numbered: a mounting hole's ring, a via stitched around it. */
    FootprintPad platedHole(String number, double x, double y, double size, double drill) {
        return new FootprintPad(number, PadType.THROUGH_HOLE, PadShape.CIRCLE, exact(x), exact(y), 0, exact(size),
                exact(size), exact(drill), exact(drill), 0);
    }

    /** A bare hole with no copper: it belongs to no net. */
    FootprintPad unplatedHole(double drill) {
        return new FootprintPad("", PadType.UNPLATED_HOLE, PadShape.CIRCLE, 0, 0, 0, exact(drill), exact(drill),
                exact(drill), exact(drill), 0);
    }

    /** A round surface pad: a test point. */
    FootprintPad roundSurfacePad(String number, double diameter) {
        return new FootprintPad(number, PadType.SURFACE, PadShape.CIRCLE, 0, 0, 0, exact(diameter), exact(diameter), 0,
                0, 0);
    }

    /** The body, its pin-1 corner cut by the chamfer — no larger than a third of its shorter side. */
    FootprintOutline body(double width, double length) {
        double halfWidth = width / 2;
        double halfLength = length / 2;
        double chamfer = Math.min(drawing.chamfer(), Math.min(width, length) / 3);
        return new FootprintOutline("fabrication", drawing.fabricationWidth(), false,
                "M" + point(-halfWidth + chamfer, -halfLength) + "H" + number(halfWidth) + "V" + number(halfLength)
                        + "H" + number(-halfWidth) + "V" + number(-halfLength + chamfer) + "Z");
    }

    /** The body of a part with no first pin: a plain rectangle. */
    FootprintOutline plainBody(double width, double length) {
        return new FootprintOutline("fabrication", drawing.fabricationWidth(), false, rectangle(width / 2, length / 2));
    }

    FootprintOutline courtyard(double halfWidth, double halfLength) {
        return new FootprintOutline("courtyard", drawing.courtyardWidth(), false,
                rectangle(drawing.nearest(halfWidth), drawing.nearest(halfLength)));
    }

    /** The courtyard as a box that need not be centred: a TO body stands in front of its leads. */
    FootprintOutline courtyard(double left, double top, double right, double bottom) {
        return new FootprintOutline("courtyard", drawing.courtyardWidth(), false,
                "M" + point(drawing.nearest(left), drawing.nearest(top)) + "H" + number(drawing.nearest(right))
                        + "V" + number(drawing.nearest(bottom)) + "H" + number(drawing.nearest(left)) + "Z");
    }

    FootprintOutline courtyardCircle(double radius) {
        return new FootprintOutline("courtyard", drawing.courtyardWidth(), false, circle(0, 0, drawing.nearest(radius)));
    }

    FootprintOutline fabrication(String path) {
        return new FootprintOutline("fabrication", drawing.fabricationWidth(), false, path);
    }

    FootprintOutline silkscreen(String path) {
        return new FootprintOutline("silkscreen", drawing.silkscreenWidth(), false, path);
    }

    /** A filled dot on the silkscreen beside the first pin. */
    FootprintOutline pinMark(double x, double y) {
        return new FootprintOutline("silkscreen", 0, true, circle(x, y, drawing.pinMarkRadius()));
    }

    /** A circle as two half arcs. */
    static String circle(double x, double y, double radius) {
        return "M" + point(x - radius, y) + "A" + number(radius) + " " + number(radius) + " 0 1 0 " + point(x + radius, y)
                + "A" + number(radius) + " " + number(radius) + " 0 1 0 " + point(x - radius, y) + "Z";
    }

    String rectangle(double halfWidth, double halfLength) {
        return "M" + point(-halfWidth, -halfLength) + "H" + number(halfWidth) + "V" + number(halfLength)
                + "H" + number(-halfWidth) + "Z";
    }

    static String point(double x, double y) {
        return number(x) + " " + number(y);
    }

    static String number(double value) {
        return Notation.number(exact(value));
    }

    /** A millimetre value without the float noise sums leave: to the micrometre. */
    static double exact(double value) {
        double rounded = Math.round(value * 1000) / 1000.0;
        return rounded == 0 ? 0 : rounded;
    }
}
