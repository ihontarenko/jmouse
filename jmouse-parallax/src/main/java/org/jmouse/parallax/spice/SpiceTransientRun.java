package org.jmouse.parallax.spice;

import org.jmouse.parallax.Dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A transient run by SPICE, as the series the engine's own transient is: the deck gets a {@code .control} block that
 * runs it, puts the solution on the printing step's even grid ({@code linearize}) and prints every vector in one table;
 * the table is read back as columns against time.
 *
 * <p>Columns are named as ngspice names them, which is lower case: a node's voltage by the node ({@code out}, V), a
 * branch current by its element ({@code v1#branch} → {@code V1}, A).</p>
 */
final class SpiceTransientRun {

    private static final String CONTROL = """
            .control
            set nobreak
            set width=100000
            set numdgt=7
            run
            linearize
            print all
            .endc
            """;

    private static final String TIME    = "time";
    private static final String BRANCH  = "#branch";
    private static final Pattern END_CARD = Pattern.compile("(?im)^[ \\t]*\\.end[ \\t]*$");

    private SpiceTransientRun() {
    }

    /**
     * The deck with the block that runs it and prints the result, before its {@code .end} — the card alone on its line,
     * not the {@code .ends} or {@code .endl} a text search for it also finds.
     */
    static String controlled(String deck) {
        Matcher end = END_CARD.matcher(deck);
        int last = -1;
        while (end.find()) {
            last = end.start();
        }
        String body = last >= 0 ? deck.substring(0, last) : deck + "\n";
        return body + CONTROL + ".end\n";
    }

    /** The printed table as a series against time. */
    static Dataset.TimeSeries series(String printout) {
        String[] lines = printout.split("\\R");
        int header = -1;
        for (int line = 0; line < lines.length; line++) {
            if (lines[line].startsWith("Index")) {
                header = line;
                break;
            }
        }
        if (header < 0) {
            throw new SpiceRunException("ngspice printed no table: " + tail(lines));
        }
        String[] names = lines[header].trim().split("\\s+");
        int timeColumn = List.of(names).indexOf(TIME);
        if (timeColumn < 0) {
            throw new SpiceRunException("ngspice's table has no time column.");
        }
        List<double[]> rows = new ArrayList<>();
        for (int line = header + 1; line < lines.length; line++) {
            String text = lines[line].trim();
            if (text.isEmpty() || !Character.isDigit(text.charAt(0))) {
                continue;
            }
            String[] cells = text.split("\\s+");
            if (cells.length != names.length) {
                continue;
            }
            double[] row = new double[cells.length];
            for (int cell = 0; cell < cells.length; cell++) {
                try {
                    row[cell] = Double.parseDouble(cells[cell]);
                } catch (NumberFormatException unreadable) {
                    throw new SpiceRunException("ngspice's table holds a value that is not a number: " + text, unreadable);
                }
            }
            rows.add(row);
        }
        if (rows.isEmpty()) {
            throw new SpiceRunException("ngspice's table is empty.");
        }
        float[] times = column(rows, timeColumn);
        List<String> columns = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        List<String> units = new ArrayList<>();
        for (int index = 1; index < names.length; index++) {
            String name = names[index];
            // The time is repeated in every group of a wide table; it is the axis, not a column.
            if (name.equals(TIME)) {
                continue;
            }
            boolean branch = name.endsWith(BRANCH);
            columns.add(branch ? name.substring(0, name.length() - BRANCH.length()).toUpperCase(Locale.ROOT) : name);
            values.add(column(rows, index));
            units.add(branch ? "A" : "V");
        }
        return new Dataset.TimeSeries("t", times, columns, values, units);
    }

    private static float[] column(List<double[]> rows, int index) {
        float[] values = new float[rows.size()];
        for (int row = 0; row < rows.size(); row++) {
            values[row] = (float) rows.get(row)[index];
        }
        return values;
    }

    private static String tail(String[] lines) {
        return String.join(" | ", List.of(lines).subList(Math.max(0, lines.length - 4), lines.length)).strip();
    }
}
