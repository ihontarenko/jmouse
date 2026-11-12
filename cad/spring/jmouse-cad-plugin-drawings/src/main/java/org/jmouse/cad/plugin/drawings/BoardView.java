package org.jmouse.cad.plugin.drawings;

import org.jmouse.cad.exchange.pcb.PcbProfile;
import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.List;

/**
 * A pcb seen on a sheet: where its millimetres land on the paper. The pcb's frame has Y growing downwards, a sheet's upwards;
 * a view from below is mirrored left to right.
 *
 * @param factor   millimetres of paper to one of pcb
 * @param left     the paper's X of the view's left edge
 * @param bottom   the paper's Y of the view's lower edge
 * @param mirrored whether it is seen from below
 */
record BoardView(Bounds bounds, double factor, double left, double bottom, boolean mirrored) {

    /** The extent of a pcb's outline, in its own frame. */
    record Bounds(double minimumX, double minimumY, double maximumX, double maximumY) {

        static Bounds of(PcbProfile profile) {
            List<PcbDrawing.Point> outline = profile.outline();
            return new Bounds(outline.stream().mapToDouble(PcbDrawing.Point::x).min().orElse(0),
                    outline.stream().mapToDouble(PcbDrawing.Point::y).min().orElse(0),
                    outline.stream().mapToDouble(PcbDrawing.Point::x).max().orElse(0),
                    outline.stream().mapToDouble(PcbDrawing.Point::y).max().orElse(0));
        }

        double width() {
            return maximumX - minimumX;
        }

        double height() {
            return maximumY - minimumY;
        }
    }

    Sheet.Point at(PcbDrawing.Point point) {
        return at(point.x(), point.y());
    }

    Sheet.Point at(double x, double y) {
        double across = mirrored ? bounds.maximumX() - x : x - bounds.minimumX();
        return new Sheet.Point(left + across * factor, bottom + (bounds.maximumY() - y) * factor);
    }

    List<Sheet.Point> at(List<PcbDrawing.Point> points) {
        return points.stream().map(this::at).toList();
    }

    double right() {
        return left + bounds.width() * factor;
    }

    double top() {
        return bottom + bounds.height() * factor;
    }
}
