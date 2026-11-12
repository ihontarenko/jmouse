package org.jmouse.cad.plugin.drawings;

import org.jmouse.strokefont.StrokeText;

import java.util.List;

/** What makes pages one document: each one's title block, numbered among them all, whichever kind of drawing it holds. */
final class SheetTitles {

    /** How many rows a title block is divided into. */
    private static final int ROWS = 4;

    private final SheetSettings       settings;
    private final SheetSettings.Words words;
    private final String              title;

    /** @param title what the whole document is called */
    SheetTitles(SheetSettings settings, String title) {
        this.settings = settings;
        this.words = settings.words();
        this.title = title;
    }

    List<Sheet> titled(List<SheetPage> pages) {
        for (int index = 0; index < pages.size(); index++) {
            if (pages.get(index).purpose() != SheetPurpose.COVER) {
                titleBlock(pages.get(index), index + 1, pages.size());
            }
        }
        return pages.stream().map(SheetPage::sheet).toList();
    }

    private void titleBlock(SheetPage page, int number, int count) {
        SheetSettings.Paper paper = page.paper();
        double left = SheetFlow.rightColumn(settings, paper);
        double right = paper.width() - settings.margin();
        double bottom = settings.margin();
        double top = bottom + settings.titleHeight();
        page.box(SheetLayer.FRAME, left, bottom, right, top);
        double padding = settings.text().label();
        double labelHeight = settings.text().label();
        double rowHeight = settings.titleHeight() / ROWS;
        page.words(SheetLayer.FRAME, title, left + padding, top - rowHeight * 0.5, settings.text().title(), StrokeText.Anchor.START, StrokeText.Baseline.MIDDLE);
        page.line(SheetLayer.FRAME, left, top - rowHeight, right, top - rowHeight);
        page.words(SheetLayer.FRAME, page.title(), left + padding, top - rowHeight * 1.5, settings.text().body(), StrokeText.Anchor.START, StrokeText.Baseline.MIDDLE);
        page.line(SheetLayer.FRAME, left, top - rowHeight * 2, right, top - rowHeight * 2);
        double middle = (left + right) / 2;
        page.line(SheetLayer.FRAME, middle, top - rowHeight * 2, middle, bottom);
        page.line(SheetLayer.FRAME, left, top - rowHeight * 3, right, top - rowHeight * 3);
        field(page, left + padding, top - rowHeight * 2.5, words.titleRevision(), page.revision(), labelHeight);
        field(page, middle + padding, top - rowHeight * 2.5, words.titleSheet(), number + " " + words.titleOf() + " " + count, labelHeight);
        field(page, left + padding, top - rowHeight * 3.5, words.titleScale(), page.scale(), labelHeight);
        field(page, middle + padding, top - rowHeight * 3.5, words.titlePaper(), paper.name(), labelHeight);
    }

    private static void field(SheetPage page, double x, double y, String label, String value, double height) {
        // A sheet of tables has no scale: an empty cell says so better than a word with nothing after it.
        if (value.isBlank()) {
            return;
        }
        String labelled = label + ": ";
        page.words(SheetLayer.FRAME, labelled, x, y, height, StrokeText.Anchor.START, StrokeText.Baseline.MIDDLE);
        page.words(SheetLayer.FRAME, value, x + page.widthOf(labelled, height), y, height, StrokeText.Anchor.START, StrokeText.Baseline.MIDDLE);
    }
}
