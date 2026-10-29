package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Measure;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;
import org.jmouse.formula.Complex;
import org.jmouse.numerics.ComputationProgress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What an {@code expect} measures, taken on a circuit — a component's bench, or a circuit that states what it does of
 * itself —, as its {@code measure} says it:
 *
 * <pre>
 * measure(operating, out)                         the operating point
 * measure(final|maximum|minimum|average|rms, out, after: 1 ms)
 * measure(value, out, at: 2 ms)                   the value at a moment
 * measure(frequency|duty, out, above: 2.5 V)      a crossing's rate, or the share of time above it
 * measure(gain|phase, out, at: 1 kHz)             the small-signal response, dB or degrees
 * measure(corner, out)                            where the gain first falls 3 dB under its peak
 * measure(…, with: [R_load: 1 kΩ, S1.closed: 1])  the circuit with other values or part settings
 * </pre>
 *
 * What is read is a net's voltage ({@code out}), the voltage between two ({@code out - reference}), a part's current
 * ({@code current: V1}) or power ({@code power: R1}), a digital net's level ({@code level: clock}), digital levels as a row ({@code levels: [a, b]}),
 * a display's figure ({@code figure: U1.reading}) or its figures as a row ({@code figures: [U1.cell_0, U1.cell_1]}).
 */
final class CircuitMeasurement {

    /** Time steps a run is simulated in. */
    static final int SAMPLES = 4000;

    /** A corner is looked for over this range, this many frequencies a decade, then bisected. */
    private static final double LOWEST_FREQUENCY = 1e-3;
    private static final double HIGHEST_FREQUENCY = 1e9;
    private static final int FREQUENCIES_PER_DECADE = 30;
    private static final int CORNER_BISECTIONS = 40;
    private static final double CORNER_DROP = 3;

    /** The smallest response a gain in decibels is taken of: what stands for nought. */
    private static final double SMALLEST_RESPONSE = 1e-12;

    /** The readings other than a net's voltage, as a measure names them. */
    private static final String CURRENT = "current";
    private static final String POWER = "power";
    private static final String LEVEL = "level";
    private static final String LEVELS = "levels";
    private static final String FIGURE = "figure";
    private static final String FIGURES = "figures";

    /** The conditions a measure may write. */
    private static final String AFTER = "after";
    private static final String AT = "at";
    private static final String ABOVE = "above";
    private static final String BELOW = "below";

    private CircuitMeasurement() {
    }

    /** Takes the measurement on the circuit, run over its duration in {@link #SAMPLES} steps when it needs a run. */
    static Measured take(Measure measure, MeasuredCircuit circuit) {
        ElaboratedCircuit measured = circuit.elaborated(measure.settings());
        return switch (measure.statistic()) {
            case SimulationVocabulary.OPERATING -> new Measured.Value(signal(measure, operatingPoint(measured))[0]);
            case "gain", "phase" -> new Measured.Value(smallSignal(measure, measured));
            case "corner" -> new Measured.Value(corner(measure, measured));
            case "final", "maximum", "minimum", "average", "rms", "value", "frequency", "duty" ->
                    overTheRun(measure, TransientRun.of(measured, SAMPLES, true, figureNames(measure), ComputationProgress.NONE));
            default -> throw new IllegalArgumentException("'" + measure.statistic() + "' is not a measurement: operating, final, "
                    + "maximum, minimum, average, rms, value, frequency, duty, gain, phase, corner");
        };
    }

    /** The display figures the measurement reads — {@code figure: U1.reading}, {@code figures: [U1.cell_0, …]} —; none else. */
    private static Set<String> figureNames(Measure measure) {
        if (!FIGURE.equals(measure.reading()) && !FIGURES.equals(measure.reading())) {
            return Set.of();
        }
        if (FIGURE.equals(measure.reading()) && measure.targets().size() != 1) {
            throw new IllegalArgumentException("'figure' reads one display figure, 'figure: U1.reading'; several are "
                    + "'figures: [U1.cell_0, U1.cell_1]'");
        }
        return new LinkedHashSet<>(measure.targets());
    }

    private static Measured overTheRun(Measure measure, TransientRun run) {
        if (measure.statistic().equals("final") && FIGURES.equals(measure.reading())) {
            List<Object> shown = new ArrayList<>();
            for (String name : measure.targets()) {
                double[] values = run.figure(name);
                shown.add(values[values.length - 1]);
            }
            return new Measured.Row(shown);
        }
        if (measure.statistic().equals("final") && LEVELS.equals(measure.reading())) {
            Map<String, LogicLevel> last = run.levels().getLast();
            List<Object> levels = new ArrayList<>();
            for (String net : measure.targets()) {
                levels.add(last.getOrDefault(net, LogicLevel.RELEASED).scriptValue());
            }
            return new Measured.Row(levels);
        }
        double[] times = run.times();
        double[] values = signal(measure, run);
        int samples = values.length;
        double after = measure.condition(AFTER).orElse(0.0);
        if (after >= times[samples - 1]) {
            throw new IllegalArgumentException("'after' waits past the end of the run, " + times[samples - 1] + " s");
        }
        int first = 0;
        while (first < samples - 1 && times[first] < after) {
            first++;
        }
        return new Measured.Value(switch (measure.statistic()) {
            case "final" -> values[samples - 1];
            case "maximum" -> Arrays.stream(values, first, samples).max().orElse(Double.NaN);
            case "minimum" -> Arrays.stream(values, first, samples).min().orElse(Double.NaN);
            case "average" -> Arrays.stream(values, first, samples).average().orElse(Double.NaN);
            case "rms" -> Math.sqrt(Arrays.stream(values, first, samples).map(value -> value * value).average().orElse(Double.NaN));
            case "value" -> valueAt(values, times, required(measure, AT, "the moment, for example 'at: 2 ms'"));
            default -> crossings(measure, values, times, first);
        });
    }

    /** {@code frequency} or {@code duty} of a signal crossing a level from below ({@code above}) or from above ({@code below}). */
    private static double crossings(Measure measure, double[] values, double[] times, int first) {
        boolean above = measure.condition(ABOVE).isPresent();
        if (above == measure.condition(BELOW).isPresent()) {
            throw new IllegalArgumentException("'" + measure.statistic() + "' needs 'above: <level>' or 'below: <level>'");
        }
        String side = above ? ABOVE : BELOW;
        double level = required(measure, side, "the level, for example '" + side + ": 2.5 V'");
        return measure.statistic().equals("duty") ? duty(values, times, first, level, above)
                : frequency(values, times, first, level, above);
    }

    private static double required(Measure measure, String condition, String needs) {
        return measure.condition(condition).orElseThrow(() -> new IllegalArgumentException(
                "'" + measure.statistic() + "' needs '" + condition + "', " + needs));
    }

    // ─── what is read ─────────────────────────────────────────────────────────────────────────────────

    /** The values of what the measure reads, sample by sample. */
    private static double[] signal(Measure measure, TransientRun run) {
        if (measure.reading() == null) {
            Subject subject = Subject.of(measure);
            double[] positive = run.voltage(subject.positive());
            if (subject.negative() == null) {
                return positive;
            }
            double[] negative = run.voltage(subject.negative());
            double[] values = new double[positive.length];
            for (int sample = 0; sample < values.length; sample++) {
                values[sample] = positive[sample] - negative[sample];
            }
            return values;
        }
        String target = single(measure);
        return switch (measure.reading()) {
            case CURRENT -> run.current(target);
            case POWER -> {
                double[] values = run.powers().get(ColumnNames.partPower(target));
                if (values == null) {
                    throw new IllegalArgumentException("'" + target + "' reports no power" + (run.samples() == 1
                            ? ": a power is measured over a run, 'measure(average, power: " + target + ")'" : ""));
                }
                yield values;
            }
            case FIGURE -> run.figure(target);
            case LEVEL -> {
                double[] values = new double[run.samples()];
                for (int sample = 0; sample < values.length; sample++) {
                    values[sample] = run.levels().get(sample).getOrDefault(target, LogicLevel.RELEASED).fraction();
                }
                yield values;
            }
            default -> throw new IllegalArgumentException("'" + measure.reading() + "' is read as a row, by 'final' alone: "
                    + "'measure(final, " + measure.reading() + ": […])'");
        };
    }

    private static String single(Measure measure) {
        if (measure.targets().size() != 1) {
            throw new IllegalArgumentException("'" + measure.reading() + "' reads one, '" + measure.reading() + ": V1'");
        }
        return measure.targets().getFirst();
    }

    /**
     * A net's voltage, or the voltage between two — {@code out}, {@code out - reference} —: what a gain, a phase and a
     * transient's voltage are of. A net is a name, whatever word it is — {@code in} is a net, though jMF keeps it —, so the
     * subject is read as names around a minus, never as an expression.
     */
    private record Subject(String positive, String negative) {

        /** What separates a subject's names: spaces, as a measure writes them. */
        private static final String SPACES = "\\s+";
        private static final String MINUS = "-";

        static Subject of(Measure measure) {
            if (measure.subject() == null) {
                throw new IllegalArgumentException("'" + measure.statistic() + "' says what it measures: a net, 'out', or two, "
                        + "'out - reference'");
            }
            String[] words = measure.subject().written().strip().split(SPACES);
            if (words.length == 1 && !words[0].contains(MINUS)) {
                return new Subject(words[0], null);
            }
            if (words.length == 3 && words[1].equals(MINUS)) {
                return new Subject(words[0], words[2]);
            }
            throw new IllegalArgumentException("'" + measure.subject() + "' is not a signal: a net 'output', a difference "
                    + "'output - reference', 'current: V1', 'power: R1', 'level: clock'");
        }
    }

    /**
     * The circuit at its operating point, read as a run of one instant: every net's voltage, and the current of every
     * part that carries one of its own — a source's —, and of every part the devices report.
     */
    private static TransientRun operatingPoint(ElaboratedCircuit elaborated) {
        Netlist netlist = Netlist.of(elaborated);
        CircuitSolver solver = new CircuitSolver(netlist);
        double[] state = solver.operatingPoint();
        Map<String, double[]> voltages = new LinkedHashMap<>();
        for (int node = 0; node < netlist.nodes().size(); node++) {
            voltages.put(netlist.nodes().get(node), new double[] {state[node]});
        }
        Map<String, double[]> currents = new LinkedHashMap<>();
        for (int branch = 0; branch < netlist.branches().size(); branch++) {
            currents.put(netlist.branches().get(branch).name(), new double[] {state[netlist.nodes().size() + branch]});
        }
        PartReadings.currents(solver.devices(), state).forEach((column, current) -> currents.putIfAbsent(column, new double[] {current}));
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

    /** {@code gain} in dB, {@code phase} in degrees: the linearized circuit's response at the frequency {@code at}. */
    private static double smallSignal(Measure measure, ElaboratedCircuit circuit) {
        double frequency = required(measure, AT, "the frequency, for example 'at: 1 kHz'");
        Complex phasor = Response.of(circuit).at(frequency, Subject.of(measure));
        return measure.statistic().equals("gain") ? gainOf(phasor) : Math.toDegrees(phasor.argument());
    }

    /** A response in decibels. */
    private static double gainOf(Complex response) {
        return 20 * Math.log10(Math.max(response.modulus(), SMALLEST_RESPONSE));
    }

    /**
     * The lowest frequency at which the gain crosses 3 dB under its peak: a low-pass's bandwidth, a high-pass's corner, a
     * band-pass's lower edge. Looked for over a fixed range on a logarithmic grid, then bisected.
     */
    private static double corner(Measure measure, ElaboratedCircuit circuit) {
        Response response = Response.of(circuit);
        Subject subject = Subject.of(measure);
        int points = (int) Math.round(Math.log10(HIGHEST_FREQUENCY / LOWEST_FREQUENCY) * FREQUENCIES_PER_DECADE) + 1;
        double[] frequencies = new double[points];
        double[] gains = new double[points];
        double peak = Double.NEGATIVE_INFINITY;
        for (int point = 0; point < points; point++) {
            frequencies[point] = LOWEST_FREQUENCY * Math.pow(10, (double) point / FREQUENCIES_PER_DECADE);
            gains[point] = gainOf(response.at(frequencies[point], subject));
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
                    boolean over = gainOf(response.at(middle, subject)) > level;
                    if (over == rising) {
                        high = middle;
                    } else {
                        low = middle;
                    }
                }
                return Math.sqrt(low * high);
            }
        }
        throw new IllegalArgumentException("the gain of '" + measure.subject() + "' never falls 3 dB under its peak between "
                + LOWEST_FREQUENCY + " Hz and " + HIGHEST_FREQUENCY + " Hz");
    }

    /** The circuit linearized once — at its operating point, or at rest when it is linear —, answered at any frequency. */
    private record Response(CircuitSolver solver, Netlist netlist, double[] bias) {

        static Response of(ElaboratedCircuit circuit) {
            Netlist netlist = Netlist.of(circuit);
            CircuitSolver solver = new CircuitSolver(netlist);
            double[] bias = solver.nonlinear() ? solver.operatingPoint() : new double[netlist.size()];
            return new Response(solver, netlist, bias);
        }

        Complex at(double frequency, Subject subject) {
            Complex[] phasors = solver.phasors(frequency, bias);
            Complex positive = phasor(phasors, subject.positive());
            return subject.negative() == null ? positive : positive.subtract(phasor(phasors, subject.negative()));
        }

        private Complex phasor(Complex[] phasors, String net) {
            if (net.equals(Circuit.GROUND)) {
                return Complex.ZERO;
            }
            int index = netlist.nodes().indexOf(net);
            if (index < 0) {
                throw new IllegalArgumentException("'" + net + "' is not an analog net of the circuit");
            }
            return phasors[index];
        }
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
}
