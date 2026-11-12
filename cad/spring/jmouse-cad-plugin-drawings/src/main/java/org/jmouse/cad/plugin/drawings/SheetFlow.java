package org.jmouse.cad.plugin.drawings;

import org.jmouse.strokefont.StrokeText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tables and notes on sheets of their own, running down columns of a width asked for, left to right, then on to a further sheet.
 * A table too long for what is left goes on in the next column, its column titles again above it. Room is left for the title
 * block along every sheet's foot.
 */
final class SheetFlow {

    private final SheetSettings       settings;
    private final double              width;
    private final Supplier<SheetPage> nextPage;
    private SheetPage                 page;
    private double                    left;
    private double                    top;
    private double                    bottom;

    /** @param nextPage a further sheet, as the flow comes to need one; none is asked until something is laid */
    SheetFlow(SheetSettings settings, double width, Supplier<SheetPage> nextPage) {
        this.settings = settings;
        this.width = width;
        this.nextPage = nextPage;
    }

    /** Where a sheet's title block begins. */
    static double rightColumn(SheetSettings settings, SheetSettings.Paper paper) {
        return paper.width() - settings.margin() - settings.columnWidth();
    }

    void table(SheetTable table) {
        begin();
        double[] columns = columnWidths(table);
        double rowHeight = settings.rowHeight();
        double firstRow = table.rows().isEmpty() ? 0 : linesOf(table.rows().getFirst(), columns) * rowHeight;
        // A heading stays with its titles and its first row.
        heading(table.heading(), rowHeight * 2 + firstRow);
        titles(table, columns);
        for (List<String> row : table.rows()) {
            double height = linesOf(row, columns) * rowHeight;
            if (top - height < bottom) {
                nextColumn();
                titles(table, columns);
            }
            drawRow(table, row, columns, height, settings.text().body());
            top -= height;
        }
        top -= settings.columnGap() * 2;
    }

    /** Paragraphs, wrapped to the column. */
    void notes(String heading, List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            return;
        }
        begin();
        double height = settings.text().body();
        double step = height * 1.75;
        heading(heading, step);
        int number = 1;
        for (String paragraph : paragraphs) {
            for (String line : wrapped(number + ". " + paragraph, width, height)) {
                if (top - step < bottom) {
                    nextColumn();
                }
                page.words(SheetLayer.NOTES, line, left, top - height, height, StrokeText.Anchor.START, StrokeText.Baseline.ALPHABETIC);
                top -= step;
            }
            number++;
            top -= step * 0.35;
        }
        top -= settings.columnGap() * 2;
    }

    private void begin() {
        if (page == null) {
            startPage();
        }
    }

    private void heading(String heading, double keptWithIt) {
        if (heading.isBlank()) {
            return;
        }
        double height = settings.text().heading();
        if (top - height * 2 - keptWithIt < bottom) {
            nextColumn();
        }
        page.add(new Sheet.Mark.Words(SheetLayer.TABLES, heading, new Sheet.Point(left, top - height), height, 0, StrokeText.Anchor.START, StrokeText.Baseline.ALPHABETIC, Sheet.Weight.BOLD));
        top -= height * 2.2;
    }

    private void titles(SheetTable table, double[] columns) {
        List<String> titles = table.columns().stream().map(SheetTable.Column::title).toList();
        double height = settings.rowHeight() * linesOf(titles, columns);
        if (top - height < bottom) {
            nextColumn();
        }
        drawRow(table, titles, columns, height, settings.text().label());
        top -= height;
        double rule = settings.penOf(SheetLayer.FRAME).pen();
        page.add(new Sheet.Mark.Line(SheetLayer.TABLES, List.of(new Sheet.Point(left, top), new Sheet.Point(left + width, top)), false, rule));
    }

    private void drawRow(SheetTable table, List<String> row, double[] columns, double height, double textHeight) {
        double x = left;
        double rowHeight = settings.rowHeight();
        double padding = settings.text().label() * 0.5;
        for (int column = 0; column < columns.length; column++) {
            boolean numeric = table.columns().get(column).numeric();
            String cell = column < row.size() ? row.get(column) : "";
            double baseline = top - (rowHeight + textHeight) / 2;
            for (String line : wrapped(cell, columns[column] - padding * 2, textHeight)) {
                page.words(SheetLayer.TABLES, line, numeric ? x + columns[column] - padding : x + padding, baseline, textHeight,
                        numeric ? StrokeText.Anchor.END : StrokeText.Anchor.START, StrokeText.Baseline.ALPHABETIC);
                baseline -= rowHeight;
            }
            x += columns[column];
        }
        page.line(SheetLayer.TABLES, left, top - height, left + width, top - height);
    }

    private int linesOf(List<String> row, double[] columns) {
        double padding = settings.text().label();
        int lines = 1;
        for (int column = 0; column < Math.min(columns.length, row.size()); column++) {
            lines = Math.max(lines, wrapped(row.get(column), columns[column] - padding, settings.text().body()).size());
        }
        return lines;
    }

    private double[] columnWidths(SheetTable table) {
        double shares = table.columns().stream().mapToDouble(SheetTable.Column::share).sum();
        return table.columns().stream().mapToDouble(column -> column.share() / shares * width).toArray();
    }

    /** Words broken at their spaces into lines no wider than asked; a single word too long is broken too, never cut. */
    private List<String> wrapped(String words, double wide, double height) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words.split(" ")) {
            String longer = line.isEmpty() ? word : line + " " + word;
            if (page.widthOf(longer, height) <= wide) {
                line = new StringBuilder(longer);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            line = new StringBuilder(brokenInto(lines, word, wide, height));
        }
        lines.add(line.toString());
        return lines;
    }

    /** A word laid into lines as far as it needs: every full line is added, and what is left to go on with returned. */
    private String brokenInto(List<String> lines, String word, double wide, double height) {
        String remaining = word;
        while (page.widthOf(remaining, height) > wide && remaining.length() > 1) {
            int fits = remaining.length() - 1;
            while (fits > 1 && page.widthOf(remaining.substring(0, fits), height) > wide) {
                fits--;
            }
            lines.add(remaining.substring(0, fits));
            remaining = remaining.substring(fits);
        }
        return remaining;
    }

    private void nextColumn() {
        double next = left + width + settings.columnGap();
        if (next + width <= page.paper().width() - settings.margin()) {
            left = next;
            top = page.paper().height() - settings.margin() - settings.columnGap();
            return;
        }
        startPage();
    }

    private void startPage() {
        page = nextPage.get();
        left = settings.margin() + settings.columnGap();
        top = page.paper().height() - settings.margin() - settings.columnGap();
        bottom = settings.margin() + settings.titleHeight() + settings.columnGap();
    }
}
