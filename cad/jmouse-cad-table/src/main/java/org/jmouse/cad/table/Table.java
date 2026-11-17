package org.jmouse.cad.table;

import org.jspecify.annotations.Nullable;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetField;
import org.jmouse.sheet.SheetSource;

import java.util.ArrayList;
import java.util.List;

/**
 * What a table document is to a reader: its lines as a reader sees them — each field's name and value as written, each entry's
 * id and title with its own lines —, which is what a shelf of the library lists and an inspector shows. A program reads the
 * sheet they make from the document's tree ({@link TableModule#sheetOf}).
 *
 * @param lines its lines, in order: the fields, then the entries
 */
public record Table(List<Line> lines) {

    /**
     * One line of a table as a reader sees it.
     *
     * @param name   a field's name, or an entry's id
     * @param title  an entry's title; null for a field and for an entry with none
     * @param value  a field's value as written; null for an entry
     * @param source where in the original it was read, as written; null when it says none
     * @param lines  an entry's own lines; none for a field
     */
    public record Line(String name, @Nullable String title, @Nullable String value, @Nullable String source, List<Line> lines) {

        public Line {
            lines = List.copyOf(lines);
        }
    }

    public Table {
        lines = List.copyOf(lines);
    }

    public static Table of(Sheet sheet) {
        return new Table(linesOf(sheet.fields(), sheet.entries()));
    }

    private static List<Line> linesOf(List<SheetField> fields, List<SheetEntry> entries) {
        List<Line> lines = new ArrayList<>();
        for (SheetField field : fields) {
            lines.add(new Line(field.name(), null, field.value().written(), field.source().map(SheetSource::written).orElse(null), List.of()));
        }
        for (SheetEntry entry : entries) {
            lines.add(new Line(entry.id(), entry.title().orElse(null), null, entry.source().map(SheetSource::written).orElse(null),
                    linesOf(entry.fields(), entry.entries())));
        }
        return lines;
    }
}
