package org.jmouse.cad.spice;

import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xyce's way: no control block — {@code .options device} for the temperature, {@code .options output initial_interval}
 * for points on the even grid (Xyce prints its own steps otherwise), and {@code .print tran format=csv v(*) i(*)}, which
 * Xyce writes as a CSV file the process hands back on its output: a header {@code TIME,V(OUT),I(V1)}, one row a point.
 */
public final class XyceDialect implements SpiceDialect {

    /** The column Xyce prints the time in. */
    private static final String TIME = "time";

    /** A vector's heading: what it measures, then what of — {@code V(OUT)}, {@code I(V1)}. */
    private static final Pattern VECTOR = Pattern.compile("([vi])\\((.+)\\)");

    @Override
    public String program() {
        return "Xyce";
    }

    @Override
    public List<SpiceStatement> runAndPrint(double step, double celsius) {
        return List.of(
                new SpiceCard.Directive("options", List.of(word("device"), assignment("temp", celsius),
                        assignment("tnom", celsius)), SpicePosition.NONE),
                new SpiceCard.Directive("options", List.of(word("output"), assignment("initial_interval", step)),
                        SpicePosition.NONE),
                new SpiceCard.Directive("print", List.of(word("tran"), new SpiceArgument.Assignment("format", word("csv"),
                        SpicePosition.NONE), word("v(*)"), word("i(*)")), SpicePosition.NONE));
    }

    @Override
    public PrintedVectors read(String output) {
        List<String> lines = output.lines().map(String::strip).toList();
        int header = -1;
        for (int line = 0; line < lines.size(); line++) {
            if (lines.get(line).toLowerCase(Locale.ROOT).startsWith(TIME + ",")) {
                header = line;
                break;
            }
        }
        if (header < 0) {
            throw new SpiceRunException("Xyce printed no run: " + tail(lines));
        }
        List<String> names = List.of(lines.get(header).toLowerCase(Locale.ROOT).split(","));
        List<double[]> rows = new ArrayList<>();
        // Its log follows the table on the same output: the table ends at the first line that is not a row of it.
        for (int line = header + 1; line < lines.size(); line++) {
            double[] row = rowOf(lines.get(line), names.size());
            if (row == null) {
                break;
            }
            rows.add(row);
        }
        if (rows.isEmpty()) {
            throw new SpiceRunException("Xyce's run printed no point: " + tail(lines));
        }
        Map<String, double[]> nodes = new LinkedHashMap<>();
        Map<String, double[]> branches = new LinkedHashMap<>();
        double[] time = column(rows, 0);
        for (int column = 1; column < names.size(); column++) {
            Matcher vector = VECTOR.matcher(names.get(column).strip());
            if (!vector.matches()) {
                continue;
            }
            Map<String, double[]> into = vector.group(1).equals("v") ? nodes : branches;
            into.putIfAbsent(vector.group(2), column(rows, column));
        }
        return new PrintedVectors(time, nodes, branches);
    }

    /** One row of the table, or null for a line that is none. */
    private static double[] rowOf(String line, int columns) {
        String[] cells = line.split(",");
        if (cells.length != columns) {
            return null;
        }
        double[] row = new double[columns];
        for (int cell = 0; cell < columns; cell++) {
            try {
                row[cell] = Double.parseDouble(cells[cell].strip());
            } catch (NumberFormatException notARow) {
                return null;
            }
        }
        return row;
    }

    private static double[] column(List<double[]> rows, int column) {
        double[] values = new double[rows.size()];
        for (int row = 0; row < rows.size(); row++) {
            values[row] = rows.get(row)[column];
        }
        return values;
    }

    /** The end of what Xyce said: why it stopped is last. */
    private static String tail(List<String> lines) {
        List<String> said = lines.stream().filter(line -> !line.isEmpty()).toList();
        return String.join(" | ", said.subList(Math.max(0, said.size() - 6), said.size()));
    }

    private static SpiceArgument word(String text) {
        return new SpiceArgument.Word(text, SpicePosition.NONE);
    }

    private static SpiceArgument assignment(String name, double value) {
        return new SpiceArgument.Assignment(name, word(Quantity.plain(value).written()), SpicePosition.NONE);
    }
}
