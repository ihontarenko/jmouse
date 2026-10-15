package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.ObjectDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One running circuit as an emulator watches it: the solver, and the probes measured on it. Each frame carries what the
 * probes read — a channel is the voltage between its two nodes, computed here, not by whoever displays it — and the
 * voltages of the probed nodes themselves.
 *
 * <p>A change of value goes on from the present ({@link #retune}); a change of the circuit's text starts it again
 * ({@link #restart}). Every method is synchronised: frames are produced by one thread while changes arrive on another.</p>
 */
public final class LiveCircuitSession {

    /** Names ground has besides the netlist's own {@code 0}. */
    private static final Set<String> GROUND = Set.of("0", "ground", "GND");

    /**
     * What a probe reads: a voltage between two nodes, the current through a part, the power a part takes, or what an
     * instrument part reads — its own `measure`, in its own unit.
     */
    public enum Quantity {
        VOLTAGE(CircuitComputations.VOLTS), CURRENT(CircuitComputations.AMPERES), POWER("W"), READING(null);

        private final String unit;

        Quantity(String unit) {
            this.unit = unit;
        }

        public String unit() {
            return unit;
        }
    }

    /**
     * A probe — an oscilloscope's channel or a meter —, read on every sample: the voltage of {@code positive} against
     * {@code negative}; or, on {@code part}, the current into its {@code terminal} (its first when none is named) or
     * the power it takes, negative for a source delivering it.
     */
    public record Probe(String channel, Quantity quantity, String positive, String negative, String part, String terminal) {
        public Probe {
            quantity = quantity == null ? Quantity.VOLTAGE : quantity;
        }

        /** A voltage probe. */
        public Probe(String channel, String positive, String negative) {
            this(channel, Quantity.VOLTAGE, positive, negative, null, null);
        }
    }

    /**
     * Samples as they came out.
     *
     * @param time     each sample's time, in seconds
     * @param channels each probe's readings, by channel
     * @param units    each channel's unit: "V", "A" or "W"
     * @param levels   every node's mean voltage over the frame — what a part's display figures are read from; its last
     *                 sample's would land on whatever phase a fast signal had then
     * @param parts    how hard every part worked over the frame, by part — what a schematic shows as load and heat
     * @param trace    the whole circuit at the traced samples — what a schematic plays its currents from
     * @param covered  how many samples the frame went through: its samples, or more when it passed some unread
     * @param peaks    each voltage channel's lowest and highest reading over every sample's interval, by channel — what
     *                 the screen draws where the signal moves faster than its samples
     */
    public record Frame(double[] time, Map<String, double[]> channels, Map<String, String> units, Map<String, Double> levels,
                        Map<String, PartLoad> parts, Trace trace, long covered, Map<String, Peaks> peaks) {

        public Frame(double[] time, Map<String, double[]> channels, Map<String, String> units, Map<String, Double> levels,
                     Map<String, PartLoad> parts, Trace trace) {
            this(time, channels, units, levels, parts, trace, time.length, Map.of());
        }
    }

    /** A channel's lowest, highest and mean reading over each sample's interval, sample by sample. */
    public record Peaks(double[] lows, double[] highs, double[] means) {
    }

    /**
     * The circuit at some of a frame's samples ({@link #trace(int)}): every node's voltage (`out`, in volts) and every
     * part's current (`R1`, `U1.output`, in amperes) — the columns and units a transient reports them in, so a sheet
     * plays a live run and a computed one alike. Empty while nothing is traced.
     *
     * @param time    each traced sample's time, in seconds
     * @param columns each column's readings at those times; NaN where a part reported no such terminal
     * @param units   each column's unit, "V" or "A"
     * @param rms     each column's RMS over the stretch each moment stands for, by column — the moment's own magnitude
     *                when it stands for its instant. A current that alternates faster than the moments has a mean near
     *                zero and an RMS that is not: it moves no charge, and it still flows.
     */
    public record Trace(double[] time, Map<String, double[]> columns, Map<String, String> units, Map<String, double[]> rms) {

        public static final Trace EMPTY = new Trace(new double[0], Map.of(), Map.of(), Map.of());
    }

    private LiveCircuitSolver solver;
    private List<Probe>       probes;
    /** The circuit as the run was given it, before any part failed; null for a session given only a solver. */
    private CompiledObject    given;
    /** The parts that failed on a realistic bench during this run, and how: kept through every change until a restart. */
    private final Map<String, String> failures = new LinkedHashMap<>();
    /** Why each failed part failed: overheated, overvoltage, overcurrent. */
    private final Map<String, String> causes = new LinkedHashMap<>();
    /** What each part shows on its symbol, compiled from the circuit given; none for a session given only a solver. */
    private PartDisplays displays;
    /** Every how many samples the whole circuit is traced; 0 traces none. */
    private int               traceEvery;
    /** What each instrument probe reads, by channel, resolved from the instrument's document and its wiring. */
    private Map<String, InstrumentReading> instruments = Map.of();

    public LiveCircuitSession(LiveCircuitSolver solver, List<Probe> probes) {
        this.solver = solver;
        probe(probes);
    }

    /**
     * A session that knows its circuit, so that on a realistic bench ({@code BenchConditions.realistic}) a part hotter
     * than its component lets it be fails — as its component says it fails — and the run goes on with it failed.
     */
    public LiveCircuitSession(CompiledObject object, double sampleInterval, List<Probe> probes) {
        this(new LiveCircuitSolver(object, sampleInterval), probes);
        this.given = object;
        this.displays = PartDisplays.of(object);
        this.instruments = instrumentReadings(this.probes);
    }

    public synchronized List<String> nodes() {
        return solver.nodes();
    }

    public synchronized double time() {
        return solver.time();
    }

    public synchronized void probe(List<Probe> probes) {
        for (Probe probe : probes) {
            if (probe.quantity() == Quantity.VOLTAGE) {
                requireNode(probe.positive());
                requireNode(probe.negative());
            } else if (probe.part() == null || probe.part().isBlank()) {
                throw new IllegalArgumentException("A " + probe.quantity().name().toLowerCase() + " probe names its part.");
            }
        }
        this.instruments = instrumentReadings(probes);
        this.probes = List.copyOf(probes);
        equipSolver();
    }

    /** Tells the solver — a new one after every change that could not go on — what the probes need read: parts and differences. */
    private void equipSolver() {
        solver.meter(meteredParts());
        solver.difference(watchedDifferences());
    }

    /**
     * The differences a voltage probe reads across a part — its node against a reference that is not ground —, in the
     * order of the probes: their peaks are kept as the difference's own ({@link #peaksOf}).
     */
    private List<LiveCircuitSolver.NodeDifference> watchedDifferences() {
        List<String> order = solver.nodes();
        return probes.stream().filter(probe -> probe.quantity() == Quantity.VOLTAGE && indexOf(order, probe.negative()) != -1)
                .map(probe -> new LiveCircuitSolver.NodeDifference(indexOf(order, probe.positive()), indexOf(order, probe.negative()))).toList();
    }

    private Set<String> meteredParts() {
        return probes.stream().filter(probe -> probe.quantity() != Quantity.VOLTAGE).map(Probe::part)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Traces the whole circuit — node voltages and part currents — on every {@code every}-th sample from now on, through
     * every change of value or text; 0 traces none.
     */
    public synchronized void trace(int every) {
        traceEvery = every;
        solver.trace(every);
    }

    /** The next samples, measured. */
    public synchronized Frame advance(int count) {
        List<LiveCircuitSolver.Sample> samples = solver.advance(count);
        double[] time = new double[samples.size()];
        Map<String, double[]> channels = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        for (Probe probe : probes) {
            channels.put(probe.channel(), new double[samples.size()]);
            units.put(probe.channel(), probe.quantity() == Quantity.READING ? readingUnit(probe) : probe.quantity().unit());
        }
        // Where each probe's two nodes are among the samples' values, found once for the whole frame.
        List<String> order = solver.nodes();
        int[] positives = probes.stream().mapToInt(probe -> nodeIndexOf(order, probe, probe.positive())).toArray();
        int[] negatives = probes.stream().mapToInt(probe -> nodeIndexOf(order, probe, probe.negative())).toArray();
        // Where each instrument's pins are among the samples' values, found once for the frame too.
        int[][] instrumentNodes = probes.stream().map(probe -> {
            InstrumentReading reading = instruments.get(probe.channel());
            return reading == null ? new int[0] : reading.expression().pinNodes().stream().mapToInt(node -> indexOf(order, node)).toArray();
        }).toArray(int[][]::new);
        double[][] channelColumns = probes.stream().map(probe -> channels.get(probe.channel())).toArray(double[][]::new);
        for (int index = 0; index < samples.size(); index++) {
            LiveCircuitSolver.Sample sample = samples.get(index);
            time[index] = sample.time();
            for (int probe = 0; probe < positives.length; probe++) {
                channelColumns[probe][index] = switch (probes.get(probe).quantity()) {
                    case VOLTAGE -> voltage(sample, positives[probe]) - voltage(sample, negatives[probe]);
                    case CURRENT -> current(sample, probes.get(probe));
                    case POWER -> power(sample, probes.get(probe));
                    case READING -> reading(sample, probes.get(probe), instrumentNodes[probe]);
                };
            }
        }
        Map<String, Double> levels = meanLevels(samples, order);
        Map<String, PartLoad> loads = shown(solver.loads(), levels);
        burnOut(loads);
        return new Frame(time, channels, units, levels, withCauses(loads), traced(samples, order), samples.size(),
                peaksOf(samples, positives, negatives));
    }

    /**
     * Goes through the next {@code count} samples without measuring them: a frame with no samples, but the parts'
     * loads, the trace and the levels of the stretch — the traced moments' means — as a measured one has them.
     */
    public synchronized Frame pass(long count) {
        List<LiveCircuitSolver.Sample> traced = solver.pass(count);
        List<String> order = solver.nodes();
        Map<String, double[]> channels = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        for (Probe probe : probes) {
            channels.put(probe.channel(), new double[0]);
            units.put(probe.channel(), probe.quantity() == Quantity.READING ? readingUnit(probe) : probe.quantity().unit());
        }
        Map<String, Double> levels = meanLevels(traced, order);
        Map<String, PartLoad> loads = shown(solver.loads(), levels);
        burnOut(loads);
        return new Frame(new double[0], channels, units, levels, withCauses(loads), traced(traced, order), count, Map.of());
    }

    /**
     * Each voltage channel's peaks over a sample's interval: against ground, its node's; across a part, the difference's
     * own, kept by the solver step by step ({@link #watchedDifferences}). Only where the solver could not keep one — the
     * two nodes in separate pieces of the circuit — the lowest of the node against the highest of the reference stands
     * in: a bound, twice the swing on a slope where the two move apart, so never where it can be avoided.
     */
    private Map<String, Peaks> peaksOf(List<LiveCircuitSolver.Sample> samples, int[] positives, int[] negatives) {
        Map<String, Peaks> peaks = new LinkedHashMap<>();
        if (samples.isEmpty() || samples.getFirst().envelope() == null) {
            return peaks;
        }
        int watched = 0;
        for (int probe = 0; probe < probes.size(); probe++) {
            if (probes.get(probe).quantity() != Quantity.VOLTAGE) {
                continue;
            }
            int difference = negatives[probe] != -1 ? watched++ : -1;
            double[] lows = new double[samples.size()];
            double[] highs = new double[samples.size()];
            double[] means = new double[samples.size()];
            for (int index = 0; index < samples.size(); index++) {
                LiveCircuitSolver.Envelope envelope = samples.get(index).envelope();
                if (envelope == null) {
                    double reading = voltage(samples.get(index), positives[probe]) - voltage(samples.get(index), negatives[probe]);
                    lows[index] = reading;
                    highs[index] = reading;
                    means[index] = reading;
                    continue;
                }
                boolean kept = difference >= 0 && difference < envelope.differenceLows().length
                        && Double.isFinite(envelope.differenceLows()[difference]);
                lows[index] = kept ? envelope.differenceLows()[difference]
                        : peak(envelope.lows(), positives[probe]) - peak(envelope.highs(), negatives[probe]);
                highs[index] = kept ? envelope.differenceHighs()[difference]
                        : peak(envelope.highs(), positives[probe]) - peak(envelope.lows(), negatives[probe]);
                // A mean is linear: the difference of the means is the mean of the difference, exactly.
                means[index] = peak(envelope.means(), positives[probe]) - peak(envelope.means(), negatives[probe]);
            }
            peaks.put(probes.get(probe).channel(), new Peaks(lows, highs, means));
        }
        return peaks;
    }

    /** A node's peak among these: ground (-1) is zero, a node the circuit lacks (-2) is not known. */
    private static double peak(double[] peaks, int node) {
        return node == -1 ? 0 : node < 0 ? Double.NaN : peaks[node];
    }

    /** Every node's mean voltage over these samples, by node: a traced sample's stretch where it stands for one. */
    private static Map<String, Double> meanLevels(List<LiveCircuitSolver.Sample> samples, List<String> order) {
        Map<String, Double> levels = new LinkedHashMap<>();
        if (samples.isEmpty()) {
            return levels;
        }
        double[] sums = new double[order.size()];
        for (LiveCircuitSolver.Sample sample : samples) {
            for (int node = 0; node < sums.length; node++) {
                sums[node] += sample.window() != null ? sample.window().means()[node] : sample.voltages()[node];
            }
        }
        for (int node = 0; node < sums.length; node++) {
            levels.put(order.get(node), sums[node] / samples.size());
        }
        return levels;
    }

    /** The loads with what each part's symbol shows over the frame, read from its mean levels. */
    private Map<String, PartLoad> shown(Map<String, PartLoad> loads, Map<String, Double> levels) {
        if (displays == null || displays.isEmpty()) {
            return loads;
        }
        Map<String, Map<String, Double>> figures = displays.read(levels, loads);
        Map<String, PartLoad> shown = new LinkedHashMap<>(loads);
        figures.forEach((part, values) -> {
            PartLoad load = shown.get(part);
            shown.put(part, (load != null ? load : new PartLoad(0, Map.of(), Double.NaN)).showing(values));
        });
        return shown;
    }

    /** On a realistic bench, every part past its limit fails now; the circuit goes on without it working. */
    private void burnOut(Map<String, PartLoad> loads) {
        if (given == null || !realistic(given)) {
            return;
        }
        boolean failed = false;
        for (Map.Entry<String, PartLoad> load : loads.entrySet()) {
            String cause = load.getValue().failure() == null ? load.getValue().breaking() : null;
            if (cause != null && !failures.containsKey(load.getKey())) {
                failures.put(load.getKey(), solver.failureOf(load.getKey()));
                causes.put(load.getKey(), cause);
                failed = true;
            }
        }
        if (failed) {
            goOn(given);
        }
    }

    /** Every failed part's load with why it failed. */
    private Map<String, PartLoad> withCauses(Map<String, PartLoad> loads) {
        if (causes.isEmpty()) {
            return loads;
        }
        Map<String, PartLoad> caused = new LinkedHashMap<>(loads);
        causes.forEach((part, cause) -> caused.computeIfPresent(part, (ignored, load) -> load.causedBy(cause)));
        return caused;
    }

    private static boolean realistic(CompiledObject object) {
        return object.definition() instanceof ObjectDefinition.Circuit circuit && circuit.conditions() != null
                && circuit.conditions().realistic();
    }

    /** The circuit given, with every part that failed so far failed in it. */
    private CompiledObject withFailures(CompiledObject object) {
        if (failures.isEmpty() || !(object.definition() instanceof ObjectDefinition.Circuit circuit)) {
            return object;
        }
        ObjectDefinition.BenchConditions conditions = circuit.conditions() != null ? circuit.conditions()
                : new ObjectDefinition.BenchConditions(ObjectDefinition.BenchConditions.NOMINAL_TEMPERATURE, 0, Map.of(), Map.of());
        for (Map.Entry<String, String> failure : failures.entrySet()) {
            conditions = conditions.withFailure(failure.getKey(), failure.getValue());
        }
        ObjectDefinition.Circuit failedCircuit = new ObjectDefinition.Circuit(circuit.elements(), circuit.time(), circuit.layout(),
                conditions, circuit.probes());
        return DefinitionCompiler.compile(failedCircuit, object.parameters(), object.parameterValues(), object.library());
    }

    /** The circuit goes on from where it is, with its failures; one whose parts changed starts again. */
    private void goOn(CompiledObject object) {
        CompiledObject failed = withFailures(object);
        try {
            solver = LiveCircuitSolver.continuing(failed, solver);
        } catch (IllegalArgumentException cannotContinue) {
            solver = new LiveCircuitSolver(failed, solver.interval());
        }
        solver.trace(traceEvery);
        equipSolver();
    }

    /** The traced samples among these as a trace: every node's voltage and every part terminal's current. */
    private static Trace traced(List<LiveCircuitSolver.Sample> samples, List<String> order) {
        List<LiveCircuitSolver.Sample> traced = samples.stream().filter(sample -> sample.terminals() != null).toList();
        if (traced.isEmpty()) {
            return Trace.EMPTY;
        }
        double[] time = new double[traced.size()];
        Map<String, double[]> columns = new LinkedHashMap<>();
        Map<String, double[]> rms = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        for (String node : order) {
            columns.put(node, new double[traced.size()]);
            rms.put(node, new double[traced.size()]);
            units.put(node, CircuitComputations.VOLTS);
        }
        for (int index = 0; index < traced.size(); index++) {
            LiveCircuitSolver.Sample sample = traced.get(index);
            LiveCircuitSolver.Window window = sample.window();
            time[index] = sample.time();
            for (int node = 0; node < order.size(); node++) {
                double voltage = window != null ? window.means()[node] : sample.voltages()[node];
                columns.get(order.get(node))[index] = voltage;
                rms.get(order.get(node))[index] = window != null ? window.rms()[node] : Math.abs(voltage);
            }
            for (Map.Entry<String, Map<String, Double>> part : sample.terminals().entrySet()) {
                Map<String, Double> partRms = window != null ? window.terminalRms().getOrDefault(part.getKey(), Map.of()) : Map.of();
                for (Map.Entry<String, Double> terminal : part.getValue().entrySet()) {
                    String column = CircuitComputations.partCurrentColumn(part.getKey(), terminal.getKey());
                    if (column != null) {
                        double current = terminal.getValue();
                        columns.computeIfAbsent(column, ignored -> unknown(traced.size()))[index] = current;
                        rms.computeIfAbsent(column, ignored -> unknown(traced.size()))[index] = partRms.getOrDefault(terminal.getKey(), Math.abs(current));
                        units.put(column, CircuitComputations.AMPERES);
                    }
                }
            }
        }
        return new Trace(time, columns, units, rms);
    }

    private static double[] unknown(int size) {
        double[] values = new double[size];
        java.util.Arrays.fill(values, Double.NaN);
        return values;
    }

    /**
     * The circuit with new values — a switch closed, a slider moved —, going on from now.
     *
     * @return false when it could not go on (its parts changed, or it is digital) and started again instead
     */
    public synchronized boolean retune(CompiledObject object) {
        if (given != null) {
            given = object;
            displays = PartDisplays.of(object);
            object = withFailures(object);
        }
        try {
            solver = LiveCircuitSolver.continuing(object, solver);
            solver.trace(traceEvery);
            equipSolver();
            return true;
        } catch (IllegalArgumentException cannotContinue) {
            restart(object);
            return false;
        }
    }

    /**
     * Samples every {@code interval} from now on — what an oscilloscope asks for when its time base changes —, going
     * on from where the circuit is; a circuit that cannot go on starts again at the new interval.
     */
    public synchronized void resample(CompiledObject object, double interval) {
        if (given != null) {
            given = object;
            object = withFailures(object);
        }
        try {
            solver = LiveCircuitSolver.continuing(object, solver, interval);
        } catch (IllegalArgumentException cannotContinue) {
            solver = new LiveCircuitSolver(object, interval);
        }
        solver.trace(traceEvery);
        equipSolver();
    }

    /** The circuit as written now, from the beginning: an edit of its text runs at once. */
    public synchronized void restart(CompiledObject object) {
        // From the beginning, with new parts: nothing has failed yet.
        if (given != null) {
            given = object;
            displays = PartDisplays.of(object);
            failures.clear();
        }
        solver = new LiveCircuitSolver(object, solver.interval());
        solver.trace(traceEvery);
        probes = probes.stream().filter(probe -> probe.quantity() != Quantity.VOLTAGE || known(probe.positive()) && known(probe.negative())).toList();
        instruments = instrumentReadings(probes);
        probes = probes.stream().filter(probe -> probe.quantity() != Quantity.READING || instruments.containsKey(probe.channel())).toList();
        equipSolver();
    }

/** A voltage probe's node among the samples' values; a probe on a part reads no node. */
    private static int nodeIndexOf(List<String> order, Probe probe, String node) {
        return probe.quantity() == Quantity.VOLTAGE ? indexOf(order, node) : -2;
    }

        /** A node's place among the samples' values: -1 for ground, which reads 0, and -2 for a node the circuit lacks. */
    private static int indexOf(List<String> order, String node) {
        if (GROUND.contains(node)) {
            return -1;
        }
        int index = order.indexOf(node);
        return index < 0 ? -2 : index;
    }

    /** The current into the probe's terminal of its part — its first when it names none —; NaN for a part not metered. */
    private static double current(LiveCircuitSolver.Sample sample, Probe probe) {
        LiveCircuitSolver.Metered metered = sample.metered() == null ? null : sample.metered().get(probe.part());
        if (metered == null || metered.terminals() == null || metered.terminals().isEmpty()) {
            return Double.NaN;
        }
        // A primitive's first terminal is its "+": its terminals come in no fixed order, and the sign must not depend on it.
        String terminal = probe.terminal() != null ? probe.terminal()
                : metered.terminals().containsKey("+") ? "+" : metered.terminals().keySet().iterator().next();
        Double current = metered.terminals().get(terminal);
        return current == null ? Double.NaN : current;
    }

    private static double power(LiveCircuitSolver.Sample sample, Probe probe) {
        LiveCircuitSolver.Metered metered = sample.metered() == null ? null : sample.metered().get(probe.part());
        return metered == null ? Double.NaN : metered.power();
    }

    /**
     * What an instrument probe reads: one figure of its part's {@code measure} block, compiled for that part, and its unit.
     * The probe names the figure as its terminal; a probe naming none reads the instrument's first.
     */
    private record InstrumentReading(PartExpression expression, String unit) {
    }

    /** Every instrument probe's reading, by channel; a probe on a part that is gone reads nothing and is dropped. */
    private Map<String, InstrumentReading> instrumentReadings(List<Probe> probes) {
        if (given == null || !(given.definition() instanceof ObjectDefinition.Circuit circuit)) {
            return Map.of();
        }
        Map<String, InstrumentReading> readings = new LinkedHashMap<>();
        for (Probe probe : probes) {
            if (probe.quantity() != Quantity.READING) {
                continue;
            }
            ObjectDefinition.CircuitElement element = circuit.elements().stream()
                    .filter(candidate -> candidate.id().equals(probe.part()) && candidate.type() == ObjectDefinition.CircuitElementType.MODEL)
                    .findFirst().orElse(null);
            if (element == null) {
                continue;
            }
            ComponentModel model = ComponentDocuments.read(given.componentSource(element.component()), element.component(), given.library());
            if (!model.isInstrument()) {
                throw new IllegalArgumentException("'" + element.id() + "' is no instrument: its component has no measure block.");
            }
            String figure = probe.terminal() != null ? probe.terminal() : model.measures().keySet().iterator().next();
            ComponentModel.Measure measure = model.measures().get(figure);
            if (measure == null) {
                throw new IllegalArgumentException("'" + element.id() + "' measures " + String.join(", ", model.measures().keySet())
                        + "; it has no '" + figure + "'.");
            }
            readings.put(probe.channel(), new InstrumentReading(PartExpression.of(given, element, model, measure.expression()), measure.unit()));
        }
        return readings;
    }

    private String readingUnit(Probe probe) {
        InstrumentReading reading = instruments.get(probe.channel());
        return reading == null ? "" : reading.unit();
    }

    /** What an instrument reads at a sample, from the voltages of its pins and what its part is doing then. */
    private double reading(LiveCircuitSolver.Sample sample, Probe probe, int[] pinNodes) {
        InstrumentReading reading = instruments.get(probe.channel());
        if (reading == null) {
            return Double.NaN;
        }
        double[] voltages = new double[pinNodes.length];
        for (int pin = 0; pin < pinNodes.length; pin++) {
            voltages[pin] = voltage(sample, pinNodes[pin]);
        }
        LiveCircuitSolver.Metered metered = sample.metered() == null ? null : sample.metered().get(probe.part());
        return reading.expression().read(voltages, metered, sample.time());
    }

    private static double voltage(LiveCircuitSolver.Sample sample, int index) {
        return index == -1 ? 0 : index < 0 ? Double.NaN : sample.voltages()[index];
    }

    private boolean known(String node) {
        return GROUND.contains(node) || solver.nodes().contains(node);
    }

    private void requireNode(String node) {
        if (!known(node)) {
            throw new IllegalArgumentException("The circuit has no node '" + node + "' to probe.");
        }
    }
}
