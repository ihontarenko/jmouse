package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.script.Quantity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What an {@code expect} measures, taken on a circuit — a component's bench, or a circuit that states what it does of
 * itself. The words are the line after {@code measure}: a kind, a signal, then clauses.
 *
 * <pre>
 * operating out                              — the operating point
 * final | maximum | minimum | average | rms out [after 1 ms]
 * value out at 2 ms                          — the value at a moment
 * frequency | duty out above 2.5 V [after 1 ms]
 * gain out at 1 kHz                          — small-signal gain, dB; 'phase out at 1 kHz' in degrees
 * corner out                                 — the lowest frequency the gain crosses 3 dB under its peak at
 * … with R_load = 1 kΩ, S1.closed = 1        — the circuit with other slider values or part settings
 * </pre>
 *
 * <p>A signal is a node's voltage by its name ({@code out}), the voltage between two ({@code out - reference}), a part's
 * current ({@code current V1}) or power ({@code power R1}), or a digital node's level ({@code level clock});
 * {@code final levels a, b} reads digital levels as a text. A DC sweep is a few expectations at the points that matter:
 * {@code operating out with V_in = 3 V}.</p>
 */
final class CircuitMeasurement {

    /** Time steps a run is simulated in. */
    static final int SAMPLES = 4000;

    private static final Set<String> CLAUSES = Set.of("above", "below", "after", "at", "with");

    /** A corner is looked for over this range, this many frequencies a decade, then bisected. */
    private static final double LOWEST_FREQUENCY = 1e-3;
    private static final double HIGHEST_FREQUENCY = 1e9;
    private static final int FREQUENCIES_PER_DECADE = 30;
    private static final int CORNER_BISECTIONS = 40;
    private static final double CORNER_DROP = 3;

    private final String kind;
    private static final String FIGURE = "figure";
    private static final String FIGURES = "figures";

    private final List<String> signal;
    private final Map<String, List<String>> clauses;

    private CircuitMeasurement(String kind, List<String> signal, Map<String, List<String>> clauses) {
        this.kind = kind;
        this.signal = signal;
        this.clauses = clauses;
    }

    /**
     * The display figures the measurement reads — {@code figure U1.reading}, or {@code figures U1.cell_0, U1.cell_1} —,
     * each {@code PART.name} written as one word however the line split it; none for any other signal.
     */
    private Set<String> figureNames() {
        if (signal.isEmpty() || !(signal.getFirst().equals(FIGURE) || signal.getFirst().equals(FIGURES))) {
            return Set.of();
        }
        Set<String> names = new LinkedHashSet<>();
        StringBuilder name = new StringBuilder();
        for (String word : signal.subList(1, signal.size())) {
            if (word.equals(",")) {
                names.add(name.toString());
                name.setLength(0);
            } else {
                name.append(word);
            }
        }
        names.add(name.toString());
        names.remove("");
        if (names.isEmpty() || (signal.getFirst().equals(FIGURE) && names.size() > 1)) {
            throw new IllegalArgumentException("'figure' reads one display figure, 'figure U1.reading'; several are "
                    + "'final figures U1.cell_0, U1.cell_1'");
        }
        return names;
    }

    /** A figure as a list of them is compared: a whole number without a point. */
    private static String written(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? Long.toString((long) value) : Double.toString(value);
    }

    /** The words of a {@code measure} line read into a kind, a signal and its clauses; nothing is computed yet. */
    static CircuitMeasurement of(List<String> words) {
        List<String> written = words.stream().filter(word -> !word.isBlank()).toList();
        if (written.isEmpty()) {
            throw new IllegalArgumentException("'measure' needs what to measure, for example 'measure final out'");
        }
        List<String> signal = new ArrayList<>();
        Map<String, List<String>> clauses = new HashMap<>();
        List<String> into = signal;
        for (String word : written.subList(1, written.size())) {
            // 'with' takes the rest of the line: its settings may hold any word.
            if (CLAUSES.contains(word) && !clauses.containsKey("with")) {
                if (clauses.containsKey(word)) {
                    throw new IllegalArgumentException("'" + word + "' is written twice in one measurement");
                }
                into = new ArrayList<>();
                clauses.put(word, into);
            } else {
                into.add(word);
            }
        }
        return new CircuitMeasurement(written.getFirst(), List.copyOf(signal), clauses);
    }

    /** Takes the measurement on the circuit, run over its duration in {@code samples} steps when it needs a run. */
    Measurement takeOn(CompiledObject circuit, int samples) {
        CompiledObject measured = clauses.containsKey("with") ? withSettings(circuit, clauses.get("with")) : circuit;
        return switch (kind) {
            case "operating" -> new Measurement(signal(operatingPoint(measured))[0], null);
            case "gain", "phase" -> new Measurement(smallSignal(measured), null);
            case "corner" -> new Measurement(corner(measured), null);
            case "final", "maximum", "minimum", "average", "rms", "value", "frequency", "duty" ->
                    overTheRun(TransientRun.of(measured, samples, true, figureNames(), ComputationProgress.NONE));
            default -> throw new IllegalArgumentException("'" + kind + "' is not a measurement: operating, final, maximum, "
                    + "minimum, average, rms, value, frequency, duty, gain, phase, corner");
        };
    }

    private Measurement overTheRun(TransientRun run) {
        double[] times = run.times();
        if (kind.equals("final") && !signal.isEmpty() && signal.getFirst().equals(FIGURES)) {
            List<String> shown = new ArrayList<>();
            for (String name : figureNames()) {
                double[] values = run.figure(name);
                shown.add(written(values[values.length - 1]));
            }
            return new Measurement(Double.NaN, "[" + String.join(",", shown) + "]");
        }
        if (kind.equals("final") && !signal.isEmpty() && signal.getFirst().equals("levels")) {
            Map<String, LogicLevel> last = run.levels().getLast();
            List<String> levels = new ArrayList<>();
            for (String node : signal.subList(1, signal.size())) {
                if (!node.equals(",")) {
                    levels.add(String.valueOf(last.getOrDefault(node, LogicLevel.RELEASED).scriptValue()));
                }
            }
            return new Measurement(Double.NaN, "[" + String.join(",", levels) + "]");
        }
        double[] values = signal(run);
        int samples = values.length;
        double after = clauses.containsKey("after") ? quantity(clause("after", "the time to wait, for example 'after 1 ms'")) : 0;
        if (after >= times[samples - 1]) {
            throw new IllegalArgumentException("'after' waits past the end of the run, " + times[samples - 1] + " s");
        }
        int first = 0;
        while (first < samples - 1 && times[first] < after) {
            first++;
        }
        return switch (kind) {
            case "final" -> new Measurement(values[samples - 1], null);
            case "maximum" -> new Measurement(Arrays.stream(values, first, samples).max().orElse(Double.NaN), null);
            case "minimum" -> new Measurement(Arrays.stream(values, first, samples).min().orElse(Double.NaN), null);
            case "average" -> new Measurement(Arrays.stream(values, first, samples).average().orElse(Double.NaN), null);
            case "rms" -> new Measurement(Math.sqrt(Arrays.stream(values, first, samples).map(value -> value * value)
                    .average().orElse(Double.NaN)), null);
            case "value" -> new Measurement(valueAt(values, times, quantity(clause("at", "the moment, for example 'at 2 ms'"))), null);
            default -> {
                boolean above = clauses.containsKey("above");
                if (above == clauses.containsKey("below")) {
                    throw new IllegalArgumentException("'" + kind + "' needs 'above <level>' or 'below <level>'");
                }
                String side = above ? "above" : "below";
                double level = quantity(clause(side, "the level, for example '" + side + " 2.5 V'"));
                yield new Measurement(kind.equals("duty") ? duty(values, times, first, level, above)
                        : frequency(values, times, first, level, above), null);
            }
        };
    }

    // ─── what is read ─────────────────────────────────────────────────────────────────────────────────

    private double[] signal(TransientRun run) {
        String written = String.join(" ", signal);
        if (signal.size() > 1 && signal.get(1).equals("(")) {
            throw new IllegalArgumentException("'" + written + "' is written as the circuit names it now: 'output', "
                    + "'output - reference', 'current V1', 'power R1', 'level clock'");
        }
        if (signal.size() == 2 && signal.getFirst().equals("current")) {
            return run.current(signal.get(1));
        }
        if (signal.size() == 2 && signal.getFirst().equals("power")) {
            double[] values = run.powers().get(ColumnNames.partPower(signal.get(1)));
            if (values == null) {
                throw new IllegalArgumentException("'" + signal.get(1) + "' reports no power"
                        + (run.samples() == 1 ? ": a power is measured over a run, 'average power " + signal.get(1) + "'" : ""));
            }
            return values;
        }
        if (!signal.isEmpty() && signal.getFirst().equals(FIGURE)) {
            return run.figure(figureNames().iterator().next());
        }
        if (signal.size() == 2 && signal.getFirst().equals("level")) {
            double[] values = new double[run.samples()];
            for (int sample = 0; sample < values.length; sample++) {
                values[sample] = run.levels().get(sample).getOrDefault(signal.get(1), LogicLevel.RELEASED).fraction();
            }
            return values;
        }
        if (signal.size() == 3 && signal.get(1).equals("-")) {
            double[] positive = run.voltage(signal.getFirst());
            double[] negative = run.voltage(signal.get(2));
            double[] values = new double[positive.length];
            for (int sample = 0; sample < values.length; sample++) {
                values[sample] = positive[sample] - negative[sample];
            }
            return values;
        }
        if (signal.size() == 1) {
            return run.voltage(signal.getFirst());
        }
        throw new IllegalArgumentException("'" + written + "' is not a signal: a node 'output', a difference "
                + "'output - reference', 'current V1', 'power R1' or 'level clock'");
    }

    /**
     * The circuit at its operating point, read as a run of one instant: every node's voltage, and the current of every
     * part that carries one of its own — a source's, an inductor's —, a diode's and a resistor's.
     */
    private static TransientRun operatingPoint(CompiledObject compiled) {
        Netlist netlist = Netlist.of(compiled);
        CircuitSolver solver = new CircuitSolver(netlist);
        double[] state = solver.operatingPoint();
        Map<String, double[]> voltages = new LinkedHashMap<>();
        for (int node = 0; node < netlist.nodes().size(); node++) {
            voltages.put(netlist.nodes().get(node), new double[]{state[node]});
        }
        Map<String, double[]> currents = new LinkedHashMap<>();
        for (int branch = 0; branch < netlist.branches().size(); branch++) {
            currents.put(netlist.branches().get(branch).name(), new double[]{state[netlist.nodes().size() + branch]});
        }
        PartReadings.currents(solver.devices(), state).forEach((column, current) -> currents.putIfAbsent(column, new double[]{current}));
        return TransientRun.instant(voltages, currents);
    }

    /** The signal's value at a moment of the run, between the two samples around it. */
    private static double valueAt(double[] values, double[] times, double moment) {
        if (moment < times[0] || moment > times[times.length - 1]) {
            throw new IllegalArgumentException("'at' is a moment within the run, 0 .. " + times[times.length - 1] + " s");
        }
        int after = 1;
        while (after < times.length - 1 && times[after] < moment) {
            after++;
        }
        double span = times[after] - times[after - 1];
        double share = span > 0 ? (moment - times[after - 1]) / span : 1;
        return values[after - 1] + (values[after] - values[after - 1]) * share;
    }

    // ─── the small-signal response ────────────────────────────────────────────────────────────────────

    /** {@code gain out at 1 kHz} in dB, {@code phase out at 1 kHz} in degrees: the linearized circuit's response there. */
    private double smallSignal(CompiledObject circuit) {
        double frequency = quantity(clause("at", "the frequency, for example 'at 1 kHz'"));
        Response response = Response.of(circuit);
        Complex phasor = response.at(frequency, signal);
        return kind.equals("gain") ? CircuitComputations.FrequencyResponse.gainOf(phasor) : Math.toDegrees(phasor.argument());
    }

    /**
     * The lowest frequency at which the gain crosses 3 dB under its peak: a low-pass's bandwidth, a high-pass's corner,
     * a band-pass's lower edge. Looked for over a fixed range on a logarithmic grid, then bisected.
     */
    private double corner(CompiledObject circuit) {
        Response response = Response.of(circuit);
        int points = (int) Math.round(Math.log10(HIGHEST_FREQUENCY / LOWEST_FREQUENCY) * FREQUENCIES_PER_DECADE) + 1;
        double[] frequencies = new double[points];
        double[] gains = new double[points];
        double peak = Double.NEGATIVE_INFINITY;
        for (int point = 0; point < points; point++) {
            frequencies[point] = LOWEST_FREQUENCY * Math.pow(10, (double) point / FREQUENCIES_PER_DECADE);
            gains[point] = CircuitComputations.FrequencyResponse.gainOf(response.at(frequencies[point], signal));
            peak = Math.max(peak, gains[point]);
        }
        double level = peak - CORNER_DROP;
        for (int point = 1; point < points; point++) {
            if ((gains[point - 1] - level) * (gains[point] - level) <= 0 && gains[point - 1] != gains[point]) {
                double low = frequencies[point - 1];
                double high = frequencies[point];
                boolean rising = gains[point] > gains[point - 1];
                for (int bisection = 0; bisection < CORNER_BISECTIONS; bisection++) {
                    double middle = Math.sqrt(low * high);
                    boolean over = CircuitComputations.FrequencyResponse.gainOf(response.at(middle, signal)) > level;
                    if (over == rising) {
                        high = middle;
                    } else {
                        low = middle;
                    }
                }
                return Math.sqrt(low * high);
            }
        }
        throw new IllegalArgumentException("the gain of '" + String.join(" ", signal) + "' never falls 3 dB under its peak between "
                + LOWEST_FREQUENCY + " Hz and " + HIGHEST_FREQUENCY + " Hz");
    }

    /** The circuit linearized once at its operating point, answered at any frequency. */
    private record Response(CircuitSolver solver, Netlist netlist, double[] operatingPoint) {

        static Response of(CompiledObject circuit) {
            Netlist netlist = Netlist.of(circuit);
            CircuitSolver solver = new CircuitSolver(netlist);
            return new Response(solver, netlist, CircuitComputations.smallSignalBias(solver, netlist));
        }

        /** A node's phasor, or the difference of two — {@code out}, {@code out - reference}. */
        Complex at(double frequency, List<String> signal) {
            Complex[] phasors = solver.phasors(frequency, operatingPoint);
            if (signal.size() == 1) {
                return phasor(phasors, signal.getFirst());
            }
            if (signal.size() == 3 && signal.get(1).equals("-")) {
                return phasor(phasors, signal.getFirst()).subtract(phasor(phasors, signal.get(2)));
            }
            throw new IllegalArgumentException("a gain or a phase is a node's, 'out', or between two, 'out - reference'");
        }

        private Complex phasor(Complex[] phasors, String node) {
            if (node.equals(Circuit.GROUND)) {
                return Complex.ZERO;
            }
            int index = netlist.nodes().indexOf(node);
            if (index < 0) {
                throw new IllegalArgumentException("'" + node + "' is not an analog node of the circuit");
            }
            return phasors[index];
        }
    }

    // ─── with ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * The circuit as {@code with} sets it: a circuit's parameter — a slider — by its name, kept within its range, or a
     * part's setting as {@code part.setting} — a switch closed, a resistor changed.
     */
    private static CompiledObject withSettings(CompiledObject circuit, List<String> words) {
        Map<String, Double> parameters = new LinkedHashMap<>();
        Map<String, Map<String, String>> settings = new LinkedHashMap<>();
        for (List<String> assignment : split(words)) {
            int equals = assignment.indexOf("=");
            if (equals < 1 || equals == assignment.size() - 1) {
                throw new IllegalArgumentException("'with' is 'with name = value, part.setting = value'");
            }
            // 'R1.resistance' comes as three words.
            String name = String.join("", assignment.subList(0, equals));
            double value = quantity(assignment.subList(equals + 1, assignment.size()));
            int dot = name.indexOf('.');
            if (dot > 0) {
                settings.computeIfAbsent(name.substring(0, dot), ignored -> new LinkedHashMap<>())
                        .put(name.substring(dot + 1), Double.toString(value));
                continue;
            }
            ParameterDefinition parameter = circuit.parameters().stream().filter(candidate -> candidate.name().equals(name))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("'" + name + "' is not a parameter of the "
                            + "circuit; a part's setting is written 'part.setting'"));
            if (parameter.clamp(value) != value) {
                throw new IllegalArgumentException("'" + name + "' = " + value + " is outside its range "
                        + parameter.minimum() + " .. " + parameter.maximum());
            }
            parameters.put(name, value);
        }
        CompiledObject measured = parameters.isEmpty() ? circuit : circuit.withParameters(parameters);
        return settings.isEmpty() ? measured : measured.withDefinition(withPartSettings((Circuit) circuit.definition(), settings));
    }

    private static Circuit withPartSettings(Circuit circuit, Map<String, Map<String, String>> settings) {
        Set<String> named = new HashSet<>(settings.keySet());
        List<CircuitElement> elements = circuit.elements().stream().map(element -> {
            Map<String, String> changed = settings.get(element.id());
            if (changed == null) {
                return element;
            }
            named.remove(element.id());
            Map<String, String> overrides = new LinkedHashMap<>(element.overrides());
            overrides.putAll(changed);
            return new CircuitElement(element.id(), element.type(), element.positive(), element.negative(), element.value(),
                    element.component(), element.pins(), overrides, element.symbol());
        }).toList();
        if (!named.isEmpty()) {
            throw new IllegalArgumentException("'with' names no part of the circuit: " + String.join(", ", named));
        }
        return new Circuit(elements, circuit.time(), circuit.layout(), circuit.conditions(), circuit.probes(),
                circuit.expectations(), circuit.timePerDivision());
    }

    private static List<List<String>> split(List<String> words) {
        List<List<String>> parts = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String word : words) {
            if (word.equals(",")) {
                parts.add(current);
                current = new ArrayList<>();
            } else {
                current.add(word);
            }
        }
        parts.add(current);
        return parts;
    }

    // ─── crossings ────────────────────────────────────────────────────────────────────────────────────

    /** Rising crossings of the level, counted after {@code first}, over the time they span. */
    private static double frequency(double[] signal, double[] times, int first, double level, boolean above) {
        List<Double> crossings = risingCrossings(signal, times, first, level, above);
        if (crossings.size() < 2) {
            return 0;
        }
        return (crossings.size() - 1) / (crossings.getLast() - crossings.getFirst());
    }

    private static List<Double> risingCrossings(double[] signal, double[] times, int first, double level, boolean above) {
        List<Double> crossings = new ArrayList<>();
        for (int sample = first + 1; sample < signal.length; sample++) {
            double before = above ? signal[sample - 1] - level : level - signal[sample - 1];
            double now = above ? signal[sample] - level : level - signal[sample];
            if (before <= 0 && now > 0) {
                crossings.add(times[sample - 1] + (times[sample] - times[sample - 1]) * (-before) / (now - before));
            }
        }
        return crossings;
    }

    /** The active share over complete cycles, so a partial cycle at either end cannot bias the result. */
    private static double duty(double[] signal, double[] times, int first, double level, boolean above) {
        List<Double> crossings = risingCrossings(signal, times, first, level, above);
        if (crossings.size() < 2) {
            return 0;
        }
        double startTime = crossings.getFirst();
        double endTime = crossings.getLast();
        double inside = 0;
        for (int sample = first + 1; sample < signal.length; sample++) {
            double left = Math.max(times[sample - 1], startTime);
            double right = Math.min(times[sample], endTime);
            if (right <= left) {
                continue;
            }
            double beginning = above ? signal[sample - 1] - level : level - signal[sample - 1];
            double ending = above ? signal[sample] - level : level - signal[sample];
            double fractionLeft = (left - times[sample - 1]) / (times[sample] - times[sample - 1]);
            double fractionRight = (right - times[sample - 1]) / (times[sample] - times[sample - 1]);
            double start = beginning + (ending - beginning) * fractionLeft;
            double end = beginning + (ending - beginning) * fractionRight;
            double width = right - left;
            if (start > 0 && end > 0) {
                inside += width;
            } else if (start > 0 || end > 0) {
                inside += width * Math.max(start, end) / Math.abs(end - start);
            }
        }
        return inside / (endTime - startTime);
    }

    // ─── words ────────────────────────────────────────────────────────────────────────────────────────

    private List<String> clause(String word, String needs) {
        List<String> words = clauses.get(word);
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("'" + kind + "' needs '" + word + "' and " + needs);
        }
        return words;
    }

    /** A quantity written as words: its number, then its unit however it was spaced — {@code 2.5 V}, {@code 1 k Hz}. */
    private static double quantity(List<String> words) {
        return Quantity.valueOf(words.getFirst() + " " + String.join("", words.subList(1, words.size())));
    }

    /** What was measured: a number, or — for digital levels — a text, {@code value} then NaN. */
    record Measurement(double value, String text) {

        boolean admittedBy(ComponentModel.Criterion criterion) {
            return text != null ? criterion.admits(text) : criterion.admits(value);
        }

        /** {@code PASS 4.9873 — must be …} or {@code FAIL …}, as a check reports it. */
        String verdict(boolean admitted, ComponentModel.Criterion criterion) {
            String measured = text != null ? text : String.format(Locale.ROOT, "%.5g", value);
            return (admitted ? "PASS " : "FAIL ") + measured + " — must be " + criterion.describe();
        }
    }
}
