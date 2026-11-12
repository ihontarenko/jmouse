package org.jmouse.cad.plugin.drawings;

import org.jmouse.dxf.DxfDrawing;
import org.jmouse.strokefont.StrokeText;

import java.util.List;

/**
 * A sheet as a DXF of its own, a layer for each of its layers, in millimetres. Words stay words, so they can be edited and
 * searched. DXF's release 12 has no filled area of many corners: an area with three or four is a solid, any other its outline.
 */
final class DxfSheets {

    private static final int SOLID_CORNERS_FEWEST = 3;
    private static final int SOLID_CORNERS_MOST   = 4;

    private final SheetSettings settings;

    DxfSheets(SheetSettings settings) {
        this.settings = settings;
    }

    String write(Sheet sheet) {
        DxfDrawing drawing = new DxfDrawing();
        for (SheetLayer layer : SheetLayer.values()) {
            drawing.layer(layer.word(), settings.penOf(layer).colour());
        }
        sheet.marks().forEach(mark -> draw(drawing, mark));
        return drawing.write();
    }

    private static void draw(DxfDrawing drawing, Sheet.Mark mark) {
        String layer = mark.layer().word();
        switch (mark) {
            case Sheet.Mark.Line line -> drawing.polyline(layer, points(line.points()), line.pen(), line.closed());
            case Sheet.Mark.Area area -> area.rings().forEach(ring -> {
                if (area.rings().size() == 1 && ring.size() >= SOLID_CORNERS_FEWEST && ring.size() <= SOLID_CORNERS_MOST) {
                    drawing.solid(layer, points(ring));
                } else {
                    drawing.polyline(layer, points(ring), 0, true);
                }
            });
            case Sheet.Mark.Inked inked -> draw(drawing, inked.mark());
            case Sheet.Mark.Circle circle -> drawing.circle(layer, point(circle.centre()), circle.radius());
            case Sheet.Mark.Words words -> drawing.text(layer, point(words.at()), words.height(), words.rotation(), across(words.anchor()),
                    upright(words.baseline()), words.text());
        }
    }

    private static DxfDrawing.Across across(StrokeText.Anchor anchor) {
        return switch (anchor) {
            case START -> DxfDrawing.Across.LEFT;
            case MIDDLE -> DxfDrawing.Across.CENTRE;
            case END -> DxfDrawing.Across.RIGHT;
        };
    }

    private static DxfDrawing.Upright upright(StrokeText.Baseline baseline) {
        return switch (baseline) {
            case ALPHABETIC -> DxfDrawing.Upright.BASELINE;
            case TOP -> DxfDrawing.Upright.TOP;
            case MIDDLE -> DxfDrawing.Upright.MIDDLE;
            case BOTTOM -> DxfDrawing.Upright.BOTTOM;
        };
    }

    private static List<DxfDrawing.Point> points(List<Sheet.Point> points) {
        return points.stream().map(DxfSheets::point).toList();
    }

    private static DxfDrawing.Point point(Sheet.Point point) {
        return new DxfDrawing.Point(point.x(), point.y());
    }
}
