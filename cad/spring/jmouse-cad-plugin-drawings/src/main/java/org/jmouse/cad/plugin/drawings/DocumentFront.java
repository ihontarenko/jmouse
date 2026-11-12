package org.jmouse.cad.plugin.drawings;

import org.jmouse.strokefont.StrokeText;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.List;

/**
 * The front of a document: its cover, with the product's mark, the design's name and what can be told of it at a glance, and its
 * contents, every sheet by the number it stands at.
 */
final class DocumentFront {

    private static final PathParser PATHS = new PathParser();

    /** How many straight pieces the mark's curves take. */
    private static final int MARK_STEPS = 16;

    private static final double FULL_TURN = 2 * Math.PI;

    /** How many straight pieces the mark's dot takes. */
    private static final int DOT_STEPS = 48;

    private final SheetSettings        settings;
    private final SheetSettings.Cover  cover;
    private final SheetSettings.Paper  paper;
    private final SheetLettering       lettering;

    DocumentFront(SheetSettings settings, SheetLettering lettering) {
        this.settings = settings;
        this.cover = settings.cover();
        this.paper = settings.papers().getFirst();
        this.lettering = lettering;
    }

    SheetPage cover(SheetFacts facts) {
        SheetPage page = new SheetPage("cover", "", settings, paper, "", lettering, facts.revision(), SheetPurpose.COVER);
        double left = settings.margin() + settings.columnGap() * 2;
        double right = paper.width() - left;
        double top = paper.height() - left;
        // The mark and the product's name, as they stand at the head of the product itself.
        mark(page, left, top - cover.markSize(), cover.markSize());
        double nameAt = top - cover.markSize() / 2;
        page.add(new Sheet.Mark.Inked(new Sheet.Mark.Words(SheetLayer.FRAME, cover.product(), new Sheet.Point(left + cover.markSize() + settings.columnGap(), nameAt),
                cover.productHeight(), 0, StrokeText.Anchor.START, StrokeText.Baseline.MIDDLE, Sheet.Weight.BOLD), cover.primary()));
        page.add(new Sheet.Mark.Inked(new Sheet.Mark.Words(SheetLayer.FRAME, facts.year(), new Sheet.Point(right, nameAt), cover.productHeight(), 0,
                StrokeText.Anchor.END, StrokeText.Baseline.MIDDLE, Sheet.Weight.REGULAR), cover.muted()));
        rule(page, left, right, top - cover.markSize() - settings.columnGap(), cover.primary());
        // The design: what the eye lands on.
        double titleAt = paper.height() * cover.titleAtShare();
        double titleHeight = fittedHeight(facts.title(), cover.titleHeight(), right - left);
        page.add(new Sheet.Mark.Inked(new Sheet.Mark.Words(SheetLayer.FRAME, facts.title(), new Sheet.Point(left, titleAt), titleHeight, 0, StrokeText.Anchor.START,
                StrokeText.Baseline.ALPHABETIC, Sheet.Weight.BOLD), cover.primary()));
        page.add(new Sheet.Mark.Inked(new Sheet.Mark.Words(SheetLayer.FRAME, cover.subtitle(), new Sheet.Point(left, titleAt - settings.columnGap() * 2), settings.text().heading(), 0,
                StrokeText.Anchor.START, StrokeText.Baseline.TOP, Sheet.Weight.REGULAR), cover.muted()));
        // What can be told of it, in one quiet grid along the foot.
        List<SheetFacts.Fact> told = new ArrayList<>();
        if (!facts.author().isBlank()) {
            told.add(new SheetFacts.Fact(settings.words().factAuthor(), facts.author()));
        }
        told.add(new SheetFacts.Fact(settings.words().titleRevision(), facts.revision()));
        told.add(new SheetFacts.Fact(settings.words().factDate(), facts.date()));
        told.addAll(facts.facts());
        int columns = cover.factColumns();
        double width = (right - left) / columns;
        int rows = (told.size() + columns - 1) / columns;
        double rowHeight = settings.text().body() * 4.2;
        double bottom = left;
        rule(page, left, right, bottom + rows * rowHeight + settings.columnGap() / 2, cover.primary());
        for (int index = 0; index < told.size(); index++) {
            double x = left + (index % columns) * width;
            double y = bottom + (rows - 1 - index / columns) * rowHeight;
            SheetFacts.Fact fact = told.get(index);
            page.add(new Sheet.Mark.Inked(new Sheet.Mark.Words(SheetLayer.FRAME, fact.label(), new Sheet.Point(x, y + settings.text().body() * 1.7), settings.text().label(), 0,
                    StrokeText.Anchor.START, StrokeText.Baseline.ALPHABETIC, Sheet.Weight.REGULAR), cover.muted()));
            page.add(new Sheet.Mark.Inked(new Sheet.Mark.Words(SheetLayer.FRAME, fact.value(), new Sheet.Point(x, y), settings.text().body(), 0,
                    StrokeText.Anchor.START, StrokeText.Baseline.ALPHABETIC, Sheet.Weight.REGULAR), cover.primary()));
        }
        return page;
    }

    /** The contents, each sheet by its number. A document is numbered from its cover, so its contents count themselves in. */
    List<SheetPage> contents(List<SheetPage> body, String revision) {
        List<SheetPage> made = new ArrayList<>();
        int pagesOfContents = 1;
        while (true) {
            made.clear();
            SheetFlow flow = new SheetFlow(settings, paper.width() - 2 * settings.margin() - 2 * settings.columnGap(), () -> {
                SheetPage page = new SheetPage("contents", settings.words().contentsSheet(), settings, paper, "", lettering, revision, SheetPurpose.DOCUMENT);
                made.add(page);
                return page;
            });
            List<List<String>> rows = new ArrayList<>();
            int number = 1 + pagesOfContents;
            for (SheetPage page : body) {
                number++;
                rows.add(List.of(String.valueOf(number), page.title(), page.paper().name()));
            }
            flow.table(new SheetTable(settings.words().contentsSheet(), List.of(new SheetTable.Column(settings.words().titleSheet(), 1, true),
                    new SheetTable.Column(settings.words().titleDrawing(), 12, false), new SheetTable.Column(settings.words().titlePaper(), 1.4, false)), rows));
            if (made.size() == pagesOfContents) {
                return List.copyOf(made);
            }
            pagesOfContents = made.size();
        }
    }

    /** The largest height up to the one asked at which the words still fit across. */
    private double fittedHeight(String words, double height, double wide) {
        double width = lettering.widthOf(words, height);
        return width <= wide ? height : height * wide / width;
    }

    private static void rule(SheetPage page, double left, double right, double y, String colour) {
        page.add(new Sheet.Mark.Inked(new Sheet.Mark.Line(SheetLayer.FRAME, List.of(new Sheet.Point(left, y), new Sheet.Point(right, y)), false, 0.25), colour));
    }

    /** The product's mark, as the product draws it: its two strokes and its dot. */
    private void mark(SheetPage page, double x, double y, double size) {
        double factor = size / cover.markBox();
        filled(page, cover.markPrimary(), x, y, factor, cover.primary());
        filled(page, cover.markAccent(), x, y, factor, cover.accent());
        List<Sheet.Point> dot = new ArrayList<>();
        for (int step = 0; step < DOT_STEPS; step++) {
            double angle = FULL_TURN * step / DOT_STEPS;
            dot.add(placed(cover.markDot().x() + cover.markDot().radius() * Math.cos(angle), cover.markDot().y() + cover.markDot().radius() * Math.sin(angle), x, y, factor));
        }
        page.add(new Sheet.Mark.Inked(new Sheet.Mark.Area(SheetLayer.FRAME, List.of(dot)), cover.accent()));
    }

    private void filled(SheetPage page, String path, double x, double y, double factor, String colour) {
        for (List<double[]> run : PathPolylines.of(PATHS.parse(path), MARK_STEPS)) {
            page.add(new Sheet.Mark.Inked(new Sheet.Mark.Area(SheetLayer.FRAME, List.of(run.stream().map(point -> placed(point[0], point[1], x, y, factor)).toList())), colour));
        }
    }

    /** A point of the mark, whose Y runs down, on paper, whose Y runs up. */
    private Sheet.Point placed(double markX, double markY, double x, double y, double factor) {
        return new Sheet.Point(x + markX * factor, y + (cover.markBox() - markY) * factor);
    }
}
