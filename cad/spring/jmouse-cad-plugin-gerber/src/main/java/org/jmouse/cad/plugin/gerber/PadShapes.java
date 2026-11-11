package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.board.geometry.PadOutline;
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
    void draw(GerberComposer composer, BoardDrawing.Placement placement, FootprintDrawing.Pad pad, double growth, String purpose) {
        double turn = (((pad.rotation() + placement.rotation()) % 360) + 360) % 360;
        boolean quarter = turn % 90 == 0;
        boolean across = turn == 90 || turn == 270;
        double width = (across ? pad.height() : pad.width()) + 2 * growth;
        double height = (across ? pad.width() : pad.height()) + 2 * growth;
        GerberShape standard = switch (pad.shape()) {
            case "circle" -> new GerberShape.Circle(pad.width() + 2 * growth);
            case "rectangle" -> quarter ? new GerberShape.Rectangle(width, height) : null;
            case "oval" -> quarter ? new GerberShape.Obround(width, height, 0) : null;
            default -> null;
        };
        if (standard != null) {
            composer.flash(composer.aperture(standard, purpose), BoardGerber.placedOut(placement, pad.x(), pad.y()));
            return;
        }
        List<GerberPoint> outline = outlines.placed(placement, pad, growth).stream()
                .map(point -> BoardGerber.out(point.x(), point.y())).toList();
        composer.region(List.of(outline));
    }
}
