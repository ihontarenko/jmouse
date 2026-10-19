package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One running circuit as a simulator watches it: the solver, and the probes measured on it. Each frame carries what the
 * probes read — a channel is the voltage between its two nodes, computed here, not by whoever displays it — and the
 * voltages of the probed nodes themselves.
 *
 * <p>A change of value goes on from the present ({@link #retune}); a change of the circuit's text starts it again
 * ({@link #restart}). On a realistic bench a part past its limit fails and the run goes on with it failed
 * ({@link PartFailures}). What a frame reads off the samples is the {@link FrameReader}'s. Every method is synchronised:
 * frames are produced by one thread while changes arrive on another.</p>
 *
 * <p>It keeps where the run stood a moment ago ({@link #checkpoint}), and a stretch the run has passed can be
 * {@link #capture}d again from there, as finely as asked, while the run goes on: an edge a few nanoseconds long, seen in
 * a stream sampled every ten microseconds, drawn as it happened.</p>
 */
public final class LiveCircuitSession {

    /** How many checkpoints are kept: a frame each, so about two seconds of a run. */
    private static final int KEPT_CHECKPOINTS = 64;
    /** How many samples a capture has at most. */
    private static final int CAPTURED_SAMPLES = 200_000;

    /**
     * What a probe reads: a voltage between two nodes, the current through a part, the power a part takes, or what an
     * instrument part reads — its own `measure`, in its own unit.
     */
    public enum Quantity {
        VOLTAGE(ColumnNames.VOLTS), CURRENT(ColumnNames.AMPERES), POWER(ColumnNames.WATTS), READING(null);

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
    /** The parts that failed on a realistic bench during this run: kept through every change until a restart. */
    private final PartFailures failures = new PartFailures();
    /** What each part shows on its symbol; none for a session given only a solver. */
    private PartDisplays      displays;
    /** Every how many samples the whole circuit is traced; 0 traces none. */
    private int               traceEvery;
    /** What the probes read off a frame's samples, built for the probes and the solver as they are now. */
    private FrameReader       reader;
    /** Where the run stood at its last frames, oldest first; emptied by any change, which the past did not have. */
    private final Deque<LiveCircuitSolver.Checkpoint> checkpoints = new ArrayDeque<>();

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
        this.displays = PartDisplays.of(solver.elaborated());
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
        this.probes = List.copyOf(probes);
        equipSolver();
    }

    /**
     * Tells the solver — a new one after every change that could not go on — what the probes need read: the parts they
     * meter and the differences whose peaks they show; and reads its instruments again over the circuit it now runs.
     */
    private void equipSolver() {
        equip(solver);
        reader = new FrameReader(probes, instrumentReadings(probes), solver.nodes());
        checkpoints.clear();
    }

    /** Tells a solver of this circuit what the probes need read: the parts they meter, the differences they show. */
    private void equip(LiveCircuitSolver target) {
        List<String> order = target.nodes();
        target.meter(probes.stream().filter(probe -> probe.quantity() != Quantity.VOLTAGE).map(Probe::part).collect(Collectors.toSet()));
        target.difference(probes.stream().filter(probe -> FrameReader.watchesDifference(probe, order))
                .map(probe -> new NodeDifference(
                        NodeReference.of(order, probe.positive()).solverIndex(), NodeReference.of(order, probe.negative()).solverIndex()))
                .toList());
    }

    /**
     * Keeps where the run stands now, for a capture to go back to: a few arrays copied, cheap enough for every frame.
     * Only a session that knows its circuit can replay it.
     */
    public synchronized void checkpoint() {
        if (given == null) {
            return;
        }
        checkpoints.addLast(solver.checkpoint());
        while (checkpoints.size() > KEPT_CHECKPOINTS) {
            checkpoints.removeFirst();
        }
    }

    /**
     * A stretch the run has passed, from {@code from} to {@code to}, measured again on {@code samples} samples: replayed
     * from the last checkpoint before it — the run itself goes on untouched —, solved to {@code from} as fast as accuracy
     * allows, and sampled finely across it. The frame carries the probes' channels and peaks only.
     *
     * <p>Only the replay's preparing holds the session: the replay itself runs on a solver of its own, so the run's
     * frames go on while a stretch of eighty thousand samples is computed. Held all through, it stopped them for a tenth
     * of a second at every edge, and the bench stuttered.</p>
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
                captured.addAll(solver.advance(Math.min(256, samples - captured.size())));
            }
            double[] time = captured.stream().mapToDouble(LiveSample::time).toArray();
            return new Frame(time, reader.channels(captured), reader.units(), Map.of(), Map.of(), Trace.EMPTY, captured.size(),
                    reader.peaks(captured));
        }
    }

    private synchronized Replay prepareReplay(double from, double to, int samples) {
        if (given == null) {
            throw new IllegalArgumentException("This run cannot be replayed: it does not know its circuit.");
        }
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
        LiveCircuitSolver replay = LiveCircuitSolver.continuing(failures.applyTo(given), start, solver, (to - from) / (samples - 1));
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

    /**
     * The loads with what each part's symbol shows over the frame: a figure read from its mean levels at its last moment,
     * a stress at its worst over the frame's samples ({@link PartDisplays#worstStresses}).
     */
    private Map<String, PartLoad> shown(Map<String, PartLoad> loads, Map<String, Double> levels,
                                        List<LiveSample> samples) {
        if (displays == null || displays.isEmpty()) {
            return loads;
        }
        double lastTime = samples.isEmpty() ? Double.NaN : samples.getLast().time();
        Map<String, Map<String, Double>> figures = displays.read(levels, loads, solver.states(), lastTime);
        displays.worstStresses(samples, solver.nodes(), loads, solver.states()).forEach((part, worst) ->
                figures.computeIfAbsent(part, ignored -> new LinkedHashMap<>()).putAll(worst));
        Map<String, PartLoad> shown = new LinkedHashMap<>(loads);
        figures.forEach((part, values) -> {
            PartLoad load = shown.get(part);
            shown.put(part, (load != null ? load : new PartLoad(0, Map.of(), Double.NaN)).showing(values));
        });
        return shown;
    }

    /** On a realistic bench, every part past its limit fails now; the circuit goes on with it failed. */
    private void burnOut(Map<String, PartLoad> loads) {
        if (given != null && PartFailures.realistic(given) && failures.burnOut(loads, solver)) {
            goOn(failures.applyTo(given));
        }
    }

    /** The circuit goes on from where it is, with its failures; one whose parts changed starts again, as warm as it was. */
    private void goOn(CompiledObject failed) {
        try {
            solver = LiveCircuitSolver.continuing(failed, solver);
        } catch (IllegalArgumentException cannotContinue) {
            solver = new LiveCircuitSolver(failed, solver.interval()).carryingHeat(solver);
        }
        solver.trace(traceEvery);
        equipSolver();
    }

    /**
     * The circuit with new values — a switch closed, a slider moved —, going on from now.
     *
     * @return false when it could not go on (its parts changed, or it is digital) and started again instead
     */
    public synchronized boolean retune(CompiledObject object) {
        if (given != null) {
            given = object;
            object = failures.applyTo(object);
        }
        try {
            solver = LiveCircuitSolver.continuing(object, solver);
            if (given != null) {
                displays = PartDisplays.of(solver.elaborated());
            }
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
            object = failures.applyTo(object);
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
            failures.clear();
        }
        solver = new LiveCircuitSolver(object, solver.interval());
        if (given != null) {
            displays = PartDisplays.of(solver.elaborated());
        }
        solver.trace(traceEvery);
        probes = probes.stream().filter(probe -> probe.quantity() != Quantity.VOLTAGE || known(probe.positive()) && known(probe.negative())).toList();
        Map<String, FrameReader.InstrumentReading> instruments = instrumentReadings(probes);
        probes = probes.stream().filter(probe -> probe.quantity() != Quantity.READING || instruments.containsKey(probe.channel())).toList();
        equipSolver();
    }

    /**
     * Every instrument probe's reading, by channel, compiled over its part as it stands on the bench; a probe on a part
     * that is gone reads nothing.
     */
    private Map<String, FrameReader.InstrumentReading> instrumentReadings(List<Probe> probes) {
        ElaboratedCircuit circuit = solver.elaborated();
        Map<String, FrameReader.InstrumentReading> readings = new LinkedHashMap<>();
        for (Probe probe : probes) {
            if (probe.quantity() != Quantity.READING) {
                continue;
            }
            ObjectDefinition.CircuitElement element = circuit.writtenParts().stream()
                    .filter(candidate -> candidate.id().equals(probe.part()) && candidate.type() == ObjectDefinition.CircuitElementType.MODEL)
                    .findFirst().orElse(null);
            if (element == null) {
                continue;
            }
            ComponentModel model = circuit.model(element.component());
            if (!model.isInstrument()) {
                throw new IllegalArgumentException("'" + element.id() + "' is no instrument: its component has no measure block.");
            }
            String figure = probe.terminal() != null ? probe.terminal() : model.measures().keySet().iterator().next();
            ComponentModel.Measure measure = model.measures().get(figure);
            if (measure == null) {
                throw new IllegalArgumentException("'" + element.id() + "' measures " + String.join(", ", model.measures().keySet())
                        + "; it has no '" + figure + "'.");
            }
            readings.put(probe.channel(), new FrameReader.InstrumentReading(
                    PartExpression.of(circuit, element, model, measure.expression()), measure.unit()));
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
}
