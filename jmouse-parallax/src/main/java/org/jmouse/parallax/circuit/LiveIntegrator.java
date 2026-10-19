package org.jmouse.parallax.circuit;

import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;

import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One piece of a circuit that keeps running: each call advances the same device histories and digital event queue,
 * sample by sample, for as long as someone watches. The integration is the {@link TransientStepper}'s; this piece reads
 * it — every part's energy and heat ({@link PartLedger}), every node's peaks and means ({@link NodeEnvelope}), the trace
 * ({@link TraceRecorder}) and the meters ({@link MeterRecorder}) —, each over the one rule of what a part is
 * ({@link PartIndex}).
 *
 * <p>A value that changes while it runs — a switch closed, a wiper moved — does not start it again: {@link #continuing}
 * builds the piece with its new values and carries the state across, restarting the integration with backward Euler
 * as at any other discontinuity. A digital part keeps its discrete life too — its flip-flops, pending transactions,
 * timers and watched crossings. Only a circuit whose topology changed starts again.</p>
 */
final class LiveIntegrator {

    private static final int    STEPS_PER_SAMPLE = 2000;
    /**
     * The longest step accuracy allows once a trace keeps one sample in several, as SPICE's TMAX is its print step: the
     * interval the engine was matched against ngspice at. The error control alone lets a slow exponential take long
     * steps whose error gathers where a comparator turns — a 555 ran 0.4 % fast with steps up to 2.5 ms. Tied to the
     * oscilloscope's interval instead, a 200 ns screen stepped a 2 Hz sine five million times a second.
     */
    private static final double ACCURATE_STEP    = 1e-5;

    /**
     * What one part did since the last reading: its average power, its current now, how warm it is, and its states now.
     *
     * @param temperature its own temperature, °C, as its {@code heat} law makes it ({@link PartHeat}) — the hottest of its
     *                    devices; NaN for a part that does not warm
     * @param states      its devices' states now, by name ({@link Device#stateValues}): what a figure its component
     *                    displays over a state reads; empty for a part without states
     */
    record PartReading(String part, double power, Map<String, Double> terminals, double temperature,
                       Map<String, Double> states) {
    }

    private final TransientStepper stepper;
    private final PartIndex        parts;
    private final PartLedger       ledger;
    private final NodeEnvelope     envelope;
    private final MeterRecorder    meters;
    private final double           interval;
    private final int              nodeCount;
    /**
     * Whether what a screen and the bench's readings need is kept step by step — each part's energy, each node's peaks
     * and mean. On by default; a caller that reads only the solution ({@link #solutionOnly}) does not pay for them.
     */
    private boolean                readings = true;
    /** The parts whose terminal currents a traced sample carries; null for every part. */
    private Set<String>            tracedParts;
    private long                   sampleIndex;
    /** Every how many samples the current into each part's terminals is read as well; 0 reads none. */
    private int                    traceEvery;
    /** The stretch since the last traced sample, while the trace keeps one sample in several; null while it keeps each. */
    private TraceRecorder          trace;
    /** What the ledger takes from each accepted step. */
    private final TransientStepper.AcceptedStep gathering;

    /**
     * A piece of a circuit, on the bench of the whole: whether the bench is realistic, and how warm, decides whether its
     * parts heat themselves and are noisy.
     *
     * @param sampleInterval the time between two samples, in seconds
     */
    LiveIntegrator(ElaboratedCircuit piece, double sampleInterval) {
        if (!Double.isFinite(sampleInterval) || sampleInterval <= 0) {
            throw new IllegalArgumentException("Choose a positive sample interval.");
        }
        BenchConditions bench = piece.bench();
        boolean realistic = bench != null && bench.realistic();
        Netlist netlist = Netlist.of(piece);
        EventSimulation events = new EventSimulation(netlist);
        List<Device> devices = Device.of(netlist, events);
        // A realistic bench is noisy: every device's noise, injected as currents (none on an ideal bench, whose answers
        // stay the textbook's).
        if (realistic) {
            devices.add(new NoiseInjection(devices, sampleInterval, 1));
        }
        this.stepper = new TransientStepper(netlist, devices, events, sampleInterval);
        this.parts = new PartIndex(stepper.devices());
        this.ledger = new PartLedger(parts, stepper.devices().size(), realistic);
        this.gathering = ledger::accepted;
        this.nodeCount = netlist.nodes().size();
        this.envelope = new NodeEnvelope(nodeCount);
        this.meters = new MeterRecorder(parts);
        this.interval = sampleInterval;
    }

    /**
     * The piece with new values, going on from where {@code previous} is: the same time, node voltages and device
     * histories, the integration restarting as at a switch edge, each part as warm as it was. Sampled at a new interval
     * from now on, the first sample is the first instant of the new grid at or after where the integration stands, so the
     * samples go on without a step back in time.
     *
     * @throws IllegalArgumentException when the circuit cannot be continued — its topology changed
     */
    static LiveIntegrator continuing(ElaboratedCircuit piece, LiveIntegrator previous, double sampleInterval) {
        LiveIntegrator next = new LiveIntegrator(piece, sampleInterval);
        synchronized (previous) {
            next.stepper.continueFrom(previous.stepper);
            next.ledger.continueFrom(previous.ledger);
            next.sampleIndex = sampleInterval == previous.interval ? previous.sampleIndex
                    : (long) Math.ceil(previous.stepper.time() / sampleInterval - 1e-9);
        }
        return next;
    }

    /** The node names, in the order a sample reports them. */
    List<String> nodes() {
        return stepper.netlist().nodes();
    }

    double interval() {
        return interval;
    }

    synchronized double time() {
        return stepper.time();
    }

    /**
     * Reads the current into every part's terminals on every {@code every}-th sample, counted from the circuit's start
     * so that every part of a cut circuit reads at the same samples; 0 reads none.
     */
    synchronized void trace(int every) {
        if (every < 0) {
            throw new IllegalArgumentException("Trace every sample, every few, or none.");
        }
        traceEvery = every;
        // Every sample traced, steps keep to the sample, as they always did; one in several, to what accuracy needs.
        stepper.allow(every > 1 ? Math.max(interval, Math.min(interval * every, ACCURATE_STEP)) : interval);
        trace = every > 1 ? new TraceRecorder(parts, nodeCount) : null;
    }

    /** Reads these parts on every sample from now on — the current into each terminal and the power —; none reads none. */
    synchronized void meter(Set<String> metered) {
        meters.meter(metered);
    }

    /** Keeps the peaks of these differences — pairs of this piece's nodes, −1 for ground — from the next step on. */
    synchronized void difference(List<NodeDifference> pairs) {
        envelope.watch(pairs);
    }

    /** The level every digital node of this piece stands at now; none when the piece has no digital part. */
    synchronized Map<String, LogicLevel> levels() {
        return stepper.levels();
    }

    /** Traces only these parts' terminal currents from now on; null traces every part. */
    synchronized void traceOnly(Set<String> only) {
        tracedParts = only == null ? null : Set.copyOf(only);
    }

    /** Stops keeping the readings — energy, peaks, means —: the samples carry the solution alone from now on. */
    synchronized void solutionOnly() {
        readings = false;
        ledger.keepNoEnergy();
    }

    /** Stops keeping the parts' energy, and keeps each node's peaks and mean: the samples carry those with the solution. */
    synchronized void keepNoEnergy() {
        ledger.keepNoEnergy();
    }

    /** The next {@code count} samples, the first at the time the last call ended. */
    synchronized List<LiveSample> advance(int count) {
        if (count < 1 || count > 256) {
            throw new IllegalArgumentException("Advance 1…256 samples at a time.");
        }
        List<LiveSample> output = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double at = sampleIndex * interval;
            reach(at, 1);
            output.add(sampleAt(at, traceEvery > 0 && sampleIndex % traceEvery == 0, true));
            sampleIndex++;
        }
        return output;
    }

    /**
     * Goes through the next {@code count} samples without reading them, but for the traced ones — what the sheet plays
     * from —, returned in order. What a live run does with the part of a frame no screen could show: at real time on a
     * 200 ns sample interval a frame is two hundred thousand samples, of which the oscilloscope keeps one screenful.
     * The circuit, its parts' energy and its trace go through all of them.
     */
    synchronized List<LiveSample> pass(long count) {
        if (count < 1) {
            throw new IllegalArgumentException("Pass at least one sample.");
        }
        List<LiveSample> traced = new ArrayList<>();
        long end = sampleIndex + count;
        while (traceEvery > 0) {
            long next = (sampleIndex + traceEvery - 1) / traceEvery * traceEvery;
            if (next >= end) {
                break;
            }
            double at = next * interval;
            reach(at, next - sampleIndex + 1);
            sampleIndex = next;
            traced.add(sampleAt(at, true, false));
            sampleIndex++;
        }
        if (sampleIndex < end) {
            reach((end - 1) * interval, end - sampleIndex);
            sampleIndex = end;
        }
        // What the unread samples went through is no read sample's peak.
        envelope.forget();
        return traced;
    }

    /** Steps until the integration has passed {@code at}, allowed so many steps for each of the samples it goes through. */
    private void reach(double at, long samples) {
        long steps = 0;
        long allowed = STEPS_PER_SAMPLE * Math.max(1, samples);
        while (stepper.time() < at - stepper.minimumStep()) {
            read(stepper.step(gathering));
            if (++steps > allowed) {
                throw new IllegalArgumentException("The circuit needs too many steps near t = " + stepper.time() + " s.");
            }
        }
    }

    /** What every reader takes of a step just accepted, {@code duration} long. */
    private void read(double duration) {
        double now = stepper.time();
        if (trace != null) {
            trace.take(stepper.state(), stepper.companion(), now, duration);
        }
        if (readings) {
            envelope.take(stepper.earlier(), stepper.state(), duration);
            if (!meters.idle()) {
                meters.take(stepper.state(), stepper.companion(), now);
            }
        }
    }

    /** The sample at {@code at}, which the integration has just passed: traced, it carries the stretch's window or its currents. */
    private LiveSample sampleAt(double at, boolean traced, boolean read) {
        double[] state = stepper.state();
        double[] companion = stepper.companion();
        double now = stepper.time();
        // The currents of the step that reached the sample: at most one step, and so one sample, after it.
        Map<String, Map<String, Double>> terminals = null;
        Map<String, Double> powers = traced ? parts.powers(state, now, tracedParts) : null;
        SampleWindow window = null;
        if (traced && trace != null && trace.duration() >= traceEvery * interval / 2) {
            TraceWindow.Closed closed = trace.close();
            terminals = closed.terminals();
            window = closed.window();
        } else if (traced) {
            // Too little of the stretch went by in whole steps to stand for it: the instant does.
            if (trace != null) {
                trace.clear();
            }
            terminals = parts.terminalCurrents(state, companion, now, tracedParts);
        }
        Map<String, MeteredPart> measured = read && readings && !meters.idle() ? meters.at(at, state, companion, now) : null;
        double[] voltages = Arrays.copyOf(stepper.valuesAt(at), nodeCount);
        SampleEnvelope peaks = read && readings
                ? envelope.read(voltages, at, state, now) : null;
        return new LiveSample(at, voltages, terminals, measured, window, peaks, powers);
    }

    /** The heat of every part that warms, by part name: the first unit's for a part of several. */
    synchronized Map<String, PartHeat> heats() {
        return parts.heats();
    }

    /**
     * What every part did since the last reading — its average power, the current into each of its terminals now, how
     * warm it is, its states — and starts the next one.
     */
    synchronized List<PartReading> readParts() {
        double[] state = stepper.state();
        ledger.advanceHeat();
        double[] powers = ledger.takePowers();
        double[] temperatures = parts.temperatures();
        List<PartReading> readings = new ArrayList<>(parts.count());
        for (int part = 0; part < parts.count(); part++) {
            readings.add(new PartReading(parts.parts()[part], powers[part],
                    parts.terminalCurrents(part, state, stepper.companion(), stepper.time()), temperatures[part],
                    parts.states(part, state)));
        }
        return readings;
    }
}
