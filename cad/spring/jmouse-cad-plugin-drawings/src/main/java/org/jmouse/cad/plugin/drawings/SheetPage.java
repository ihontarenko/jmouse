package org.jmouse.cad.plugin.drawings;

import org.jmouse.strokefont.StrokeText;

import java.util.ArrayList;
import java.util.List;

/**
 * A sheet as it is being drawn: the marks so far, and the drawing a page knows how to do by itself — lines, boxes, words,
 * dimensions and its frame.
 */
final class SheetPage {

    private final String              name;
    private final String              title;
    private final SheetSettings       settings;
    private final SheetSettings.Paper paper;
    private final String              scale;
    private final SheetLettering      lettering;
    private final SheetPurpose        purpose;
    private final String              revision;
    private final List<Sheet.Mark>    marks = new ArrayList<>();

    SheetPage(String name, String title, SheetSettings settings, SheetSettings.Paper paper, String scale, SheetLettering lettering, String revision, SheetPurpose purpose) {
        this.name = name;
        this.title = title;
        this.settings = settings;
        this.paper = paper;
        this.scale = scale;
        this.lettering = lettering;
        this.purpose = purpose;
        this.revision = revision;
        if (purpose != SheetPurpose.COVER) {
            double margin = settings.margin();
            box(SheetLayer.FRAME, margin, margin, paper.width() - margin, paper.height() - margin);
        }
    }

    SheetSettings.Paper paper() {
        return paper;
    }

    String title() {
        return title;
    }

    SheetPurpose purpose() {
        return purpose;
    }

    /** The revision of what it shows, which its title block says. */
    String revision() {
        return revision;
    }

    String scale() {
        return scale;
    }

    void add(Sheet.Mark mark) {
        marks.add(mark);
    }

    void line(SheetLayer layer, double fromX, double fromY, double toX, double toY) {
        marks.add(new Sheet.Mark.Line(layer, List.of(new Sheet.Point(fromX, fromY), new Sheet.Point(toX, toY)), false,
                settings.penOf(layer).pen()));
    }

    void box(SheetLayer layer, double left, double bottom, double right, double top) {
        marks.add(new Sheet.Mark.Line(layer, List.of(new Sheet.Point(left, bottom), new Sheet.Point(right, bottom),
                new Sheet.Point(right, top), new Sheet.Point(left, top)), true, settings.penOf(layer).pen()));
    }

    void words(SheetLayer layer, String text, double x, double y, double height, StrokeText.Anchor anchor, StrokeText.Baseline baseline) {
        if (!text.isBlank()) {
            marks.add(new Sheet.Mark.Words(layer, text, new Sheet.Point(x, y), height, 0, anchor, baseline));
        }
    }

    /**
     * A dimension: two extension lines from what is measured, the line between them with its arrowheads, and what it measures
     * written along it.
     *
     * @param from   one end of what is measured, on paper
     * @param to     the other
     * @param offset how far the line stands from them, along the normal to the left of from→to
     * @param value  what it says
     */
    void dimension(Sheet.Point from, Sheet.Point to, double offset, String value) {
        double length = Math.hypot(to.x() - from.x(), to.y() - from.y());
        if (length == 0) {
            return;
        }
        double alongX = (to.x() - from.x()) / length;
        double alongY = (to.y() - from.y()) / length;
        double normalX = -alongY * Math.signum(offset == 0 ? 1 : offset);
        double normalY = alongX * Math.signum(offset == 0 ? 1 : offset);
        double standing = Math.abs(offset);
        double arrow = settings.dimensionArrow();
        Sheet.Point first = new Sheet.Point(from.x() + normalX * standing, from.y() + normalY * standing);
        Sheet.Point second = new Sheet.Point(to.x() + normalX * standing, to.y() + normalY * standing);
        double overshoot = arrow / 2;
        line(SheetLayer.DIMENSIONS, from.x(), from.y(), first.x() + normalX * overshoot, first.y() + normalY * overshoot);
        line(SheetLayer.DIMENSIONS, to.x(), to.y(), second.x() + normalX * overshoot, second.y() + normalY * overshoot);
        line(SheetLayer.DIMENSIONS, first.x(), first.y(), second.x(), second.y());
        arrowhead(first, alongX, alongY, arrow);
        arrowhead(second, -alongX, -alongY, arrow);
        double rotation = Math.toDegrees(Math.atan2(alongY, alongX));
        if (rotation > 90 || rotation <= -90) {
            rotation += rotation > 0 ? -180 : 180;
        }
        double clearance = settings.text().label() * 0.3;
        // The words stand on the side of the line away from what it measures, reading from the bottom or from the right.
        double side = normalY > 0 || (normalY == 0 && normalX < 0) ? 1 : -1;
        double textNormalX = -Math.sin(Math.toRadians(rotation)) * side;
        double textNormalY = Math.cos(Math.toRadians(rotation)) * side;
        marks.add(new Sheet.Mark.Words(SheetLayer.DIMENSIONS, value, new Sheet.Point((first.x() + second.x()) / 2 + textNormalX * clearance,
                (first.y() + second.y()) / 2 + textNormalY * clearance), settings.text().label(), rotation, StrokeText.Anchor.MIDDLE,
                side > 0 ? StrokeText.Baseline.ALPHABETIC : StrokeText.Baseline.TOP));
    }

    /** How wide words stand. */
    double widthOf(String text, double height) {
        return lettering.widthOf(text, height);
    }

    Sheet sheet() {
        return new Sheet(name, title, paper, scale, purpose, marks);
    }

    private void arrowhead(Sheet.Point tip, double alongX, double alongY, double length) {
        double half = length * 0.22;
        Sheet.Point base = new Sheet.Point(tip.x() + alongX * length, tip.y() + alongY * length);
        marks.add(new Sheet.Mark.Area(SheetLayer.DIMENSIONS, List.of(List.of(tip,
                new Sheet.Point(base.x() - alongY * half, base.y() + alongX * half),
                new Sheet.Point(base.x() + alongY * half, base.y() - alongX * half)))));
    }
}
