package org.jmouse.cad.packages.footprint;

import org.jmouse.cad.packages.MeasuredPath;
import org.jmouse.cad.packages.footprint.FootprintDrawing.Outline;
import org.jmouse.cad.packages.footprint.FootprintDrawing.Pad;
import org.jmouse.cad.packages.footprint.FootprintDrawing.PadShape;
import org.jmouse.cad.packages.footprint.FootprintDrawing.PadType;

import static org.jmouse.cad.packages.Measures.exact;

/**
 * The drawing every generated footprint shares: pads at a place, a body on the fabrication layer, the courtyard around
 * copper and body, a dot beside the first pin. Origin at the package's centre, Y down; every length to the micrometre.
 */
final class FootprintDrafting {

    private static final String FIRST_PIN = "1";

    private final LandPatternRules.Drawing drawing;

    FootprintDrafting(LandPatternRules.Drawing drawing) {
        this.drawing = drawing;
    }

    /** The n-th lead of a row, the row centred on the origin. */
    static double rowY(int index, int count, double pitch) {
        return (index - (count - 1) / 2.0) * pitch;
    }

    Pad surfacePad(String number, double x, double y, double width, double height) {
        return pad(number, PadType.SURFACE, PadShape.ROUNDED_RECTANGLE, x, y, width, height, 0, drawing.roundness());
    }

    /** A through-hole pad; the first is square so pin 1 is found on the bare board. */
    Pad throughHolePad(String number, double x, double y, double size, double drill) {
        PadShape shape = FIRST_PIN.equals(number) ? PadShape.RECTANGLE : PadShape.CIRCLE;
        return pad(number, PadType.THROUGH_HOLE, shape, x, y, size, size, drill, 0);
    }

    /** A through-hole pad {@code width} by {@code height}: pin 1 square-cornered, the others round — an oval where a neighbour narrowed them. */
    Pad throughHolePad(String number, double x, double y, double width, double height, double drill) {
        PadShape shape = FIRST_PIN.equals(number) ? PadShape.RECTANGLE : width == height ? PadShape.CIRCLE : PadShape.OVAL;
        return pad(number, PadType.THROUGH_HOLE, shape, x, y, width, height, drill, 0);
    }

    /** A plated hole of one shape however it is numbered: a mounting hole's ring, a via stitched around it. */
    Pad platedHole(String number, double x, double y, double size, double drill) {
        return pad(number, PadType.THROUGH_HOLE, PadShape.CIRCLE, x, y, size, size, drill, 0);
    }

    /** A bare hole with no copper: it belongs to no net, so it has no number. */
    Pad unplatedHole(double drill) {
        return pad("", PadType.UNPLATED_HOLE, PadShape.CIRCLE, 0, 0, drill, drill, drill, 0);
    }

    /** A round surface pad: a test point. */
    Pad roundSurfacePad(String number, double diameter) {
        return pad(number, PadType.SURFACE, PadShape.CIRCLE, 0, 0, diameter, diameter, 0, 0);
    }

    /** The body, its pin-1 corner cut by the chamfer — no larger than a third of its shorter side. */
    Outline body(double width, double length) {
        double halfWidth = width / 2;
        double halfLength = length / 2;
        double chamfer = Math.min(drawing.chamfer(), Math.min(width, length) / 3);
        return fabrication(MeasuredPath.path().moveTo(-halfWidth + chamfer, -halfLength).horizontalTo(halfWidth)
                .verticalTo(halfLength).horizontalTo(-halfWidth).verticalTo(-halfLength + chamfer).close());
    }

    /** The body of a part with no first pin: a plain rectangle. */
    Outline plainBody(double width, double length) {
        return fabrication(MeasuredPath.path().rectangle(width / 2, length / 2));
    }

    Outline courtyard(double halfWidth, double halfLength) {
        return outline("courtyard", drawing.courtyardWidth(),
                MeasuredPath.path().rectangle(drawing.nearest(halfWidth), drawing.nearest(halfLength)));
    }

    /** The courtyard as a box that need not be centred: a TO body stands in front of its leads. */
    Outline courtyard(double left, double top, double right, double bottom) {
        return outline("courtyard", drawing.courtyardWidth(), MeasuredPath.path().box(drawing.nearest(left),
                drawing.nearest(top), drawing.nearest(right), drawing.nearest(bottom)));
    }

    Outline courtyardCircle(double radius) {
        return outline("courtyard", drawing.courtyardWidth(), MeasuredPath.path().circle(0, 0, drawing.nearest(radius)));
    }

    Outline fabrication(MeasuredPath path) {
        return outline("fabrication", drawing.fabricationWidth(), path);
    }

    Outline silkscreen(MeasuredPath path) {
        return outline("silkscreen", drawing.silkscreenWidth(), path);
    }

    /** A filled dot on the silkscreen beside the first pin. */
    Outline pinMark(double x, double y) {
        return new Outline("silkscreen", 0, true, MeasuredPath.path().circle(x, y, drawing.pinMarkRadius()).build());
    }

    private static Outline outline(String layer, double width, MeasuredPath path) {
        return new Outline(layer, width, false, path.build());
    }

    private static Pad pad(String number, PadType type, PadShape shape, double x, double y, double width, double height,
                           double drill, double roundness) {
        return new Pad(number, type, shape, exact(x), exact(y), exact(width), exact(height), exact(drill), roundness);
    }
}
