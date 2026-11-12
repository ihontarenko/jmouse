package org.jmouse.cad.plugin.drawings;

import org.jmouse.strokefont.StrokeText;

import java.util.ArrayList;
import java.util.List;

/**
 * Schematics as pages of a document. A sheet is routed and drawn only in the interface, so what arrives is its drawing as
 * geometry: lines and words in the drawing's own units, Y down. Here it is fitted to the paper above the title block, and
 * from then on it is a page like any other.
 */
final class SchematicPages {

    /** How high a capital stands in a line of a given font size. */
    private static final double CAP_SHARE = 0.72;

    private static final double FULL_TURN = 360;

    private final SheetSettings settings;
    private final SheetLettering lettering;

    SchematicPages(SheetSettings settings, SheetLettering lettering) {
        this.settings = settings;
        this.lettering = lettering;
    }

    /**
     * A run of a drawing.
     *
     * @param points its corners, x and y in turn
     * @param width  its pen, in the drawing's units
     * @param stroke its colour, {@code #rrggbb}; null for none
     * @param fill   the colour of its area; null for none
     */
    record Run(double[] points, boolean closed, double width, String stroke, String fill) {
    }

    /**
     * Words of a drawing.
     *
     * @param size     the font's size, in the drawing's units
     * @param rotation degrees clockwise, as the drawing turns
     * @param anchor   {@code start}, {@code middle} or {@code end}
     * @param baseline {@code alphabetic} or {@code middle}
     */
    record Lettering(String text, double x, double y, double size, double rotation, String anchor, String baseline, String colour) {
    }

    /** One schematic's drawing. */
    record Drawn(String name, String title, String revision, List<Run> runs, List<Lettering> words) {
    }

    SheetPage page(Drawn drawn) {
        SheetSettings.Paper paper = paperOf();
        double[] box = boxOf(drawn);
        double top = paper.height() - settings.margin() - settings.columnGap();
        double bottom = settings.margin() + settings.titleHeight() + settings.columnGap();
        double left = settings.margin() + settings.columnGap();
        double right = paper.width() - settings.margin() - settings.columnGap();
        double width = Math.max(box[2] - box[0], 1);
        double height = Math.max(box[3] - box[1], 1);
        double factor = Math.min((right - left) / width, (top - bottom) / height);
        double originX = (left + right - width * factor) / 2 - box[0] * factor;
        double originY = (top + bottom + height * factor) / 2 + box[1] * factor;
        SheetPage page = new SheetPage(drawn.name(), settings.words().schematicSheet() + " · " + drawn.title(), settings, paper, settings.words().schematicScale(), lettering, drawn.revision(), SheetPurpose.DOCUMENT);
        for (Run run : drawn.runs()) {
            add(page, run, factor, originX, originY);
        }
        for (Lettering words : drawn.words()) {
            add(page, words, factor, originX, originY);
        }
        return page;
    }

    private void add(SheetPage page, Run run, double factor, double originX, double originY) {
        List<Sheet.Point> points = new ArrayList<>();
        for (int index = 0; index + 1 < run.points().length; index += 2) {
            points.add(new Sheet.Point(originX + run.points()[index] * factor, originY - run.points()[index + 1] * factor));
        }
        if (points.size() < 2) {
            return;
        }
        if (run.fill() != null) {
            page.add(new Sheet.Mark.Inked(new Sheet.Mark.Area(SheetLayer.ARTWORK, List.of(points)), run.fill()));
        }
        if (run.stroke() != null) {
            double pen = Math.max(run.width() * factor, settings.penOf(SheetLayer.ARTWORK).pen());
            page.add(new Sheet.Mark.Inked(new Sheet.Mark.Line(SheetLayer.ARTWORK, points, run.closed(), pen), run.stroke()));
        }
    }

    private void add(SheetPage page, Lettering words, double factor, double originX, double originY) {
        if (words.text() == null || words.text().isBlank()) {
            return;
        }
        Sheet.Mark.Words mark = new Sheet.Mark.Words(SheetLayer.NOTES, words.text().strip(), new Sheet.Point(originX + words.x() * factor, originY - words.y() * factor),
                // However far a large sheet is fitted down, its words stay words one can read on the paper.
                Math.max(words.size() * factor * CAP_SHARE, settings.text().schematicLeast()), (FULL_TURN - words.rotation()) % FULL_TURN, anchorOf(words.anchor()),
                "middle".equals(words.baseline()) ? StrokeText.Baseline.MIDDLE : StrokeText.Baseline.ALPHABETIC);
        page.add(words.colour() == null ? mark : new Sheet.Mark.Inked(mark, words.colour()));
    }

    private static StrokeText.Anchor anchorOf(String anchor) {
        return switch (anchor == null ? "" : anchor) {
            case "middle" -> StrokeText.Anchor.MIDDLE;
            case "end" -> StrokeText.Anchor.END;
            default -> StrokeText.Anchor.START;
        };
    }

    private SheetSettings.Paper paperOf() {
        return settings.papers().stream().filter(paper -> paper.name().equals(settings.schematicPaper())).findFirst()
                .orElseThrow(() -> new IllegalStateException("plugin/drawings/sheets names '" + settings.schematicPaper() + "', which is none of its papers"));
    }

    /** What the drawing takes: left, top, right, bottom, in its own units. Words count by where they stand. */
    private static double[] boxOf(Drawn drawn) {
        double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Run run : drawn.runs()) {
            for (int index = 0; index + 1 < run.points().length; index += 2) {
                include(box, run.points()[index], run.points()[index + 1]);
            }
        }
        drawn.words().forEach(words -> include(box, words.x(), words.y()));
        return box[0] > box[2] ? new double[]{0, 0, 1, 1} : box;
    }

    private static void include(double[] box, double x, double y) {
        box[0] = Math.min(box[0], x);
        box[1] = Math.min(box[1], y);
        box[2] = Math.max(box[2], x);
        box[3] = Math.max(box[3], y);
    }
}
