package org.jmouse.cad.exchange.imported;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The bus lines of another product's sheet, as every import reads them: stretches that meet end to end made one line, and
 * what the product writes on one — {@code D[0..7]}, {@code {SDA SCL}}, {@code D[0..7],CLK} — made a name and its nets. A bus joins
 * nothing by itself, so the circuit's connections do not depend on what is read here.
 */
public final class ImportedBuses {

    private static final Pattern RANGE = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\[(\\d+)\\.\\.(\\d+)]");

    /** What a word of a name may not hold, on the way to being a word of the language. */
    private static final Pattern NOT_A_WORD = Pattern.compile("[^A-Za-z0-9_]+");

    /** The name of a bus nobody named. */
    private static final String UNNAMED = "BUS";

    /** How near, in the product's own units, two ends must stand to be one corner. */
    private static final double SAME_POINT = 1e-6;

    private ImportedBuses() {
    }

    /** Stretches, each a line of points, joined where one's end is another's: every line that results, whole. */
    public static List<List<double[]>> joined(List<List<double[]>> stretches) {
        List<List<double[]>> lines = new ArrayList<>();
        for (List<double[]> stretch : stretches) {
            lines.add(new ArrayList<>(stretch));
        }
        boolean joining = true;
        while (joining) {
            joining = false;
            for (int first = 0; first < lines.size() && !joining; first++) {
                for (int second = first + 1; second < lines.size() && !joining; second++) {
                    List<double[]> whole = joinedPair(lines.get(first), lines.get(second));
                    if (whole != null) {
                        lines.set(first, whole);
                        lines.remove(second);
                        joining = true;
                    }
                }
            }
        }
        return lines;
    }

    private static List<double[]> joinedPair(List<double[]> first, List<double[]> second) {
        List<double[]> reversedSecond = reversed(second);
        if (same(first.getLast(), second.getFirst())) {
            return continued(first, second);
        }
        if (same(first.getLast(), second.getLast())) {
            return continued(first, reversedSecond);
        }
        if (same(first.getFirst(), second.getLast())) {
            return continued(second, first);
        }
        if (same(first.getFirst(), second.getFirst())) {
            return continued(reversedSecond, first);
        }
        return null;
    }

    private static List<double[]> reversed(List<double[]> line) {
        List<double[]> reversed = new ArrayList<>(line);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private static List<double[]> continued(List<double[]> line, List<double[]> rest) {
        List<double[]> whole = new ArrayList<>(line);
        whole.addAll(rest.subList(1, rest.size()));
        return whole;
    }

    private static boolean same(double[] first, double[] second) {
        return Math.hypot(first[0] - second[0], first[1] - second[1]) < SAME_POINT;
    }

    /** Whether a point stands on a bus line: on one of its stretches, an end included. */
    public static boolean liesOn(double[] point, List<double[]> line) {
        SheetTracing.Point at = new SheetTracing.Point(point[0], point[1]);
        for (int index = 1; index < line.size(); index++) {
            double[] start = line.get(index - 1);
            double[] end = line.get(index);
            if (at.liesOn(new SheetTracing.Point(start[0], start[1]), new SheetTracing.Point(end[0], end[1]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * What a product writes on a bus line: the text of the first of its labels that stands on the line, as KiCad and Altium name a
     * bus — a label on it, not a name of its own —; null when none does.
     *
     * @param at   where a label stands, in the product's units, as the line's points are
     * @param text what a label says
     */
    public static <Label> String writtenOn(List<double[]> line, List<Label> labels, Function<Label, double[]> at, Function<Label, String> text) {
        return labels.stream().filter(label -> liesOn(at.apply(label), line)).map(text).findFirst().orElse(null);
    }

    /** Whether a label stands on any of a sheet's bus lines: a label there names the bus and joins no net. */
    public static boolean onAnyBus(double[] point, List<List<double[]>> lines) {
        return lines.stream().anyMatch(line -> liesOn(point, line));
    }

    /**
     * A bus line as the sheet takes it, from what the product writes on it.
     *
     * @param id      its reference on the sheet
     * @param written what the product writes on it; null for nothing
     * @param via     its corners, as the sheet draws them
     * @param netOf   the net a member is, as the import names its nets
     */
    public static ImportedCircuit.Bus bus(String id, String written, List<double[]> via, UnaryOperator<String> netOf) {
        String words = written == null ? "" : written.trim().replaceAll("^[{]|[}]$", "");
        // An Eagle name may open with the design's prefix: `B$1:D[0..7]`.
        String named = words.contains(":") ? words.substring(words.lastIndexOf(':') + 1) : words;
        List<String> members = new ArrayList<>();
        String name = null;
        for (String part : named.split("[,\\s]+")) {
            if (part.isEmpty()) {
                continue;
            }
            Matcher range = RANGE.matcher(part);
            if (range.matches()) {
                name = name == null ? range.group(1) : name;
                int from = Integer.parseInt(range.group(2));
                int to = Integer.parseInt(range.group(3));
                int step = from <= to ? 1 : -1;
                for (int number = from; number != to + step; number += step) {
                    members.add(netOf.apply(range.group(1) + number));
                }
            } else {
                name = name == null ? part : name;
                members.add(netOf.apply(part));
            }
        }
        // A word alone names a bus and says nothing of its nets, as a sheet's own bus does.
        if (members.size() == 1 && !RANGE.matcher(named.trim()).matches()) {
            members.clear();
        }
        return new ImportedCircuit.Bus(id, name == null ? UNNAMED : NOT_A_WORD.matcher(name).replaceAll("_"), members, via);
    }
}
