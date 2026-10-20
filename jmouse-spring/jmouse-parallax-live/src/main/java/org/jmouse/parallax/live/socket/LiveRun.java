package org.jmouse.parallax.live.socket;

import org.jmouse.parallax.live.LivePrincipal;
import org.jmouse.parallax.circuit.LiveCircuitSession;
import org.jmouse.parallax.circuit.LiveControls;
import org.jmouse.parallax.circuit.PartLoad;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.circuit.LiveCircuitSolver;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * One socket's running circuit. The server steps it and streams what its probes read; the reader starts and pauses it,
 * moves the probes, works its live parts and edits its text — each as a message, and each without a page reload.
 *
 * <h2>Messages</h2>
 * <pre>
 * → start   { objectId, values, interval, probes, [speed], [definition] }   ← started { nodes, controls, interval }
 * → pause · resume · step · reset                                    ← frame, binary — see {@link FrameEncoding}
 * → probes  { probes: [{ channel, positive, negative }] }
 * → sample  { interval } — sample every interval from now on; the circuit goes on, it does not start again
 * → acquire { gapless } — true: every sample is read, none passed, and the run plays no faster than that allows — what
 *                       a deep time base needs to see one edge in detail; false: real time first, the screen's block a frame
 * → pace    { speed } — circuit seconds per second of wall time: 1 is real time, 0.01 a hundred times slower; the slower
 *                       it plays, the more of its samples a frame traces for the sheet
 * → set     { part, parameter, value }  or  { parameter, value } for the circuit's own   ← changed { continued, controls }
 * → restart { source } or { definition }, [interval], [probes] — the edited circuit, run at once with its probes   ← started
 * → retune  { definition } — the same circuit with other values (a part's value or model values changed on the bench):
 *                       the run goes on from now, and starts again only when its parts changed   ← changed { continued }
 * → capture { from, to, samples } — a stretch the run passed, replayed from where it stood just before and sampled
 *                       finely; the run goes on   ← capture, binary like a frame, its header's type "capture"
 *                                                                                ← problem { message }
 * </pre>
 */
final class LiveRun {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveRun.class);

    /**
     * How fast a circuit plays, as a share of real time, and the bounds a reader may ask for: real time by default, as
     * a bench oscilloscope shows it. What a frame cannot compute within its budget is not owed to the next one — the
     * circuit plays slower, and the reader's clock says by how much.
     */
    private static final double REAL_TIME = 1;
    private static final double SLOWEST   = 1e-5;
    private static final double FASTEST   = 100;
    private static final long   FRAME_MILLISECONDS = 40;
    /**
     * How long one frame may compute, and in what pieces: a circuit the processor cannot keep up with — a timer's every
     * edge solved through its comparators — plays slower and smoothly, instead of in long silent jerks.
     */
    private static final long FRAME_BUDGET_NANOSECONDS = 35_000_000;
    /**
     * The share of a frame's period a held speed may fill: the budget is 35 of every 40 ms, and a frame heavier than
     * the average — more edges in it — still fits.
     */
    private static final double SUSTAINED_SHARE = 0.65;
    private static final int  SAMPLES_PER_PIECE = 64;
    /**
     * How many samples a frame sends at most — four screens of a thousand, a hundred thousand a second. A real-time run
     * on a 200 ns sample interval computes five million a second, which no screen shows and no socket should carry:
     * like a digital oscilloscope, the frame keeps one contiguous block, its acquisition, and goes through the rest
     * unread — the circuit, its parts' energy and its trace for the sheet still go through every sample. Reading a
     * sample costs ten times passing one; sixteen thousand a frame took half the server's time whatever the speed.
     */
    private static final int  SAMPLES_SHOWN_PER_FRAME = 4_096;
    /** How many unread samples are gone through between two looks at the frame's time budget. */
    private static final int  SAMPLES_PASSED_PER_PIECE = 4_096;
    /**
     * How many moments of the whole circuit a frame carries at most — every node's voltage, every part's current —,
     * which the sheet plays its currents from: one each 2.5 ms of wall time, finer than a screen redraws. Slowed down,
     * that is every sample; at real time, every few dozen.
     */
    private static final int  TRACED_PER_FRAME = 16;

    /** What a reader sends; every field but {@code type} belongs to some messages only. */
    record Message(String type, String objectId, String source, ObjectDefinition definition, Map<String, Double> values,
                   Double interval, Double speed, List<LiveCircuitSession.Probe> probes, String part,
                   String parameter, Double value, Boolean gapless, Double from, Double to, Integer samples) {
    }

    private final LiveCircuits     circuits;
    private final LivePrincipal    principal;
    private final ObjectMapper     mapper;
    private final LiveOutput       output;

    private MathematicalObject          object;
    private ObjectDefinition.Circuit    circuit;
    private Map<String, Double>         values = Map.of();
    /** The sliders of an edited text, which the saved object does not have yet; null while it runs as saved. */
    private List<ParameterDefinition>   parameters;
    private double                      interval;
    private List<LiveCircuitSession.Probe> probes = List.of();
    private LiveCircuitSession          session;
    private List<LiveControls.LiveControl> controls = List.of();
    private ScheduledFuture<?>          ticking;
    /** Whether this run is counted among the circuits stepping now. */
    private boolean                     counted;
    private volatile double             speed = REAL_TIME;
    /**
     * The speed it plays at: the speed asked for, or — while the server cannot keep up — a steady speed it can, with room
     * to spare. A frame cut short by its budget plays whatever it reached, a different share each frame, and the screen
     * playing that stream lurches; held at a speed the server keeps, it moves evenly, and creeps back up while the
     * server has time left.
     */
    private double                      played = REAL_TIME;
    /**
     * Whether every sample is read: a frame never passes any, and the run plays no faster than
     * {@link #SAMPLES_SHOWN_PER_FRAME} samples a frame allow. At a fine time base the screen then holds an unbroken
     * record — every edge in it, the trigger finding each —, the run slowed to show it, as a reader zooming into one
     * transistor's switching wants; off, the run keeps real time and the screen holds one block a frame, gaps between.
     */
    private volatile boolean            gapless;
    /** What a sample read, a sample passed and a frame besides them cost, in nanoseconds, smoothed; NaN until measured. */
    private double                      readCost = Double.NaN;
    private double                      passCost = Double.NaN;
    private double                      frameCost = Double.NaN;
    /** Samples this run is behind its speed by, a fraction of one carried from frame to frame. */
    private double                      due;
    /** Whether a capture is being replayed: one at a time — an edge found while it runs is let go. */
    private final AtomicBoolean         capturing = new AtomicBoolean();
    /** Every how many samples the session traces the whole circuit now. */
    private int                         tracedEvery = 1;

    LiveRun(LiveCircuits circuits, LivePrincipal principal, ObjectMapper mapper, LiveOutput output) {
        this.circuits = circuits;
        this.principal = principal;
        this.mapper = mapper;
        this.output = output;
    }

    void receive(String text) {
        try {
            Message message = mapper.readValue(text, Message.class);
            principal.runAs(() -> {
                dispatch(message);
                return null;
            });
        } catch (ParallaxProblem | IllegalArgumentException | IllegalStateException refused) {
            pause();
            reply(Map.of("type", "problem", "message", refused.getMessage() == null ? "The circuit stopped." : refused.getMessage()));
        } catch (RuntimeException unreadable) {
            reply(Map.of("type", "problem", "message", "The message was not understood: " + unreadable.getMessage()));
        }
    }

    private void dispatch(Message message) {
        switch (message.type() == null ? "" : message.type()) {
            case "start" -> start(message);
            case "pause" -> pause();
            case "resume" -> resume();
            case "step" -> step();
            case "reset" -> reset();
            case "probes" -> probe(message.probes());
            case "pace" -> pace(message.speed());
            case "sample" -> sample(message.interval());
            case "acquire" -> acquire(message.gapless());
            case "set" -> set(message);
            case "restart" -> restart(message);
            case "retune" -> retune(message);
            case "capture" -> capture(message);
            default -> throw ParallaxProblem.invalid("'" + message.type() + "' is not something a running circuit does.");
        }
    }

    // ─── running ────────────────────────────────────────────────────────────────────────────────────

    private synchronized void start(Message message) {
        pause();
        object = circuits.requireRunnable(message.objectId());
        if (message.definition() != null && !(message.definition() instanceof ObjectDefinition.Circuit)) {
            throw ParallaxProblem.invalid("Only a circuit runs live.");
        }
        circuit = message.definition() instanceof ObjectDefinition.Circuit given ? given
                : circuits.storedCircuit(object);
        values = message.values() == null ? Map.of() : Map.copyOf(message.values());
        parameters = null;
        interval = sampleInterval(message.interval(), 1e-5);
        probes = message.probes() == null ? List.of() : List.copyOf(message.probes());
        // The speed comes with the start: a pace sent after it would let the first frames play at real time.
        if (message.speed() != null) {
            speed = requireSpeed(message.speed());
        }
        if (message.gapless() != null) {
            gapless = message.gapless();
        }
        played = Math.min(speed, fastestGapless());
        build();
        resume();
    }

    /** A new session for the circuit as it is now, from its beginning. */
    private void build() {
        forgetCosts();
        LiveCircuits.Prepared prepared = circuits.prepare(object, circuit, parameters, values);
        session = new LiveCircuitSession(prepared.compiled(), interval, List.of());
        controls = prepared.controls();
        session.probe(probes);
        traceDensity();
        reply(started());
    }

    private synchronized void resume() {
        requireSession();
        if (ticking == null || ticking.isDone()) {
            due = 0;
            // At a fixed rate: the pause after a frame is what is left of its forty milliseconds, not forty more.
            ticking = circuits.clock().scheduleAtFixedRate(this::frame, 0, FRAME_MILLISECONDS, TimeUnit.MILLISECONDS);
            if (!counted) {
                counted = true;
                circuits.started();
            }
        }
    }

    private synchronized void pause() {
        if (ticking != null) {
            ticking.cancel(false);
            ticking = null;
        }
        if (counted) {
            counted = false;
            circuits.stopped();
        }
    }

    private synchronized void step() {
        requireSession();
        pause();
        sendFrame(session.advance(1), 0);
    }

    private synchronized void reset() {
        requireSession();
        boolean running = ticking != null;
        pause();
        build();
        if (running) {
            resume();
        }
    }

    /**
     * One frame, sent. A failure the engine names is the reader's to read; anything else is a defect, logged with its
     * stack — and still answered: an exception on the scheduler's thread would otherwise end the run in silence.
     */
    private void frame() {
        try {
            computeFrame();
        } catch (RuntimeException defect) {
            LOGGER.error("A live frame failed", defect);
            pause();
            reply(Map.of("type", "problem", "message", "The run stopped on an internal error: " + defect));
        }
    }


    private void computeFrame() {
        List<LiveCircuitSession.Frame> pieces = new ArrayList<>();
        long began = System.nanoTime();
        long readNanos = 0;
        long passNanos = 0;
        long read = 0;
        long passed = 0;
        try {
            due += played * FRAME_MILLISECONDS / 1000.0 / interval;
            int planned = (int) Math.min(Integer.MAX_VALUE, Math.floor(due));
            int left = planned;
            due -= left;
            // The screen keeps one contiguous block of samples a frame; the rest of the frame is gone through unread.
            int shown = Math.min(planned, SAMPLES_SHOWN_PER_FRAME);
            while (left > 0 && System.nanoTime() - began < FRAME_BUDGET_NANOSECONDS) {
                boolean reading = planned - left < shown;
                int piece = reading ? Math.min(SAMPLES_PER_PIECE, shown - (planned - left)) : Math.min(SAMPLES_PASSED_PER_PIECE, left);
                long pieceBegan = System.nanoTime();
                pieces.add(reading ? session.advance(piece) : session.pass(piece));
                if (reading) {
                    readNanos += System.nanoTime() - pieceBegan;
                    read += piece;
                } else {
                    passNanos += System.nanoTime() - pieceBegan;
                    passed += piece;
                }
                left -= piece;
            }
            followComputed(planned - left);
            if (left > 0) {
                // What a frame cut short did not reach is not owed to the next one.
                due = 0;
            }
            if (pieces.isEmpty()) {
                return;
            }
        } catch (IllegalArgumentException failed) {
            pause();
            reply(Map.of("type", "problem", "message", failed.getMessage()));
            return;
        }
        LiveCircuitSession.Frame joined = joined(pieces);
        // The share of the frame's period spent on it, encoding included: what the reader is shown as the server's load.
        long spent = System.nanoTime() - began;
        sendFrame(joined, spent / (FRAME_MILLISECONDS * 1e6));
        // Where the run stands, for a capture to go back to: a few arrays, every frame.
        session.checkpoint();
        govern(new FrameCost(read, readNanos, passed, passNanos, spent));
    }

    /** The pieces a frame was computed in, as one frame: their samples in order, the levels of the last. */
    private static LiveCircuitSession.Frame joined(List<LiveCircuitSession.Frame> pieces) {
        if (pieces.size() == 1) {
            return pieces.getFirst();
        }
        double[] time = pieces.stream().flatMapToDouble(piece -> Arrays.stream(piece.time())).toArray();
        return new LiveCircuitSession.Frame(time, joinedColumns(pieces, LiveCircuitSession.Frame::channels),
                pieces.getLast().units(), joinedLevels(pieces), joinedLoads(pieces), joinedTrace(pieces),
                pieces.stream().mapToLong(LiveCircuitSession.Frame::covered).sum(), joinedPeaks(pieces));
    }

    /** Each channel's peaks over the pieces that read samples, in order. */
    private static Map<String, LiveCircuitSession.Peaks> joinedPeaks(List<LiveCircuitSession.Frame> pieces) {
        Map<String, LiveCircuitSession.Peaks> joined = new LinkedHashMap<>();
        for (String channel : pieces.getFirst().peaks().keySet()) {
            List<LiveCircuitSession.Peaks> read = pieces.stream().map(piece -> piece.peaks().get(channel)).filter(java.util.Objects::nonNull).toList();
            joined.put(channel, new LiveCircuitSession.Peaks(read.stream().flatMapToDouble(peaks -> Arrays.stream(peaks.lows())).toArray(),
                    read.stream().flatMapToDouble(peaks -> Arrays.stream(peaks.highs())).toArray(),
                    read.stream().flatMapToDouble(peaks -> Arrays.stream(peaks.means())).toArray()));
        }
        return joined;
    }

    /** Every node's mean voltage over the whole frame: each piece's mean, weighted by how many samples it went through. */
    private static Map<String, Double> joinedLevels(List<LiveCircuitSession.Frame> pieces) {
        Map<String, Double> sums = new LinkedHashMap<>();
        Map<String, Double> weights = new LinkedHashMap<>();
        for (LiveCircuitSession.Frame piece : pieces) {
            double count = piece.covered();
            piece.levels().forEach((node, level) -> {
                sums.merge(node, level * count, Double::sum);
                weights.merge(node, count, Double::sum);
            });
        }
        sums.replaceAll((node, sum) -> sum / Math.max(1, weights.get(node)));
        return sums;
    }

    private static LiveCircuitSession.Trace joinedTrace(List<LiveCircuitSession.Frame> pieces) {
        List<LiveCircuitSession.Frame> traced = pieces.stream().filter(piece -> piece.trace().time().length > 0).toList();
        if (traced.isEmpty()) {
            return LiveCircuitSession.Trace.EMPTY;
        }
        double[] time = traced.stream().flatMapToDouble(piece -> Arrays.stream(piece.trace().time())).toArray();
        return new LiveCircuitSession.Trace(time, joinedTraceColumns(traced, trace -> trace.columns()), traced.getFirst().trace().units(),
                joinedTraceColumns(traced, trace -> trace.rms()));
    }

    /** One of the traces' column sets over the pieces, in order; NaN where a piece has no such column. */
    private static Map<String, double[]> joinedTraceColumns(List<LiveCircuitSession.Frame> traced,
                                                            Function<LiveCircuitSession.Trace, Map<String, double[]>> columnsOf) {
        Map<String, double[]> columns = new LinkedHashMap<>();
        for (String name : columnsOf.apply(traced.getFirst().trace()).keySet()) {
            columns.put(name, traced.stream().flatMapToDouble(piece -> {
                double[] values = columnsOf.apply(piece.trace()).get(name);
                return values == null ? Arrays.stream(new double[piece.trace().time().length]).map(ignored -> Double.NaN) : Arrays.stream(values);
            }).toArray());
        }
        return columns;
    }

    /**
     * The parts' loads over the whole frame. Power and every signal figure average over the pieces, each weighted by how
     * many samples it covered — a frame's last piece may be a few hundred samples, a quarter of a 1 kHz period, and a
     * figure read from it alone flickers between the extremes. A stress figure is the worst piece's, since a limit is
     * broken at the moment it is passed. A moment figure — a character, a count — and a picture are as the last piece left
     * them: averaged, a space and an 'l' made a 'T'. The terminal currents, how warm a part is, its limits and whether
     * it failed are as the last piece left them too.
     */
    private static Map<String, PartLoad> joinedLoads(List<LiveCircuitSession.Frame> pieces) {
        Map<String, Double> powers = new LinkedHashMap<>();
        Map<String, Map<String, Double>> figures = new LinkedHashMap<>();
        Map<String, Map<String, Double>> figureWeights = new LinkedHashMap<>();
        Map<String, PartLoad> last = new LinkedHashMap<>();
        double samples = 0;
        for (LiveCircuitSession.Frame piece : pieces) {
            double weight = piece.covered();
            samples += weight;
            piece.parts().forEach((part, load) -> {
                powers.merge(part, load.power() * weight, Double::sum);
                Map<String, Double> partFigures = figures.computeIfAbsent(part, ignored -> new LinkedHashMap<>());
                Map<String, Double> partWeights = figureWeights.computeIfAbsent(part, ignored -> new LinkedHashMap<>());
                load.displays().forEach((name, value) -> {
                    if (load.moments().contains(name)) {
                        partFigures.put(name, value);
                    } else if (PartLoad.STRESSES.contains(name)) {
                        partFigures.merge(name, value, Math::max);
                    } else {
                        partFigures.merge(name, value * weight, Double::sum);
                        partWeights.merge(name, weight, Double::sum);
                    }
                });
                last.put(part, load);
            });
        }
        Map<String, PartLoad> joined = new LinkedHashMap<>();
        double total = samples;
        powers.forEach((part, power) -> {
            PartLoad latest = last.get(part);
            Map<String, Double> displays = new LinkedHashMap<>(figures.get(part));
            figureWeights.get(part).forEach((name, weight) -> displays.computeIfPresent(name, (ignored, sum) -> sum / weight));
            joined.put(part, new PartLoad(power / total, latest.terminals(), latest.temperature(), latest.maximum(),
                    latest.rating(), latest.failure(), displays, latest.moments(), latest.pictures(), latest.cause()));
        });
        return joined;
    }

    private static Map<String, double[]> joinedColumns(List<LiveCircuitSession.Frame> pieces,
                                                       Function<LiveCircuitSession.Frame, Map<String, double[]>> columns) {
        Map<String, double[]> joined = new LinkedHashMap<>();
        for (String name : columns.apply(pieces.getFirst()).keySet()) {
            joined.put(name, pieces.stream().flatMapToDouble(piece -> {
                double[] values = columns.apply(piece).get(name);
                return values == null ? Arrays.stream(new double[piece.time().length]).map(ignored -> Double.NaN) : Arrays.stream(values);
            }).toArray());
        }
        return joined;
    }

    /** What the oscilloscope's time base asks for: samples this far apart, from where the circuit is now. */
    private synchronized void sample(Double requested) {
        requireSession();
        double next = sampleInterval(requested, interval);
        if (next == interval) {
            return;
        }
        interval = next;
        due = 0;
        forgetCosts();
        played = Math.min(speed, fastestGapless());
        session.resample(circuits.prepare(object, circuit, parameters, values).compiled(), interval);
        traceDensity();
    }

    /** Every sample read or real time first, from now on; the circuit goes on. */
    private synchronized void acquire(Boolean requested) {
        gapless = Boolean.TRUE.equals(requested);
        due = 0;
        played = Math.min(speed, fastestGapless());
        if (session != null) {
            traceDensity();
        }
    }

    /**
     * The fastest a gapless run plays: as much circuit time a frame as {@link #SAMPLES_SHOWN_PER_FRAME} samples cover —
     * at 10 ns a sample a thousand times slower than real time. Unbounded while the run keeps real time first.
     */
    private double fastestGapless() {
        return gapless ? Math.max(SLOWEST, SAMPLES_SHOWN_PER_FRAME * interval * 1000.0 / FRAME_MILLISECONDS) : FASTEST;
    }

    private synchronized void pace(Double requested) {
        speed = requireSpeed(requested);
        played = Math.min(speed, fastestGapless());
        if (session != null) {
            traceDensity();
        }
    }

    private static double requireSpeed(Double requested) {
        if (requested == null || !(requested >= SLOWEST && requested <= FASTEST)) {
            throw ParallaxProblem.invalid("Play between " + SLOWEST + " and " + FASTEST + " times real time.");
        }
        return requested;
    }

    /** What one frame cost: its samples read and the nanoseconds they took, the same for those passed, and the whole. */
    private record FrameCost(long read, long readNanos, long passed, long passNanos, long spentNanos) {
    }

    /**
     * Holds the speed it plays at where the server keeps it. A frame has three costs, each measured and smoothed over
     * frames: a sample read for the screen, a sample passed through unread, and what the frame costs besides — joining
     * and encoding. The fastest speed whose frame fits {@link #SUSTAINED_SHARE} of the frame's period is worked out
     * from them, and it plays at that or at the speed asked for, whichever is slower.
     *
     * <p>Two simpler governors failed in turn. One scaled the speed by the share a frame cut short had reached: a single
     * hiccup — a changed time base, the compiler warming up — dragged it down a hundredfold. The next took circuit
     * seconds per second spent as one figure: with the screen's samples a fixed cost of every frame, a lower speed
     * measured a lower rate, which lowered the speed again, down to a sixth of what the server could do.</p>
     */
    private void govern(FrameCost cost) {
        if (cost.read() > 0) {
            readCost = smoothed(readCost, (double) cost.readNanos() / cost.read());
        }
        if (cost.passed() > 0) {
            passCost = smoothed(passCost, (double) cost.passNanos() / cost.passed());
        }
        frameCost = smoothed(frameCost, Math.max(0, cost.spentNanos() - cost.readNanos() - cost.passNanos()));
        if (Double.isNaN(readCost)) {
            return;
        }
        double available = FRAME_MILLISECONDS * 1e6 * SUSTAINED_SHARE - frameCost;
        double shownCost = SAMPLES_SHOWN_PER_FRAME * readCost;
        // Passing is what the frames beyond the screen's block cost; until one was passed, reading is its bound.
        double samples = available <= shownCost
                ? Math.max(1, available / readCost)
                : SAMPLES_SHOWN_PER_FRAME + (available - shownCost) / (Double.isNaN(passCost) ? readCost : passCost);
        double next = Math.min(Math.min(speed, fastestGapless()), Math.max(SLOWEST, samples * interval * 1000.0 / FRAME_MILLISECONDS));
        if (Math.abs(next - played) > played * 0.05) {
            played = next;
            traceDensity();
        }
    }

    /** A sample costs what its interval makes it cost: what the server kept up with before says nothing now. */
    private void forgetCosts() {
        readCost = Double.NaN;
        passCost = Double.NaN;
        frameCost = Double.NaN;
    }

    /**
     * A cost followed over frames, robustly: a frame the JVM stopped in — the compiler, the collector — measures a sample
     * at a hundred times its cost, and one such frame took the speed down to a two-hundredth. A measure more than three
     * times the estimate is taken as three times; a cost that falls is followed at once, one that rises over frames.
     */
    private static double smoothed(double previous, double measured) {
        if (Double.isNaN(previous)) {
            return measured;
        }
        double bounded = Math.min(measured, previous * 3);
        double weight = bounded < previous ? 0.5 : 0.2;
        return previous + (bounded - previous) * weight;
    }


    /** Traces as many samples as a frame at this speed and interval carries moments: all of them, slowed enough. */
    private void traceDensity() {
        traceEvery(played * FRAME_MILLISECONDS / 1000.0 / interval);
    }

    /**
     * Traces as many moments as the frame just computed carries, when that is far from what the speed asked for: a
     * circuit the server cannot keep up with computes fewer samples than its speed asks, and a trace spaced for the
     * speed asked would leave a frame one moment or none — a sheet whose lights jump.
     */
    private void followComputed(int computed) {
        int wanted = (int) Math.max(1, Math.ceil((double) computed / TRACED_PER_FRAME));
        if (wanted > tracedEvery * 3 / 2 || wanted * 3 / 2 < tracedEvery) {
            traceEvery(computed);
        }
    }

    private void traceEvery(double samplesPerFrame) {
        tracedEvery = (int) Math.max(1, Math.ceil(samplesPerFrame / TRACED_PER_FRAME));
        session.trace(tracedEvery);
    }

    // ─── changes while it runs ──────────────────────────────────────────────────────────────────────

    private synchronized void probe(List<LiveCircuitSession.Probe> requested) {
        requireSession();
        probes = requested == null ? List.of() : List.copyOf(requested);
        session.probe(probes);
    }

    /**
     * A live part worked, or a parameter of the circuit moved: the circuit is compiled with the new value and goes on from
     * now. Only a value a live part offers, or one of the circuit's own parameters, can be set this way.
     */
    private synchronized void set(Message message) {
        requireSession();
        if (message.parameter() == null || message.value() == null || !Double.isFinite(message.value())) {
            throw ParallaxProblem.invalid("Name the value to set and give it a number.");
        }
        if (message.part() != null) {
            LiveControls.LiveControl control = controls.stream()
                    .filter(candidate -> candidate.part().equals(message.part()) && candidate.parameter().equals(message.parameter()))
                    .findFirst().orElseThrow(() -> ParallaxProblem.invalid("'" + message.part() + "' has no live '"
                            + message.parameter() + "'."));
            double value = Math.max(control.minimum(), Math.min(control.maximum(), message.value()));
            // A live part inside a placed sheet (S1_SW1) is a part of the circuit only once its sheets are unfolded: the
            // run goes on from the unfolded circuit then, which keeps every part's setting.
            boolean written = circuit.elements().stream().anyMatch(element -> element.id().equals(control.part()));
            ObjectDefinition.Circuit target = written ? circuit
                    : (ObjectDefinition.Circuit) circuits.prepare(object, circuit, parameters, values).compiled().definition();
            circuit = withOverride(target, control.part(), control.parameter(), value);
        } else {
            Map<String, Double> changed = new HashMap<>(values);
            changed.put(message.parameter(), message.value());
            values = Map.copyOf(changed);
        }
        LiveCircuits.Prepared prepared = circuits.prepare(object, circuit, parameters, values);
        boolean continued = session.retune(prepared.compiled());
        controls = prepared.controls();
        reply(Map.of("type", "changed", "continued", continued, "controls", controls, "time", session.time()));
    }

    /**
     * The circuit with other values — a part's value, its model's values, as a bench changes them while it runs —, going
     * on from now as a moved slider does ({@link LiveCircuitSession#retune}). A restart threw away the run, the screen
     * and the parts' heat for a resistor changed from 47 to 56 Ω; a circuit whose parts changed still starts again.
     */
    private synchronized void retune(Message message) {
        requireSession();
        if (!(message.definition() instanceof ObjectDefinition.Circuit edited)) {
            throw ParallaxProblem.invalid("Send the circuit with its new values to go on with them.");
        }
        circuit = edited;
        LiveCircuits.Prepared prepared = circuits.prepare(object, circuit, parameters, values);
        boolean continued = session.retune(prepared.compiled());
        controls = prepared.controls();
        reply(Map.of("type", "changed", "continued", continued, "controls", controls, "time", session.time()));
    }

    /** The edited circuit — its text, or the definition an editor holds —, run at once: what is written is what runs. */
    private synchronized void restart(Message message) {
        requireSession();
        if (message.source() != null && !message.source().isBlank()) {
            DefinedObject edited = circuits.circuitOf(message.source());
            circuit = (ObjectDefinition.Circuit) edited.definition();
            parameters = edited.parameters();
            // The text is what runs: its own values, not the sliders the page had for the text before.
            values = Map.of();
        } else if (message.definition() instanceof ObjectDefinition.Circuit edited) {
            circuit = edited;
        } else {
            throw ParallaxProblem.invalid("Send the edited circuit — its text or its definition — to run it.");
        }
        // An edited circuit is run with the probes that belong to it: the old ones may name a node it no longer has.
        if (message.probes() != null) {
            probes = List.copyOf(message.probes());
        }
        // A new window is a new sample interval: the samples span the window the reader set, so it starts again with it.
        interval = sampleInterval(message.interval(), interval);
        boolean running = ticking != null;
        pause();
        build();
        if (running) {
            resume();
        }
    }

    /** The time between two samples a reader asked for, or the one already in use. */
    private static double sampleInterval(Double requested, double current) {
        double interval = requested == null ? current : requested;
        if (!Double.isFinite(interval) || interval <= 0 || interval > 1) {
            throw ParallaxProblem.invalid("Choose a sample interval between 0 and 1 s.");
        }
        return interval;
    }

    void close() {
        pause();
    }

    // ─── replies ────────────────────────────────────────────────────────────────────────────────────

    private Map<String, Object> started() {
        Map<String, Object> started = new LinkedHashMap<>();
        started.put("type", "started");
        started.put("interval", interval);
        started.put("nodes", session.nodes());
        started.put("controls", controls);
        started.put("probes", probes);
        return started;
    }

    private void sendFrame(LiveCircuitSession.Frame frame, double load) {
        output.binary(FrameEncoding.encode("frame", frame, interval, load, played, mapper));
    }

    /**
     * A stretch the run has passed, replayed finely from the checkpoint before it ({@link LiveCircuitSession#capture})
     * and sent as one binary message; the run goes on meanwhile. Refused — as a problem the reader reads — when the
     * stretch is older than the run keeps or not yet run; the run is not paused for that. Replayed on its own thread
     * ({@link LiveCircuits#replays}), one at a time, while the run goes on.
     */
    private void capture(Message message) {
        if (message.from() == null || message.to() == null || message.samples() == null) {
            throw ParallaxProblem.invalid("A capture names its stretch, from and to, and its samples.");
        }
        LiveCircuitSession running;
        double speed;
        synchronized (this) {
            requireSession();
            running = session;
            speed = played;
        }
        if (!capturing.compareAndSet(false, true)) {
            reply(Map.of("type", "capture-refused", "message", "Still capturing the edge before; this one is let go."));
            return;
        }
        circuits.replays().execute(() -> {
            try {
                LiveCircuitSession.Frame captured = running.capture(message.from(), message.to(), message.samples());
                double step = (message.to() - message.from()) / (message.samples() - 1);
                output.binary(FrameEncoding.encode("capture", captured, step, 0, speed, mapper));
            } catch (IllegalArgumentException refused) {
                reply(Map.of("type", "capture-refused", "message", refused.getMessage()));
            } catch (RuntimeException defect) {
                LOGGER.error("A capture failed", defect);
                reply(Map.of("type", "capture-refused", "message", "The capture failed on an internal error: " + defect));
            } finally {
                capturing.set(false);
            }
        });
    }

    private void reply(Map<String, Object> message) {
        output.text(mapper.writeValueAsString(message));
    }

    private void requireSession() {
        if (session == null) {
            throw ParallaxProblem.invalid("Start the circuit first.");
        }
    }

    private static ObjectDefinition.Circuit withOverride(ObjectDefinition.Circuit circuit, String part, String parameter,
                                                         double value) {
        List<ObjectDefinition.CircuitElement> elements = circuit.elements().stream().map(element -> {
            if (!element.id().equals(part)) {
                return element;
            }
            Map<String, String> overrides = new LinkedHashMap<>(element.overrides());
            overrides.put(parameter, Double.toString(value));
            return new ObjectDefinition.CircuitElement(element.id(), element.type(), element.positive(), element.negative(),
                    element.value(), element.component(), element.pins(),
                    overrides, element.symbol());
        }).toList();
        return circuit.withElements(elements);
    }
}
