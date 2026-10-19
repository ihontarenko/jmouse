package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;

import org.jmouse.parallax.script.Quantity;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.parallax.component.ComponentModel.Expectation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.parallax.verification.ConsistencyCheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A component model held against its datasheet: every {@code expect} built into a circuit on its bench, simulated,
 * measured, and compared with what the datasheet says. A model that does not meet its own datasheet says so here,
 * point by point — the measured value, the window it had to fall in, and where in the datasheet that window is. It is
 * a check of the model, run with the catalogue's other consistency checks, not a way of showing it.
 *
 * <p>The bench is a netlist, one element per line:</p>
 * <pre>
 * V1 vcc 0 dc 15 V          — or step, sine 10 V 50 Hz, square 5 V 1 kHz
 * I1 a 0 dc 1 mA
 * R1 a b 10 kOhm · C1 a 0 10 nF · L1 a b 1 mH · D1 a k 1e-14
 * forward(current=10mA) or reverse(voltage=20V) - full two-terminal diode benches
 * U1 self vcc=vcc out=out A.inp=in      — the part under test, pins and parameter overrides by name
 * </pre>
 *
 * <p>The measurements: {@code operating x}, {@code final x}, {@code maximum | minimum | average x},
 * {@code frequency x above 2.5 V}, {@code duty x above 2.5 V} — each optionally {@code after 1 ms} — and
 * {@code final levels a, b, …} for digital nodes. A signal is a node's voltage by its name ({@code output}), the
 * voltage between two ({@code output - reference}), a part's current ({@code current V1}) or a digital node's level
 * ({@code level clock}).</p>
 */
public final class ComponentCheck implements ConsistencyCheck {

    private static final String SELF = "self";

    /** Time steps each bench is simulated in. */
    static final int SAMPLES = 4000;

    public String code() {
        return "COMPONENT_DATASHEET";
    }

    public String description() {
        return "A component model meets every expectation its datasheet sets";
    }

    public boolean appliesTo(ObjectDefinition definition) {
        return definition instanceof ObjectDefinition.Component;
    }

    public CheckResult check(CompiledObject object) {
        String source = ((ObjectDefinition.Component) object.definition()).source();
        ComponentModel model = ComponentDocuments.read(source, "component", object.library());
        if (model.expectations().isEmpty()) {
            return CheckResult.skipped("The component sets no expectations.");
        }
        ComponentLibrary library = reference -> reference.equals(SELF) ? Optional.of(source) : object.library().source(reference);
        List<String> verdicts = new ArrayList<>();
        int failed = 0;
        for (Expectation expectation : model.expectations()) {
            String verdict;
            try {
                Measurement measurement = measure(expectation, model, library, SAMPLES);
                boolean admitted = measurement.admittedBy(expectation.criterion());
                if (!admitted) {
                    failed++;
                }
                verdict = measurement.verdict(admitted, expectation.criterion());
            } catch (RuntimeException failure) {
                failed++;
                verdict = "ERROR " + failure.getMessage();
            }
            verdicts.add(expectation.title() + ": " + verdict + (expectation.reference() == null ? "" : " — " + expectation.reference()));
        }
        int total = model.expectations().size();
        String detail = (total - failed) + " of " + total + " expectations met. " + String.join("; ", verdicts);
        return new CheckResult(failed == 0 ? ConsistencyCheck.Outcome.PASSED : ConsistencyCheck.Outcome.FAILED, (double) failed,
                0.0, detail);
    }

    /** One expectation run: its bench built, simulated and measured. */
    static Measurement measure(Expectation expectation, ComponentModel model, ComponentLibrary library, int samples) {
        Circuit circuit = bench(expectation, model);
        CompiledObject compiled = DefinitionCompiler.compile(circuit, List.of(), Map.of(), library);
        return measureCompiled(compiled, expectation.measure(), samples);
    }

    /**
     * A circuit already compiled — a component's bench, or a circuit that states what it does of itself — simulated
     * and measured as {@code measure} says: {@code final out}, {@code operating current V1}, {@code average x after 1 ms}…
     */
    static Measurement measureCompiled(CompiledObject compiled, List<String> measure, int samples) {
        String kind = measure.getFirst();
        if (kind.equals("operating")) {
            return new Measurement(signal(measure.subList(1, measure.size()), operatingPoint(compiled))[0], null);
        }
        TransientRun run = TransientRun.of(compiled, samples, true, ComputationProgress.NONE);
        double[] times = run.times();
        List<String> rest = measure.subList(1, measure.size());
        double after = 0;
        int afterAt = rest.indexOf("after");
        if (afterAt >= 0) {
            if (afterAt + 1 >= rest.size()) {
                throw new IllegalArgumentException("'after' needs the time to wait, for example 'after 1 ms'");
            }
            after = quantity(rest.subList(afterAt + 1, rest.size()));
            rest = rest.subList(0, afterAt);
        }
        if (kind.equals("final") && !rest.isEmpty() && rest.getFirst().equals("levels")) {
            Map<String, LogicLevel> last = run.levels().getLast();
            List<String> levels = new ArrayList<>();
            for (String node : rest.subList(1, rest.size())) {
                if (!node.equals(",")) {
                    levels.add(String.valueOf(last.getOrDefault(node, LogicLevel.RELEASED).scriptValue()));
                }
            }
            return new Measurement(Double.NaN, "[" + String.join(",", levels) + "]");
        }
        int threshold = indexOfAny(rest, "above", "below");
        List<String> signalWords = threshold >= 0 ? rest.subList(0, threshold) : rest;
        double[] signal = signal(signalWords, run);
        int first = 0;
        while (first < samples - 1 && times[first] < after) {
            first++;
        }
        return switch (kind) {
            case "final" -> new Measurement(signal[samples - 1], null);
            case "maximum" -> new Measurement(Arrays.stream(signal, first, samples).max().orElse(Double.NaN), null);
            case "minimum" -> new Measurement(Arrays.stream(signal, first, samples).min().orElse(Double.NaN), null);
            case "average" -> new Measurement(Arrays.stream(signal, first, samples).average().orElse(Double.NaN), null);
            case "frequency", "duty" -> {
                if (threshold < 0) {
                    throw new IllegalArgumentException("'" + kind + "' needs 'above <level>' or 'below <level>'");
                }
                boolean above = rest.get(threshold).equals("above");
                if (threshold + 1 >= rest.size()) {
                    throw new IllegalArgumentException("'" + rest.get(threshold) + "' needs the level, for example 'above 2.5 V'");
                }
                double level = quantity(rest.subList(threshold + 1, rest.size()));
                yield new Measurement(kind.equals("duty") ? duty(signal, times, first, level, above) : frequency(signal, times, first, level, above), null);
            }
            default -> throw new IllegalArgumentException("'" + kind + "' is not a measurement: operating, final, maximum, minimum, average, frequency, duty");
        };
    }

    /** What a bench measured: a number, or — for digital levels — a text, {@code value} then NaN. */
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

    /** Standard two-terminal fixtures, each a whole bench: {@code X1 forward current = 10 mA}. */
    private static final Set<String> FIXTURES = Set.of("forward", "reverse");

    /**
     * The bench's parts as a circuit. A fixture is the whole bench; a part whose component is one of the solver's
     * primitives becomes that device; any other part — {@code self} included — is a model, its pins bound by name and
     * its settings overriding its parameters.
     */
    private static Circuit bench(Expectation expectation, ComponentModel model) {
        if (!(expectation.duration() > 0)) {
            throw new IllegalArgumentException("the bench says how long it runs: 'bench for 2 ms { … }'");
        }
        List<Block.Part> parts = expectation.bench();
        if (parts.stream().anyMatch(part -> FIXTURES.contains(part.reference()))) {
            if (parts.size() != 1) {
                throw new IllegalArgumentException("a standard bench fixture is the whole bench, not one part among others");
            }
            return atTemperature(new Circuit(fixture(parts.getFirst(), model), new Interval(0, expectation.duration())), expectation);
        }
        List<CircuitElement> elements = parts.stream().map(ComponentCheck::element).toList();
        return atTemperature(new Circuit(elements, new Interval(0, expectation.duration())), expectation);
    }

    /**
     * The bench as its line sets it — {@code bench for 1 ms at 85 °C realistic} —: its temperature, and whether its
     * instruments load what they read; applied as a bench applies it.
     */
    private static Circuit atTemperature(Circuit circuit, Expectation expectation) {
        if (expectation.temperature() == null && !expectation.realistic()) {
            return circuit;
        }
        double temperature = expectation.temperature() != null ? expectation.temperature()
                : ObjectDefinition.BenchConditions.NOMINAL_TEMPERATURE;
        return new Circuit(circuit.elements(), circuit.time(), null, new ObjectDefinition.BenchConditions(
                temperature, 0, Map.of(), Map.of(), expectation.realistic(), Map.of()));
    }

    /**
     * {@code forward current = 10 mA}: a current source into the part's first pin; {@code reverse voltage = 20 V}: a
     * voltage across it, positive at the second. A two-terminal part declares its anode first.
     */
    private static List<CircuitElement> fixture(Block.Part part, ComponentModel model) {
        if (model.pins().size() != 2) {
            throw new IllegalArgumentException("bench fixture '" + part.reference() + "' tests a two-terminal part; '"
                    + model.name() + "' has " + model.pins().size() + " pins");
        }
        String anode = model.pins().get(0).name();
        String cathode = model.pins().get(1).name();
        boolean forward = part.reference().equals("forward");
        String required = forward ? "current" : "voltage";
        SourceText setting = part.settings().get(required);
        if (setting == null) {
            throw new IllegalArgumentException("bench fixture '" + part.reference() + "' needs " + required
                    + " = <quantity>, for example " + required + " = " + (forward ? "10 mA" : "20 V"));
        }
        double value = Quantity.valueOf(setting.text());
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("bench fixture '" + part.reference() + "' needs a positive finite " + required);
        }
        if (forward) {
            return List.of(new CircuitElement("I1", CircuitElementType.CURRENT_SOURCE,
                            "anode", "0", number(value), null, null, null),
                    new CircuitElement("U1", CircuitElementType.MODEL,
                            null, null, null, SELF, Map.of(anode, "anode", cathode, "0"), Map.of()));
        }
        return List.of(new CircuitElement("V1", CircuitElementType.VOLTAGE_SOURCE,
                        "cathode", "0", number(value), null, null, null),
                new CircuitElement("U1", CircuitElementType.MODEL,
                        null, null, null, SELF, Map.of(anode, "0", cathode, "cathode"), Map.of()));
    }

    /** A bench part line, read as a circuit's is: one reader of a part line, never a second. */
    private static CircuitElement element(Block.Part part) {
        return ExperienceDefinitions.element(part, "bench part '" + part.id() + "'");
    }

    private static double[] signal(List<String> words, TransientRun run) {
        List<String> signal = words.stream().filter(word -> !word.isBlank()).toList();
        String written = String.join(" ", signal);
        if (signal.size() > 1 && signal.get(1).equals("(")) {
            throw new IllegalArgumentException("'" + written + "' is written as the circuit names it now: 'output', "
                    + "'output - reference', 'current V1', 'level clock'");
        }
        if (signal.size() == 2 && signal.getFirst().equals("current")) {
            return run.current(signal.get(1));
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
                + "'output - reference', 'current V1' or 'level clock'");
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

    private static int indexOfAny(List<String> words, String... candidates) {
        for (int index = 0; index < words.size(); index++) {
            for (String candidate : candidates) {
                if (words.get(index).equals(candidate)) {
                    return index;
                }
            }
        }
        return -1;
    }

    /** A quantity written as words: its number, then its unit however it was spaced — {@code 2.5 V}, {@code 1 k Hz}. */
    private static double quantity(List<String> words) {
        return Quantity.valueOf(words.getFirst() + " " + String.join("", words.subList(1, words.size())));
    }

    private static String number(double value) {
        return Double.toString(value);
    }
}
