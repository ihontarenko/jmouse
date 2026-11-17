package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.Index;
import org.jmouse.cad.circuit.Bus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The bus lines of another product's sheet, as every import reads them: stretches that meet end to end made one line, and
 * what the product writes on one — {@code D[0..7]}, {@code {SDA SCL}}, {@code D[0..7],CLK} — made a name and its nets. A bus joins
 * nothing by itself, so the circuit's connections do not depend on what is read here. One reading per sheet collects the buses
 * the sheet declares — each bus once, by its name, whatever lines draw it — beside the lines that name them.
 */
public final class ImportedBuses {

    /** The sheet's buses as declared so far, by name: each the nets it carries, in its order. */
    private final Map<String, List<String>> declared = new LinkedHashMap<>();

    private static final Pattern RANGE = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\[(\\d+)\\.\\.(\\d+)]");

    /** What a word of a name may not hold, on the way to being a word of the language. */
    private static final Pattern NOT_A_WORD = Pattern.compile("[^A-Za-z0-9_]+");

    /** The name of a bus nobody named. */
    private static final String UNNAMED = "BUS";

    /** How near, in the product's own units, two ends must stand to be one corner. */
    private static final double SAME_POINT = 1e-6;

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
     * A bus line whose name and nets the product's own reading gave — a format whose bus syntax is its own, such as KiCad's
     * {@code IRQ-[1..7]} —, its nets as the sheet names them: the line, the bus it draws declared on the sheet.
     *
     * @param id      its reference on the sheet
     * @param name    what the product calls it; blank for nothing
     * @param members its nets, as the sheet names them; empty when the product names it and says nothing of its nets
     * @param via     its corners, as the sheet draws them
     */
    public ImportedCircuit.BusLine line(String id, String name, List<String> members, List<double[]> via) {
        String bus = declare(name, members);
        return new ImportedCircuit.BusLine(id, bus, via);
    }

    /**
     * A bus line as the sheet takes it, from what the product writes on it — {@code D[0..7]}, {@code {SDA SCL}},
     * {@code D[0..7],CLK}, an Eagle {@code B$1:D[0..7]} —: the line, the bus it draws declared on the sheet.
     *
     * @param id      its reference on the sheet
     * @param written what the product writes on it; null for nothing
     * @param via     its corners, as the sheet draws them
     * @param netOf   the net a member is, as the import names its nets
     */
    public ImportedCircuit.BusLine line(String id, String written, List<double[]> via, UnaryOperator<String> netOf) {
        return new ImportedCircuit.BusLine(id, declareWritten(written, netOf), via);
    }

    /**
     * Declares the bus a product writes — {@code D[0..7]}, {@code {SDA SCL}}, {@code D[0..7],CLK}, an Eagle {@code B$1:D[0..7]} — on a
     * line, a port or a pin: its name as the circuit writes it. A word alone names a bus and says nothing of its nets.
     *
     * @param netOf the net a member is, as the import names its nets
     */
    public String declareWritten(String written, UnaryOperator<String> netOf) {
        Written read = read(written);
        return declare(read.name(), read.members().stream().map(netOf).toList());
    }

    /** Whether what a product writes on a port or a label names a bus — a range, a group, several nets — rather than a net. */
    public static boolean namesBus(String written) {
        return !read(written).members().isEmpty();
    }

    /** What a product writes for a bus, read: its name, and its members as the product names them. */
    private record Written(String name, List<String> members) {
    }

    private static Written read(String written) {
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
                    members.add(range.group(1) + number);
                }
            } else {
                name = name == null ? part : name;
                members.add(part);
            }
        }
        // A word alone names a bus and says nothing of its nets, as a sheet's own bus does.
        if (members.size() == 1 && !RANGE.matcher(named.trim()).matches()) {
            members.clear();
        }
        return new Written(name == null ? "" : name, members);
    }

    /**
     * Declares a bus of the sheet, by its name made a word of the language: the same name and the same nets again are the same bus,
     * as two lines of one bus are; the same name over other nets is another bus — KiCad's `DQ[0..15]` beside `DQ[0..31]` —, named
     * apart, `DQ_2`. A name with no net declares nothing: it names the first bus of the name, or one the sheet never says the nets
     * of, which the sheet's checks report.
     *
     * @return the bus's name as the circuit writes it
     */
    public String declare(String name, List<String> members) {
        String word = wordOf(name);
        if (members.isEmpty()) {
            return word;
        }
        List<String> carried = List.copyOf(new LinkedHashSet<>(members));
        String candidate = word;
        for (int number = 2; declared.containsKey(candidate) && !declared.get(candidate).equals(carried); number++) {
            candidate = word + "_" + number;
        }
        declared.putIfAbsent(candidate, carried);
        return candidate;
    }

    /** The name a bus of the product is in the language: what a name may not hold made an underscore, never empty. */
    public static String wordOf(String name) {
        String word = NOT_A_WORD.matcher(name == null ? "" : name).replaceAll("_").replaceAll("^_+|_+$", "");
        if (word.isEmpty()) {
            return UNNAMED;
        }
        return Character.isLetter(word.charAt(0)) ? word : UNNAMED + "_" + word;
    }

    /**
     * Every bus the sheet declared: a vector where its nets are the language's indexed names of it, {@code D_0} … {@code D_7} in
     * order — written {@code bus(D[0..7])} —, a group of its nets otherwise.
     */
    public List<Bus> buses() {
        List<Bus> buses = new ArrayList<>();
        declared.forEach((name, members) -> buses.add(vectorOf(name, members).orElseGet(() -> Bus.group(name, members))));
        return buses;
    }

    private static Optional<Bus> vectorOf(String name, List<String> members) {
        Matcher first = Pattern.compile(Pattern.quote(name) + "_(\\d+)").matcher(members.getFirst());
        if (!first.matches()) {
            return Optional.empty();
        }
        Index index = new Index(Integer.parseInt(first.group(1)), Integer.parseInt(first.group(1)) + members.size() - 1);
        return index.names(name).equals(members) ? Optional.of(Bus.vector(name, index)) : Optional.empty();
    }
}
