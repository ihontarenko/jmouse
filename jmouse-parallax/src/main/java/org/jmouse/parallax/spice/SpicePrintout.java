package org.jmouse.parallax.spice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What ngspice printed, read back: the run's control block marks each analysis with an {@code echo} of its own
 * ({@link #MARKER}), so every section is found by its name, never by its position among ngspice's own chatter. A section
 * holds either an operating point — {@code name = value} lines — or a table: a header that opens with {@code Index}, one
 * row a point, a complex value written as two cells, the first ending in a comma.
 */
final class SpicePrintout {

    /** What opens a section: the control block echoes it with the section's name after it. */
    static final String MARKER = "@@parallax";

    private static final Pattern VALUE = Pattern.compile("^\\s*(\\S+)\\s*=\\s*(\\S+)\\s*$");
    private static final String INDEX = "Index";

    private final Map<String, List<String>> sections = new LinkedHashMap<>();
    private final String printout;

    private SpicePrintout(String printout) {
        this.printout = printout;
        List<String> current = null;
        for (String line : printout.split("\\R")) {
            String text = line.strip();
            if (text.startsWith(MARKER)) {
                current = new ArrayList<>();
                sections.put(text.substring(MARKER.length()).strip(), current);
            } else if (current != null && !text.isEmpty()) {
                current.add(text);
            }
        }
    }

    static SpicePrintout of(String printout) {
        return new SpicePrintout(printout);
    }

    /** A section's {@code name = value} lines, in the order printed. */
    Map<String, Double> values(String section) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (String line : lines(section)) {
            Matcher value = VALUE.matcher(line);
            if (value.matches()) {
                values.put(value.group(1), number(value.group(2), line));
            }
        }
        if (values.isEmpty()) {
            throw new SpiceRunException("ngspice printed no values for " + section + ": " + tail());
        }
        return values;
    }

    /** A section's table: its columns by name, each a real column or a complex one, every row a point. */
    Table table(String section) {
        List<String> lines = lines(section);
        int header = -1;
        for (int line = 0; line < lines.size(); line++) {
            if (lines.get(line).startsWith(INDEX)) {
                header = line;
                break;
            }
        }
        if (header < 0) {
            throw new SpiceRunException("ngspice printed no table for " + section + ": " + tail());
        }
        String[] headings = lines.get(header).split("\\s+");
        // The first heading is the row's index, not a column.
        List<String> names = List.of(headings).subList(1, headings.length);
        List<double[][]> rows = new ArrayList<>();
        for (int line = header + 1; line < lines.size(); line++) {
            String text = lines.get(line);
            if (!Character.isDigit(text.charAt(0))) {
                continue;
            }
            String[] cells = text.split("\\s+");
            double[][] row = new double[names.size()][];
            int cell = 1;
            for (int column = 0; column < names.size() && cell < cells.length; column++) {
                // A complex value is two cells, the real part's ending in a comma.
                if (cells[cell].endsWith(",") && cell + 1 < cells.length) {
                    row[column] = new double[]{number(cells[cell].substring(0, cells[cell].length() - 1), text),
                            number(cells[cell + 1], text)};
                    cell += 2;
                } else {
                    row[column] = new double[]{number(cells[cell], text)};
                    cell++;
                }
            }
            if (row[names.size() - 1] != null) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            throw new SpiceRunException("ngspice's table for " + section + " is empty.");
        }
        return new Table(names, rows);
    }

    private List<String> lines(String section) {
        List<String> lines = sections.get(section);
        if (lines == null) {
            throw new SpiceRunException("ngspice did not reach " + section + ": " + tail());
        }
        return lines;
    }

    private static double number(String cell, String line) {
        try {
            return Double.parseDouble(cell);
        } catch (NumberFormatException unreadable) {
            throw new SpiceRunException("ngspice printed a value that is not a number: " + line, unreadable);
        }
    }

    /** The end of the printout: ngspice says why it stopped last. */
    private String tail() {
        String[] lines = printout.strip().split("\\R");
        return String.join(" | ", List.of(lines).subList(Math.max(0, lines.length - 4), lines.length)).strip();
    }

    /**
     * A printed table: one of its columns is the axis — {@code time}, {@code frequency}, the swept value {@code v-sweep}
     * —, found by its name: ngspice prints a linearized run's columns in alphabetical order, {@code time} among them.
     *
     * @param rows each point's cells by column: one number, or a real and an imaginary part
     */
    record Table(List<String> names, List<double[][]> rows) {

        Table {
            names = List.copyOf(names);
            rows = List.copyOf(rows);
        }

        /**
         * Where the axis is: the first column named one of these.
         *
         * @throws SpiceRunException when none is
         */
        int axisColumn(List<String> axes) {
            for (int column = 0; column < names.size(); column++) {
                if (axes.contains(names.get(column).toLowerCase(Locale.ROOT))) {
                    return column;
                }
            }
            throw new SpiceRunException("ngspice's table has no " + String.join(" or ", axes) + " column: " + names);
        }

        /** A column's real values. */
        float[] real(int column) {
            float[] values = new float[rows.size()];
            for (int row = 0; row < rows.size(); row++) {
                values[row] = (float) rows.get(row)[column][0];
            }
            return values;
        }

        /** Whether a column holds complex values. */
        boolean complex(int column) {
            return rows.getFirst()[column].length == 2;
        }

        /** A complex column's value at a row, as its real and imaginary parts. */
        double[] at(int row, int column) {
            return rows.get(row)[column];
        }

        int size() {
            return rows.size();
        }
    }
}
