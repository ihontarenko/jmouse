package org.jmouse.cad.plugin.drawings;

import java.util.List;

/**
 * A table on a sheet: a heading, column titles and rows of words, a cell too long for its column wrapping onto more lines.
 *
 * @param heading what it is called; empty for none
 * @param columns each with its share of the width
 */
public record SheetTable(String heading, List<Column> columns, List<List<String>> rows) {

    public SheetTable {
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
    }

    /**
     * @param share   its part of the table's width, against the others' shares
     * @param numeric whether its words are numbers, which stand to the right
     */
    public record Column(String title, double share, boolean numeric) {
    }
}
