package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Setting;
import org.jmouse.cad.component.Figure;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;
import org.jmouse.cad.simulation.elaboration.ElaboratedPart;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One running circuit as a simulator watches it: the solver, and the probes measured on it. Each frame carries what the
 * probes read — a channel is the voltage between its two nodes, computed here, not by whoever displays it — and the
 * voltages of the probed nodes themselves.
 *
 * <p>A change of value goes on from the present ({@link #retune}); a change of the circuit's text starts it again
 * ({@link #restart}). On a realistic bench a part past its limit fails and the run goes on with it failed: the failure is
 * written into the bench and the circuit elaborated again on it ({@link PartFailures}). Every method is synchronised:
 * frames are produced by one thread while changes arrive on another.</p>
 *
 * <p>It keeps where the run stood a moment ago ({@link #checkpoint}), and a stretch the run has passed can be
 * {@link #capture}d again from there, as finely as asked, while the run goes on: an edge a few nanoseconds long, seen in
 * a stream sampled every ten microseconds, drawn as it happened.</p>
 */
public final class LiveCircuitSession {

    /**
     * How many checkpoints are kept: the newest a frame apart, the older ones thinned to reach far back
     * ({@link #thinCheckpoints}).
     */
    private static final int KEPT_CHECKPOINTS = 64;
    /** How many samples a capture has at most. */
    private static final int CAPTURED_SAMPLES = 200_000;
    /** How many samples a replay is advanced by at a time. */
    private static final int REPLAY_BATCH = 256;

    /**
     * What a probe reads: a voltage between two nodes, the current through a part, the power a part takes, what an
     * instrument part reads — its own {@code measure}, in its own unit —, or a logic analyser's channel: a digital node's
     * level (1, 0, −1 unknown, −2 let go), or an analog node's voltage for the analyser to read through its thresholds.
     */
    public enum Quantity {
        VOLTAGE(ColumnNames.VOLTS), CURRENT(ColumnNames.AMPERES), POWER(ColumnNames.WATTS), READING(null), LEVEL(ColumnNames.LEVEL);

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
     * {@code negative}; or, on {@code part}, the current into its {@code terminal} (its first when none is named), the
     * power it takes, negative for a source delivering it, or an instrument's figure named as its terminal.
     */
    public record Probe(String channel, Quantity quantity, String positive, String negative, String part, String terminal) {
        public Probe {
            quantity = quantity == null ? Quantity.VOLTAGE : quantity;
        }
    }

    /**
     * What a session runs: a circuit as a measurement takes it — its catalogue, its bench —, and what a reader set on it:
     * a slider by its parameter's name, a live part's setting as {@code S1.closed}.
     */
    public record LiveCircuit(MeasuredCircuit measured, List<Setting> with) {

        public LiveCircuit {
            with = List.copyOf(with);
        }

        BenchConditions bench() {
            return measured.bench();
        }

        /** The circuit as it is simulated on a bench: its own, or its own with the parts that failed failed on it. */
        ElaboratedCircuit elaborate(BenchConditions on) {
            return new MeasuredCircuit(measured.circuit(), measured.catalogue(), on, measured.fixtures()).elaborated(with);
        }
    }

    /**
     * Samples as they came out.
     *
     * @param time     each sample's time, in seconds
     * @param channels each probe's readings, by channel
     * @param units    each channel's unit: "V", "A", "W", "level" or an instrument's own
     * @param levels   every node's mean voltage over the frame — what a part's display figures are read from
     * @param parts    how hard every part worked over the frame, by part — what a schematic shows as load and heat
     * @param trace    the whole circuit at the traced samples — what a schematic plays its currents from
     * @param covered  how many samples the frame went through: its samples, or more when it passed some unread
     * @param peaks    each voltage channel's lowest and highest reading over every sample's interval, by channel
     */
    public record Frame(double[] time, Map<String, double[]> channels, Map<String, String> units, Map<String, Double> levels,
                        Map<String, PartLoad> parts, Trace trace, long covered, Map<String, Peaks> peaks) {
    }

    /** A channel's lowest, highest and mean reading over each sample's interval, sample by sample. */
    public record Peaks(double[] lows, double[] highs, double[] means) {
    }

    /**
     * The circuit at some of a frame's samples ({@link #trace(int)}): every node's voltage and every part's current — the
     * columns and units a transient reports them in. Empty while nothing is traced.
     *
     * @param rms each column's RMS over the stretch each moment stands for, by column
     */
    public record Trace(double[] time, Map<String, double[]> columns, Map<String, String> units, Map<String, double[]> rms) {

        public static final Trace EMPTY = new Trace(new double[0], Map.of(), Map.of(), Map.of());
    }

    private LiveCircuitSolver solver;
    private List<Probe>       probes;
    /** The circuit as the run was given it, before any part failed. */
    private LiveCircuit       given;
    /** The parts that failed on a realistic bench during this run: kept through every change until a restart. */
    private final PartFailures failures = new PartFailures();
    /** What each part shows on its symbol. */
    private PartDisplays      displays;
    /** Every how many samples the whole circuit is traced; 0 traces none. */
    private int               traceEvery;
    /** What the probes read off a frame's samples, built for the probes and the solver as they are now. */
    private FrameReader       reader;
    /** Where the run stood at its last frames, oldest first; emptied by any change, which the past did not have. */
    private final Deque<LiveCircuitSolver.Checkpoint> checkpoints = new ArrayDeque<>();

    /**
     * @throws IllegalArgumentException when the circuit does not elaborate, or a probe names what it does not have
     */
    public LiveCircuitSession(LiveCircuit circuit, double sampleInterval, List<Probe> probes) {
        this.given = circuit;
        this.solver = new LiveCircuitSolver(circuit.elaborate(circuit.bench()), sampleInterval);
        this.displays = PartDisplays.of(solver.elaborated());
        probe(probes);
    }

    public synchronized List<String> nodes() {
        return solver.nodes();
    }

    public synchronized double time() {
        return solver.time();
    }

    public synchronized double interval() {
        return solver.interval();
    }

    /** What a reader can work on the circuit as it runs now. */
    public synchronized List<LiveControls.LiveControl> controls() {
        return LiveControls.of(solver.elaborated());
    }

    public synchronized List<Probe> probes() {
        return probes;
    }

    /** The probes read from now on; one naming what the circuit does not have is refused. */
    public synchronized void probe(List<Probe> wanted) {
        // A digital net has a level and no voltage: an oscilloscope's tip on one reads the level, as a logic channel does.
        List<Probe> read = wanted.stream().map(this::onDigitalNode).toList();
        for (Probe probe : read) {
            if (probe.quantity() == Quantity.VOLTAGE) {
                requireNode(probe.positive());
                requireNode(probe.negative());
            } else if (probe.quantity() == Quantity.LEVEL) {
                requireLevelNode(probe.positive());
            } else if (probe.part() == null || probe.part().isBlank()) {
                throw new IllegalArgumentException("A " + probe.quantity().name().toLowerCase(Locale.ROOT) + " probe names its part.");
            }
        }
        this.probes = List.copyOf(read);
        equipSolver();
    }

    /**
     * Tells the solver — a new one after every change that could not go on — what the probes need read: the parts they
     * meter and the differences whose peaks they show; and reads its instruments again over the circuit it now runs.
     */
    private void equipSolver() {
        equip(solver);
        reader = new FrameReader(probes, instrumentReadings(probes), solver.nodes());
    }

    /**
     * Lets the checkpoints go once the circuit is another: other values, a part failed, a new text. Replayed from them,
     * the past would be computed as a circuit it never was. Other probes or another sample interval change nothing of
     * what the circuit did, so they keep them.
     */
    private void forgetPast() {
        checkpoints.clear();
    }

    /** Tells a solver of this circuit what the probes need read: the parts they meter, the differences they show. */
    private void equip(LiveCircuitSolver target) {
        List<String> order = target.nodes();
        target.meter(probes.stream().filter(LiveCircuitSession::readsPart).map(Probe::part).collect(Collectors.toSet()));
        target.difference(probes.stream().filter(probe -> FrameReader.watchesDifference(probe, order))
                .map(probe -> new NodeDifference(
                        NodeReference.of(order, probe.positive()).solverIndex(), NodeReference.of(order, probe.negative()).solverIndex()))
                .toList());
    }

    /** Keeps where the run stands now, for a capture to go back to: a few arrays copied, cheap enough for every frame. */
    public synchronized void checkpoint() {
        checkpoints.addLast(solver.checkpoint());
        if (checkpoints.size() > KEPT_CHECKPOINTS) {
            thinCheckpoints();
        }
    }

    /**
     * Lets one checkpoint go so that the rest reach as far back as before: the one whose going leaves the smallest gap
     * against how old it is. The newest stay a frame apart and the older ones space out, so a fixed number of them
     * reaches as far back as a screen can be dragged. The oldest is kept: it is how far the reach goes.
     */
    private void thinCheckpoints() {
        List<LiveCircuitSolver.Checkpoint> kept = new ArrayList<>(checkpoints);
        double now = kept.getLast().time();
        int dropped = -1;
        double smallest = Double.POSITIVE_INFINITY;
        for (int index = 1; index < kept.size() - 1; index++) {
            double gap = kept.get(index + 1).time() - kept.get(index - 1).time();
            double age = Math.max(now - kept.get(index).time(), Double.MIN_NORMAL);
            if (gap / age < smallest) {
                smallest = gap / age;
                dropped = index;
            }
        }
        kept.remove(dropped);
        checkpoints.clear();
        checkpoints.addAll(kept);
    }

    /**
     * A stretch the run has passed, from {@code from} to {@code to}, measured again on {@code samples} samples: replayed
     * from the last checkpoint before it — the run itself goes on untouched —, solved to {@code from} as fast as accuracy
     * allows, and sampled finely across it. The frame carries the probes' channels and peaks only.
     *
     * <p>Only the replay's preparing holds the session: the replay itself runs on a solver of its own, so the run's
     * frames go on while a long stretch is computed.</p>
     *
     * @throws IllegalArgumentException when the stretch is older than the checkpoints kept, or not yet run
     */
    public Frame capture(double from, double to, int samples) {
        return prepareReplay(from, to, samples).run();
    }

    /** A replay ready to run: its own solver, equipped as the run's, and its own reader of the same probes. */
    private record Replay(LiveCircuitSolver solver, FrameReader reader, double from, int samples) {

        Frame run() {
            solver.skipTo(from);
            List<LiveSample> captured = new ArrayList<>(samples);
            while (captured.size() < samples) {
                captured.addAll(solver.advance(Math.min(REPLAY_BATCH, samples - captured.size())));
            }
            double[] time = captured.stream().mapToDouble(LiveSample::time).toArray();
            return new Frame(time, reader.channels(captured), reader.units(), Map.of(), Map.of(), Trace.EMPTY, captured.size(),
                    reader.peaks(captured));
        }
    }

    private synchronized Replay prepareReplay(double from, double to, int samples) {
        if (!(to > from) || samples < 2 || samples > CAPTURED_SAMPLES) {
            throw new IllegalArgumentException("Capture a stretch forward in time, on 2…" + CAPTURED_SAMPLES + " samples.");
        }
        if (to > solver.time()) {
            throw new IllegalArgumentException("That stretch has not been run yet.");
        }
        LiveCircuitSolver.Checkpoint start = null;
        for (LiveCircuitSolver.Checkpoint candidate : checkpoints) {
            if (candidate.time() <= from) {
                start = candidate;
            }
        }
        if (start == null) {
            throw new IllegalArgumentException("That stretch is older than the run keeps; capture a later edge.");
        }
        LiveCircuitSolver replay = LiveCircuitSolver.continuing(given.elaborate(failures.applyTo(given.bench())), start, solver,
                (to - from) / (samples - 1));
        equip(replay);
        return new Replay(replay, new FrameReader(probes, instrumentReadings(probes), replay.nodes()), from, samples);
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
        List<LiveSample> samples = solver.advance(count);
        double[] time = samples.stream().mapToDouble(LiveSample::time).toArray();
        FrameReader frame = reader;
        Map<String, Double> levels = frame.meanLevels(samples);
        Map<String, PartLoad> loads = shown(solver.loads(), levels, samples);
        burnOut(loads);
        return new Frame(time, frame.channels(samples), frame.units(), levels, failures.withCauses(loads), frame.trace(samples),
                samples.size(), frame.peaks(samples));
    }

    /**
     * Goes through the next {@code count} samples without measuring them: a frame with no samples, but the parts'
     * loads, the trace and the levels of the stretch — the traced moments' means — as a measured one has them.
     */
    public synchronized Frame pass(long count) {
        List<LiveSample> traced = solver.pass(count);
        FrameReader frame = reader;
        Map<String, Double> levels = frame.meanLevels(traced);
        Map<String, PartLoad> loads = shown(solver.loads(), levels, traced);
        burnOut(loads);
        return new Frame(new double[0], frame.emptyChannels(), frame.units(), levels, failures.withCauses(loads),
                frame.trace(traced), count, Map.of());
    }

    /** The loads with what each part's symbol shows over the frame: its figures, and each stress at its worst. */
    private Map<String, PartLoad> shown(Map<String, PartLoad> loads, Map<String, Double> levels, List<LiveSample> samples) {
        if (displays == null || displays.isEmpty()) {
            return loads;
        }
        double lastTime = samples.isEmpty() ? Double.NaN : samples.getLast().time();
        Map<String, Map<String, Double>> figures = displays.read(levels, loads, samples, solver.states(), lastTime);
        displays.worstStresses(samples, solver.nodes(), loads, solver.states()).forEach((part, worst) ->
                figures.computeIfAbsent(part, ignored -> new LinkedHashMap<>()).putAll(worst));
        Map<String, Map<String, String>> pictures = displays.pictures(solver.states());
        Set<String> showing = new LinkedHashSet<>(figures.keySet());
        showing.addAll(pictures.keySet());
        Map<String, PartLoad> shown = new LinkedHashMap<>(loads);
        for (String part : showing) {
            PartLoad load = shown.get(part);
            shown.put(part, (load != null ? load : new PartLoad(0, Map.of(), Double.NaN))
                    .showing(figures.getOrDefault(part, Map.of()), displays.momentsOf(part), pictures.getOrDefault(part, Map.of())));
        }
        return shown;
    }

    /** On a realistic bench, every part past its limit fails now; the circuit goes on with it failed. */
    private void burnOut(Map<String, PartLoad> loads) {
        if (PartFailures.realistic(solver.elaborated()) && failures.burnOut(loads, solver)) {
            goOn(given.elaborate(failures.applyTo(given.bench())));
        }
    }

    /** The circuit goes on from where it is, with its failures; one whose parts changed starts again, as warm as it was. */
    private void goOn(ElaboratedCircuit failed) {
        try {
            solver = LiveCircuitSolver.continuing(failed, solver);
        } catch (IllegalArgumentException cannotContinue) {
            solver = new LiveCircuitSolver(failed, solver.interval()).carryingHeat(solver);
        }
        solver.trace(traceEvery);
        equipSolver();
        forgetPast();
    }

    /**
     * The circuit with new values — a switch closed, a slider moved —, going on from now.
     *
     * @return false when it could not go on (its parts changed, or it is digital) and started again instead
     */
    public synchronized boolean retune(LiveCircuit circuit) {
        given = circuit;
        try {
            solver = LiveCircuitSolver.continuing(circuit.elaborate(failures.applyTo(circuit.bench())), solver);
            displays = PartDisplays.of(solver.elaborated());
            solver.trace(traceEvery);
            equipSolver();
            forgetPast();
            return true;
        } catch (IllegalArgumentException cannotContinue) {
            restart(circuit);
            return false;
        }
    }

    /**
     * Samples every {@code interval} from now on — what an oscilloscope asks for when its time base changes —, going on
     * from where the circuit is; a circuit that cannot go on starts again at the new interval.
     */
    public synchronized void resample(double interval) {
        ElaboratedCircuit elaborated = given.elaborate(failures.applyTo(given.bench()));
        try {
            solver = LiveCircuitSolver.continuing(elaborated, solver, interval);
        } catch (IllegalArgumentException cannotContinue) {
            solver = new LiveCircuitSolver(elaborated, interval);
            forgetPast();
        }
        solver.trace(traceEvery);
        equipSolver();
    }

    /** The circuit as written now, from the beginning: an edit of its text runs at once. */
    public synchronized void restart(LiveCircuit circuit) {
        // From the beginning, with new parts: nothing has failed yet.
        given = circuit;
        failures.clear();
        solver = new LiveCircuitSolver(circuit.elaborate(circuit.bench()), solver.interval());
        displays = PartDisplays.of(solver.elaborated());
        solver.trace(traceEvery);
        probes = probes.stream().filter(probe -> probe.quantity() != Quantity.VOLTAGE || known(probe.positive()) && known(probe.negative()))
                .filter(probe -> probe.quantity() != Quantity.LEVEL || knownLevel(probe.positive())).toList();
        Map<String, FrameReader.InstrumentReading> instruments = instrumentReadings(probes);
        probes = probes.stream().filter(probe -> probe.quantity() != Quantity.READING || instruments.containsKey(probe.channel())).toList();
        equipSolver();
        forgetPast();
    }

    /**
     * Every instrument probe's reading, by channel, compiled over its part as it stands on the bench; a probe on a part
     * that is gone reads nothing.
     */
    private Map<String, FrameReader.InstrumentReading> instrumentReadings(List<Probe> read) {
        ElaboratedCircuit circuit = solver.elaborated();
        Map<String, FrameReader.InstrumentReading> readings = new LinkedHashMap<>();
        for (Probe probe : read) {
            if (probe.quantity() != Quantity.READING) {
                continue;
            }
            ElaboratedPart part = circuit.written().parts().stream().filter(candidate -> candidate.reference().equals(probe.part()))
                    .findFirst().orElse(null);
            if (part == null) {
                continue;
            }
            if (!part.component().instrument()) {
                throw new IllegalArgumentException("'" + part.reference() + "' is no instrument: its component has no measure block.");
            }
            List<Figure> figures = part.component().readings();
            Figure figure = probe.terminal() == null ? figures.getFirst() : figures.stream()
                    .filter(candidate -> candidate.name().equals(probe.terminal())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("'" + part.reference() + "' measures "
                            + String.join(", ", figures.stream().map(Figure::name).toList()) + "; it has no '" + probe.terminal() + "'."));
            if (!(figure.value() instanceof Formula formula)) {
                throw new IllegalArgumentException("'" + part.reference() + "' reads " + figure.name() + " as words, not as a figure.");
            }
            readings.put(probe.channel(), new FrameReader.InstrumentReading(PartExpression.of(circuit, part, formula.written()),
                    figure.unit()));
        }
        return readings;
    }

    private boolean known(String node) {
        return !(NodeReference.of(solver.nodes(), node) instanceof NodeReference.Absent);
    }

    private void requireNode(String node) {
        if (!known(node)) {
            throw new IllegalArgumentException("The circuit has no node '" + node + "' to probe.");
        }
    }

    /** A voltage probe whose tip is on a digital node and whose clip is on ground, as the level probe it can only be. */
    private Probe onDigitalNode(Probe probe) {
        boolean grounded = probe.negative() == null || NodeReference.of(solver.nodes(), probe.negative()) == NodeReference.GROUND;
        if (probe.quantity() == Quantity.VOLTAGE && grounded && !known(probe.positive()) && solver.levels().containsKey(probe.positive())) {
            return new Probe(probe.channel(), Quantity.LEVEL, probe.positive(), probe.negative(), null, null);
        }
        return probe;
    }

    /** Whether a logic channel can read this node: a digital one the event solver keeps, or an analog one's voltage. */
    private boolean knownLevel(String node) {
        return known(node) || solver.levels().containsKey(node);
    }

    private void requireLevelNode(String node) {
        if (!knownLevel(node)) {
            throw new IllegalArgumentException("The circuit has no node '" + node + "' for a logic channel to read.");
        }
    }

    /** Whether a probe reads a part — its current, its power, its instrument's figure — and so the part is metered. */
    private static boolean readsPart(Probe probe) {
        return probe.quantity() == Quantity.CURRENT || probe.quantity() == Quantity.POWER || probe.quantity() == Quantity.READING;
    }
}
