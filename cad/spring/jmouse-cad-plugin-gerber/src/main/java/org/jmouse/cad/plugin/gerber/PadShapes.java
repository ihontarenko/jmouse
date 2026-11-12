package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.exchange.pcb.PadStandardShape;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.gerber.GerberPoint;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.compose.GerberComposer;

import java.util.List;

/**
 * A pad as Gerber draws it, grown all round by what a layer asks — nothing on copper, the mask's expansion on the mask —:
 * flashed with a standard aperture where it is one turned a whole quarter — a circle, a rectangle, an obround —, else
 * filled as the area of its outline ({@link PadOutline}).
 */
final class PadShapes {

    private final PadOutline outlines;

    PadShapes(PadOutline outlines) {
        this.outlines = outlines;
    }

    /**
     * @param growth  how far the pad is grown all round
     * @param purpose the aperture's function — {@code SMDPad,CuDef}, {@code ComponentPad} —; null where a layer has none
     */
    void draw(GerberComposer composer, PcbDrawing.Placement placement, FootprintDrawing.Pad pad, double growth, String purpose) {
        // A rounded rectangle has no standard aperture: it is filled as its outline.
        GerberShape standard = PadStandardShape.of(placement, pad, growth).map(shape -> switch (shape) {
            case PadStandardShape.Circle circle -> new GerberShape.Circle(circle.diameter());
            case PadStandardShape.Rectangle rectangle -> new GerberShape.Rectangle(rectangle.width(), rectangle.height());
            case PadStandardShape.Oval oval -> new GerberShape.Obround(oval.width(), oval.height(), 0);
            case PadStandardShape.RoundedRectangle ignored -> null;
        }).orElse(null);
        if (standard != null) {
            composer.flash(composer.aperture(standard, purpose), PcbGerber.placedOut(placement, pad.x(), pad.y()));
            return;
        }
        List<GerberPoint> outline = outlines.placed(placement, pad, growth).stream()
                .map(point -> PcbGerber.out(point.x(), point.y())).toList();
        composer.region(List.of(outline));
    }
}
