package org.jmouse.parallax.circuit;

import org.jmouse.parallax.numerics.SparseSystem;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A transient that keeps running: each call advances the same device histories and digital event queue, sample by
 * sample, for as long as someone watches. The batch {@link CircuitSolver} answers a whole window at once; this one is
 * what an emulator steps while a circuit runs.
 *
 * <p>A value that changes while it runs — a switch closed, a wiper moved — does not start it again: {@link #continuing}
 * builds the circuit with its new values and carries the state across, restarting the integration with backward Euler
 * as at any other discontinuity. A digital part keeps its discrete life too — its flip-flops, pending transactions,
 * timers and watched crossings. Only a circuit whose topology changed starts again.</p>
 */
final class LiveIntegrator {


    /**
     * Newton's budget on a time step, as SPICE's itl4: a step that has not settled in ten iterations is cut, not
     * pushed on — a shorter step starts closer to its answer, and three hundred iterations spent on a step that will be
     * cut anyway were three quarters of all the work.
     */
    private static final int    NEWTON_ITERATIONS   = 10;
    /** What a step is cut to when Newton did not settle on it — SPICE's factor. */
    private static final double UNSETTLED_CUT       = 8;
    /** A cut step plain Newton did not settle in its ten either is given this many before the line search. */
    private static final int    LONGER_ITERATIONS   = 60;
    /** A line search's iterations, the fallback when plain Newton did not settle; its halvings count among them. */
    private static final int    SEARCH_ITERATIONS   = 300;
    /** How short the line search lets a Newton step become before it takes it anyway. */
    private static final double SMALLEST_FRACTION   = 1.0 / 1024;
    /** Newton has settled when no unknown moves by more than this share of itself plus {@link #ABSOLUTE_TOLERANCE}. */
    private static final double SETTLED_RELATIVE    = 1e-4;
    private static final double ABSOLUTE_TOLERANCE  = 1e-6;
    private static final int    STEPS_PER_SAMPLE    = 2000;
    /** Corrector minus a three-point predictor is about thirteen trapezoidal errors; ten keeps a margin. */
    private static final double MILNE               = 10;
    /** How many sample intervals one step may span at most while every sample is traced; the error decides within that. */
    private static final double FREE_STEP           = 1;
    /**
     * The longest step accuracy allows once a trace keeps one sample in several, as SPICE's TMAX is its print step: the
     * interval the engine was matched against ngspice at. The error control alone lets a slow exponential take long
     * steps whose error gathers where a comparator turns — a 555 ran 0.4 % fast with steps up to 2.5 ms. Tied to the
     * oscilloscope's interval instead, a 200 ns screen stepped a 2 Hz sine five million times a second.
     */
    private static final double ACCURATE_STEP       = 1e-5;
    /** How much a step may grow at once when it used little of its allowed error. */
    private static final double GROWTH              = 2;

    /** What one part did since the last reading: its average power, its current now, and how it warms. */
    /**
     * @param heating how far the part's own model has heated it above the bench, kelvin — a model that keeps its own heat
     *                ({@code heating}); NaN for a part that does not, whose temperature is estimated from its power
     */
    record PartReading(String part, double power, Map<String, Double> terminals, double thermalResistance, double heating) {
    }

    private final Netlist             netlist;
    /** The one linear system every Newton iteration assembles into. */
    private final SparseSystem        system;
    private final EventSimulation     events;
    private final List<Device>        devices;
    /** The devices nothing in a run changes, stamped once for it ({@link Device#invariant}). */
    private final List<Device>        invariantDevices;
    private boolean                   invariantStamped;
    /**
     * Whether what a screen and the bench's readings need is kept step by step — each part's energy, each node's peaks
     * and mean. On by default; a caller that reads only the solution ({@link #solutionOnly}) does not pay for them.
     */
    private boolean                   readings = true;
    /** The parts whose terminal currents a traced sample carries; null for every part. */
    private java.util.Set<String>     tracedParts;
    /** The devices whose stamp does not read the Newton guess, stamped once per attempt; and those that do. */
    private final List<Device>        linearDevices;
    private final List<Device>        nonlinearDevices;
    private final boolean             nonlinear;
    private final boolean             digital;
    private final double              interval;
    /** The longest step the devices allow — a generator's edges —; infinite when none limits it. */
    private final double              deviceMaximumStep;
    /** The longest step now: the devices' limit, and the stretch between two traced moments. */
    private double                    maximumStep;
    private final double              minimumStep;
    private final double              crossingResolution;
    /** The generators' signals, whose edges a step lands on when it would cross one. */
    private final List<SourceSignal>  signals;
    /** The parts the devices belong to, each device's part (or -1), how each part warms, and what it took since read. */
    private final String[]            parts;
    private final int[]               partOfDevice;
    private final double[]            thermalResistances;
    private final double[]            energy;
    private double                    energyTime;
    private double[] state;
    private double[] earlier;
    private double   earlierStep;
    /** The state before {@code earlier}, so a step can be predicted by a parabola through three points. */
    private double[] oldest;
    private double   oldestStep;
    private double[] companion;
    private double   time;
    private double   step;
    private boolean  restart = true;
    private long     sampleIndex;
    /** Every how many samples the current into each part's terminals is read as well; 0 reads none. */
    private int      traceEvery;
    /** The stretch since the last traced sample, while the trace keeps one sample in several; null while it keeps each. */
    private TraceWindow traceWindow;
    /** Each node's lowest and highest voltage since the last read sample, over every accepted step: its peak detection. */
    private double[] nodeLows;
    private double[] nodeHighs;
    /** Pairs of nodes, positive and negative (−1 for ground), whose difference's peaks are kept the same way: a probe across a part. */
    private List<LiveCircuitSolver.NodeDifference> differences = List.of();
    private double[] differenceLows = new double[0];
    private double[] differenceHighs = new double[0];
    /**
     * Each node's voltage integrated since the run began, step by step, and its value at the last read sample: the
     * mean over a sample's interval is their difference over the interval — exact wherever a step crosses the sample.
     */
    private double[] nodeIntegrals;
    private double[] markedIntegrals;
    /**
     * Whether the last step landed on an edge: it was solved with the level after the jump, so what lies inside it is the
     * level before — held, not a ramp drawn between its ends.
     */
    private boolean  landedOnEdge;
    private double   markedTime = Double.NaN;
    /** Each device's two terminal columns in the trace window, or null; a model's columns by pin; the step's currents. */
    private int[][] terminalColumns;
    private Map<String, Integer>[] modelColumns;
    private double[] stepCurrents;
    /** The parts a meter stands on, read on every sample: their terminal currents and their power. */
    private java.util.Set<String> metered = java.util.Set.of();

    /**
     * @param sampleInterval the time between two samples, in seconds
     */
    LiveIntegrator(CompiledObject object, double sampleInterval) {
        this(object, sampleInterval, benchOf(object));
    }

    /**
     * A piece of a circuit, on the bench of the whole: the piece itself carries no conditions — they are applied to its
     * elements already, and applying them again would move a resistor's value twice —, yet whether the bench is
     * realistic, and how warm, decides whether its parts heat themselves and are noisy.
     *
     * @param bench the whole circuit's bench conditions; null for an ideal bench at room temperature
     */
    LiveIntegrator(CompiledObject object, double sampleInterval, org.jmouse.parallax.object.ObjectDefinition.BenchConditions bench) {
        if (!Double.isFinite(sampleInterval) || sampleInterval <= 0) {
            throw new IllegalArgumentException("Choose a positive sample interval.");
        }
        this.netlist = Netlist.of(object);
        this.system = new SparseSystem(netlist.size());
        this.events = new EventSimulation(netlist);
        List<Device> built = new ArrayList<>();
        // On a realistic bench a resistor heats itself and moves with its own heat; on an ideal one it is stamped once.
        boolean realistic = bench != null && bench.realistic();
        Map<String, PartThermal> thermals = realistic ? PartThermal.of(object) : Map.of();
        for (Netlist.Element element : netlist.elements()) {
            PartThermal thermal = element.type() == org.jmouse.parallax.object.ObjectDefinition.CircuitElementType.RESISTOR
                    ? thermals.get(element.id()) : null;
            built.add(thermal != null ? Device.Resistor.heated(element, thermal) : Device.of(element, built.size()));
        }
        for (Netlist.ModelUnit unit : netlist.units()) {
            built.add(new ModelDevice(unit, events.discrete(unit.label())));
        }
        for (Netlist.Element capacitor : events.inputCapacitances()) {
            built.add(new Device.Capacitor(capacitor, built.size()));
        }
        for (OutputBridge bridge : events.bridges()) {
            bridge.slot(built.size());
            built.add(bridge);
        }
        // A realistic bench is noisy: every device's noise, injected as currents (none on an ideal bench, whose answers
        // stay the textbook's).
        if (realistic) {
            built.add(new NoiseInjection(built, sampleInterval, bench.temperature() + 273.15, 1));
        }
        this.devices = List.copyOf(built);
        this.invariantDevices = devices.stream().filter(device -> !device.nonlinear() && device.invariant()).toList();
        this.linearDevices = devices.stream().filter(device -> !device.nonlinear() && !device.invariant()).toList();
        this.nonlinearDevices = devices.stream().filter(Device::nonlinear).toList();
        this.nonlinear = !nonlinearDevices.isEmpty();
        Map<String, Integer> partIndex = new LinkedHashMap<>();
        this.partOfDevice = new int[devices.size()];
        for (int index = 0; index < devices.size(); index++) {
            String part = partName(devices.get(index).part());
            partOfDevice[index] = part == null ? -1 : partIndex.computeIfAbsent(part, ignored -> partIndex.size());
        }
        this.parts = partIndex.keySet().toArray(String[]::new);
        this.thermalResistances = new double[parts.length];
        this.energy = new double[parts.length];
        for (int index = 0; index < devices.size(); index++) {
            int part = partOfDevice[index];
            if (part >= 0) {
                thermalResistances[part] = Math.max(thermalResistances[part], devices.get(index).thermalResistance());
            }
        }
        this.digital = events.active();
        this.signals = netlist.elements().stream().map(Netlist.Element::signal).filter(java.util.Objects::nonNull).toList();
        this.interval = sampleInterval;
        double limit = Double.POSITIVE_INFINITY;
        for (Device device : devices) {
            limit = Math.min(limit, device.maximumStep());
        }
        this.deviceMaximumStep = limit;
        this.maximumStep = Math.min(limit, sampleInterval * FREE_STEP);
        this.minimumStep = sampleInterval * 1e-9;
        this.crossingResolution = Math.max(sampleInterval * 1e-3, minimumStep * 10);
        this.state = new double[netlist.size()];
        this.companion = new double[devices.size()];
        for (Device device : devices) {
            device.initialize(state);
        }
        if (digital) {
            events.start(state);
        }
        for (Device device : devices) {
            device.begin(state);
        }
        this.step = maximumStep / 8;
        this.nodeLows = new double[netlist.nodes().size()];
        this.nodeHighs = new double[netlist.nodes().size()];
        this.nodeIntegrals = new double[netlist.nodes().size()];
        this.markedIntegrals = new double[netlist.nodes().size()];
        forgetPeaks();
    }

    /** A circuit's own bench conditions; null where it has none. */
    static org.jmouse.parallax.object.ObjectDefinition.BenchConditions benchOf(CompiledObject object) {
        return object.definition() instanceof org.jmouse.parallax.object.ObjectDefinition.Circuit circuit ? circuit.conditions() : null;
    }

    /**
     * The circuit with new values, going on from where {@code previous} is: the same time, node voltages and device
     * histories, the integration restarting as at a switch edge.
     *
     * @throws IllegalArgumentException when the circuit cannot be continued — its topology changed, or it is digital
     */
    static LiveIntegrator continuing(CompiledObject object, LiveIntegrator previous) {
        return continuing(object, previous, previous.interval);
    }

    /**
     * The same, sampled at a new interval from now on: the first sample is the first instant of the new grid at or after
     * where the integration stands, so the samples go on without a step back in time.
     */
    static LiveIntegrator continuing(CompiledObject object, LiveIntegrator previous, double sampleInterval) {
        return continuing(object, previous, sampleInterval, benchOf(object));
    }

    /** The same, on the bench of the whole circuit the piece belongs to. */
    static LiveIntegrator continuing(CompiledObject object, LiveIntegrator previous, double sampleInterval,
                                     org.jmouse.parallax.object.ObjectDefinition.BenchConditions bench) {
        LiveIntegrator next = new LiveIntegrator(object, sampleInterval, bench);
        synchronized (previous) {
            if (next.digital != previous.digital || next.state.length != previous.state.length
                    || next.companion.length != previous.companion.length
                    || !next.netlist.nodes().equals(previous.netlist.nodes())) {
                throw new IllegalArgumentException("The circuit changed its parts; it starts again.");
            }
            if (next.digital) {
                next.events.continueFrom(previous.events);
            }
            next.state = previous.state.clone();
            next.companion = previous.companion.clone();
            next.time = previous.time;
            next.sampleIndex = sampleInterval == previous.interval ? previous.sampleIndex
                    : (long) Math.ceil(previous.time / sampleInterval - 1e-9);
            next.restart = true;
            next.earlier = null;
            next.oldest = null;
        }
        for (Device device : next.devices) {
            device.begin(next.state);
        }
        return next;
    }

    /** The node names, in the order a sample reports them. */
    List<String> nodes() {
        return netlist.nodes();
    }

    double interval() {
        return interval;
    }

    synchronized double time() {
        return time;
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
        double free = every > 1 ? Math.max(interval * FREE_STEP, Math.min(interval * every, ACCURATE_STEP)) : interval * FREE_STEP;
        maximumStep = Math.min(deviceMaximumStep, free);
        traceWindow = every > 1 ? layTraceWindow() : null;
    }

    /** Reads these parts on every sample from now on — the current into each terminal and the power —; none reads none. */
    synchronized void meter(java.util.Set<String> parts) {
        metered = java.util.Set.copyOf(parts);
    }

    /** Keeps the peaks of these differences — pairs of this piece's nodes, −1 for ground — from the next step on. */
    synchronized void difference(List<LiveCircuitSolver.NodeDifference> pairs) {
        differences = List.copyOf(pairs);
        differenceLows = new double[pairs.size()];
        differenceHighs = new double[pairs.size()];
        Arrays.fill(differenceLows, Double.POSITIVE_INFINITY);
        Arrays.fill(differenceHighs, Double.NEGATIVE_INFINITY);
    }

    /** One watched difference in these voltages. */
    private static double across(double[] voltages, LiveCircuitSolver.NodeDifference pair) {
        return voltageOf(voltages, pair.positive()) - voltageOf(voltages, pair.negative());
    }

    /** A node's voltage among these; ground, −1, is 0. */
    private static double voltageOf(double[] voltages, int node) {
        return node < 0 ? 0 : voltages[node];
    }

    /** The next {@code count} samples, the first at the time the last call ended. */
    synchronized List<LiveCircuitSolver.Sample> advance(int count) {
        if (count < 1 || count > 256) {
            throw new IllegalArgumentException("Advance 1…256 samples at a time.");
        }
        List<LiveCircuitSolver.Sample> output = new ArrayList<>(count);
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
    synchronized List<LiveCircuitSolver.Sample> pass(long count) {
        if (count < 1) {
            throw new IllegalArgumentException("Pass at least one sample.");
        }
        List<LiveCircuitSolver.Sample> traced = new ArrayList<>();
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
        forgetPeaks();
        return traced;
    }

    /** Steps until the integration has passed {@code at}, allowed so many steps for each of the samples it goes through. */
    private void reach(double at, long samples) {
        long steps = 0;
        long allowed = STEPS_PER_SAMPLE * Math.max(1, samples);
        while (time < at - minimumStep) {
            step();
            if (++steps > allowed) {
                throw new IllegalArgumentException("The circuit needs too many steps near t = " + time + " s.");
            }
        }
    }

    /** The sample at {@code at}, which the integration has just passed: traced, it carries the stretch's window or its currents. */
    private LiveCircuitSolver.Sample sampleAt(double at, boolean traced, boolean read) {
        // The currents of the step that reached the sample: at most one step, and so one sample, after it.
        Map<String, Map<String, Double>> terminals = null;
        LiveCircuitSolver.Window window = null;
        if (traced && traceWindow != null && traceWindow.duration() >= traceEvery * interval / 2) {
            TraceWindow.Closed closed = traceWindow.close();
            terminals = closed.terminals();
            window = closed.window();
        } else if (traced) {
            // Too little of the stretch went by in whole steps to stand for it: the instant does.
            if (traceWindow != null) {
                traceWindow.clear();
            }
            terminals = terminalCurrents(tracedParts);
        }
        Map<String, LiveCircuitSolver.Metered> measured = read && readings && !metered.isEmpty() ? measuredAt(at) : null;
        double[] voltages = Arrays.copyOf(valuesAt(at), netlist.nodes().size());
        LiveCircuitSolver.Envelope envelope = read && readings ? peaksWith(voltages, at) : null;
        return new LiveCircuitSolver.Sample(at, voltages, terminals, measured, window, envelope);
    }

    /**
     * The peaks since the last read sample, this sample's own voltages among them, and the next ones begun from them: an
     * interval holds both its ends. Begun empty, the interval a square wave falls in saw only the low level after the
     * fall — the step that lands on an edge ends at the new level — and its high half vanished from the peaks.
     */
    private LiveCircuitSolver.Envelope peaksWith(double[] voltages, double at) {
        double[] lows = new double[voltages.length];
        double[] highs = new double[voltages.length];
        double[] means = new double[voltages.length];
        for (int node = 0; node < voltages.length; node++) {
            lows[node] = Math.min(nodeLows[node], voltages[node]);
            highs[node] = Math.max(nodeHighs[node], voltages[node]);
            // The integral at the sample, taken back from the step's end along the line to the sample's value.
            double integral = nodeIntegrals[node] - (landedOnEdge ? earlier[node] : (state[node] + voltages[node]) / 2) * (time - at);
            means[node] = Double.isNaN(markedTime) || !(at > markedTime) ? voltages[node] : (integral - markedIntegrals[node]) / (at - markedTime);
            markedIntegrals[node] = integral;
        }
        markedTime = at;
        System.arraycopy(voltages, 0, nodeLows, 0, voltages.length);
        System.arraycopy(voltages, 0, nodeHighs, 0, voltages.length);
        double[] lowsAcross = new double[differences.size()];
        double[] highsAcross = new double[differences.size()];
        for (int pair = 0; pair < differences.size(); pair++) {
            double difference = across(voltages, differences.get(pair));
            lowsAcross[pair] = Math.min(differenceLows[pair], difference);
            highsAcross[pair] = Math.max(differenceHighs[pair], difference);
            differenceLows[pair] = difference;
            differenceHighs[pair] = difference;
        }
        return new LiveCircuitSolver.Envelope(lows, highs, means, lowsAcross, highsAcross);
    }

    private void forgetPeaks() {
        markedTime = Double.NaN;
        Arrays.fill(nodeLows, Double.POSITIVE_INFINITY);
        Arrays.fill(nodeHighs, Double.NEGATIVE_INFINITY);
        Arrays.fill(differenceLows, Double.POSITIVE_INFINITY);
        Arrays.fill(differenceHighs, Double.NEGATIVE_INFINITY);
    }

    /**
     * The state at a sample time the integration has passed: on the parabola through the last three accepted points,
     * or the line through the last two. The solver steps as its error allows and stops only at edges and events — a
     * sample is read off the solution, it does not force a step to land on it. A calm stretch between two edges is a
     * few long steps, not one per sample.
     */
    private double[] valuesAt(double at) {
        double back = at - time;
        if (back >= -minimumStep || earlier == null) {
            return state;
        }
        if (landedOnEdge && back > -earlierStep) {
            return earlier;
        }
        if (oldest != null && back >= -(earlierStep + oldestStep)) {
            return parabola(oldest, oldestStep, earlier, earlierStep, state, back);
        }
        return extrapolated(earlier, earlierStep, state, back);
    }

    /** Brings the step's start into every node's and every watched difference's peaks, and its stretch into the means. */
    private void keepPeaks(double attempt) {
        for (int node = 0; node < nodeLows.length; node++) {
            nodeLows[node] = Math.min(nodeLows[node], earlier[node]);
            nodeIntegrals[node] += (landedOnEdge ? earlier[node] : (earlier[node] + state[node]) / 2) * attempt;
            nodeHighs[node] = Math.max(nodeHighs[node], earlier[node]);
        }
        for (int pair = 0; pair < differences.size(); pair++) {
            double difference = across(earlier, differences.get(pair));
            differenceLows[pair] = Math.min(differenceLows[pair], difference);
            differenceHighs[pair] = Math.max(differenceHighs[pair], difference);
        }
    }

    /** The level every digital node of this piece stands at now; none when the piece has no digital part. */
    synchronized Map<String, Object> levels() {
        return digital ? Map.copyOf(events.levels()) : Map.of();
    }

    /** Traces only these parts' terminal currents from now on; null traces every part. */
    synchronized void traceOnly(java.util.Set<String> parts) {
        tracedParts = parts == null ? null : java.util.Set.copyOf(parts);
    }

    /** Stops keeping the readings — energy, peaks, means —: the samples carry the solution alone from now on. */
    void solutionOnly() {
        readings = false;
    }

    /** One accepted step of the integration, retried as often as its error or a crossing asks. */
    private void step() {
        // Whether Newton has already failed at this time point: a step is cut for it once, not again.
        boolean unsettledOnce = false;
        while (true) {
            double stop = digital ? events.nextTime(time) : Double.POSITIVE_INFINITY;
            boolean stopIsEdge = Double.isFinite(stop);
            double attempt = Math.min(Math.min(step, maximumStep), stop - time);
            boolean landsOnStop = stop - (time + attempt) <= minimumStep;
            if (landsOnStop) {
                attempt = stop - time;
            }
            // A generator's edge inside the step: the step ends on it, the level after the jump already in force, and the
            // next one restarts as after any switch. Located to the resolution a timer's edge is, never finer: an edge a
            // rounding error after a sample is that sample's, not a step of its own.
            double signalEdge = firstSignalEdge(time, time + attempt);
            // A long step does not go straight onto an edge: it stops one sample short, and a short one lands. The step
            // that lands is solved with the level after the jump, so a sample inside it is read off a ramp — a square
            // wave drawn with slanted sides as long as the step was. One sample long, it is as before steps were free.
            double approach = signalEdge - interval;
            if (!Double.isNaN(signalEdge) && approach - time > crossingResolution) {
                attempt = Math.min(attempt, approach - time);
            } else if (!Double.isNaN(signalEdge) && signalEdge - time > crossingResolution) {
                if (signalEdge < time + attempt - crossingResolution) {
                    attempt = signalEdge - time;
                }
                // The step lands where it ends: the time is set from the stop, so the stop is the edge.
                stop = time + attempt;
                landsOnStop = true;
                stopIsEdge = true;
            } else if (!Double.isNaN(signalEdge)) {
                // An edge nearer than a step may land is this step's to carry, as a step that lands on one does: its
                // jump is not held to the smoothness test, and the next step restarts. Crossed as a smooth step, the jump
                // failed the test at every length, and the step shrank towards the edge until the run gave up.
                stop = time + attempt;
                landsOnStop = true;
                stopIsEdge = true;
            }
            if (attempt <= 0) {
                throw new IllegalArgumentException("The circuit cannot advance past t = " + time + " s.");
            }

            Assembly.Integration integration = restart ? Assembly.Integration.BACKWARD_EULER : Assembly.Integration.TRAPEZOIDAL;
            double[] companionTrial = companion.clone();
            Assembly assembly = Assembly.transientStep(system, state, companionTrial, time + attempt, attempt, integration);
            boolean quadratic = !restart && earlier != null && oldest != null;
            double[] predicted = earlier == null || restart ? state : quadratic
                    ? parabola(oldest, oldestStep, earlier, earlierStep, state, attempt) : extrapolated(earlier, earlierStep, state, attempt);
            boolean mayCut = !unsettledOnce && attempt / UNSETTLED_CUT >= minimumStep;
            double[] solution = newton(assembly, predicted, mayCut);
            unsettledOnce |= solution == null;
            boolean landsOnEdge = landsOnStop && stopIsEdge;
            // Through three points the prediction misses by about thirteen times the trapezoidal rule's own error (Milne);
            // through two it is a coarser bound and is taken as it is.
            double error = solution == null || earlier == null || restart || landsOnEdge ? 0
                    : truncationRatio(solution, predicted) / (quadratic ? MILNE : 1);
            double order = quadratic ? 3 : 2;
            boolean accurate = solution != null && error <= 1;
            // A crossing inside the step: aim the next attempt just past it, on the line between the step's ends —
            // an edge is found in one or two attempts instead of halving towards it a dozen times.
            double share = accurate && digital && attempt > crossingResolution
                    ? events.crossingShare(state, time, solution, time + attempt) : 1;
            boolean crossingUnresolved = share < 1 && (1 - share) * attempt > crossingResolution;
            if (!accurate || crossingUnresolved) {
                step = crossingUnresolved ? Math.max(crossingResolution / 2, share * attempt + crossingResolution / 2) : solution == null ? attempt / UNSETTLED_CUT : attempt * stepFactor(error, order);
                if (step < minimumStep) {
                    throw new IllegalArgumentException("The circuit does not settle at t = " + time + " s.");
                }
                continue;
            }
            for (Device device : devices) {
                device.accept(assembly, solution);
            }
            companion = companionTrial;
            // A step that lands on an edge held the level before the jump all along; its end is the level after.
            boolean landing = landsOnStop && stopIsEdge;
            if (readings) {
                accumulateEnergy(landing ? state : solution, landing ? time : time + attempt, attempt);
            }
            oldest = restart ? null : earlier;
            oldestStep = earlierStep;
            earlier = state;
            earlierStep = attempt;
            state = solution;
            time = landsOnStop ? stop : time + attempt;
            restart = landsOnStop && stopIsEdge;
            landedOnEdge = landsOnStop && stopIsEdge;
            if (digital && events.advance(state, time)) {
                restart = true;
            }
            step = restart ? Math.min(maximumStep, crossingResolution * 4) : Math.min(attempt * stepFactor(error, order), maximumStep);
            if (traceWindow != null) {
                takeIntoTraceWindow(attempt, landedOnEdge ? earlier : state, landedOnEdge ? time - attempt : time);
            }
            // A step brings its START into the peaks: its end may lie past the sample it reached, and belongs to the next
            // interval — taken as the end, an interval's band held a value up to a step ahead of it, and a sine grew teeth
            // leading the wave. The sample's own value closes the interval; the next step brings this end in.
            if (readings) {
                keepPeaks(attempt);
            }
            if (readings && !metered.isEmpty()) {
                measuredBefore = measuredAfter;
                measuredBeforeTime = measuredAfterTime;
                measuredAfter = measured();
                measuredAfterTime = time;
            }
            return;
        }
    }

    /**
     * What the metered parts read at {@code at}, between the ends of the step that reached it: their currents and power
     * taken at both ends and laid along the step, as a sample's voltages are. Read at the step's end alone, a current
     * stood up to a step after its sample — a clamp on a 1.2 kHz series RLC read the current 2.6° ahead of where it is,
     * while the voltages beside it were exact. A part's states are the step end's: a state is a level, not a wave.
     */
    private Map<String, LiveCircuitSolver.Metered> measuredAt(double at) {
        if (measuredAfter == null) {
            return measured();
        }
        if (measuredBefore == null || !(measuredAfterTime > measuredBeforeTime)) {
            return measuredAfter;
        }
        double share = Math.min(1, Math.max(0, (at - measuredBeforeTime) / (measuredAfterTime - measuredBeforeTime)));
        Map<String, LiveCircuitSolver.Metered> between = new LinkedHashMap<>();
        measuredAfter.forEach((part, after) -> {
            LiveCircuitSolver.Metered before = measuredBefore.get(part);
            if (before == null || after.terminals() == null || before.terminals() == null) {
                between.put(part, after);
                return;
            }
            Map<String, Double> terminals = new LinkedHashMap<>();
            after.terminals().forEach((terminal, current) -> {
                Double earlier = before.terminals().get(terminal);
                terminals.put(terminal, earlier == null ? current : earlier + (current - earlier) * share);
            });
            double power = before.power() + (after.power() - before.power()) * share;
            between.put(part, new LiveCircuitSolver.Metered(terminals, power, after.states()));
        });
        return between;
    }

    /** Each part's energy over an accepted step, as its power at the step's end times the step. */
    private void accumulateEnergy(double[] solution, double at, double duration) {
        for (int index = 0; index < partOfDevice.length; index++) {
            int part = partOfDevice[index];
            if (part >= 0) {
                energy[part] += devices.get(index).power(solution, at) * duration;
            }
        }
        energyTime += duration;
    }

    /**
     * What every part did since the last reading — its average power, the current into each of its terminals now — and
     * starts the next one.
     */
    synchronized List<PartReading> readParts() {
        Map<String, Map<String, Double>> terminals = terminalCurrents();
        double[] heatings = new double[parts.length];
        java.util.Arrays.fill(heatings, Double.NaN);
        for (int index = 0; index < devices.size(); index++) {
            if (partOfDevice[index] >= 0) {
                double heating = devices.get(index).heating(state);
                if (!Double.isNaN(heating)) {
                    int part = partOfDevice[index];
                    heatings[part] = Double.isNaN(heatings[part]) ? heating : Math.max(heatings[part], heating);
                }
            }
        }
        List<PartReading> readings = new ArrayList<>(parts.length);
        for (int part = 0; part < parts.length; part++) {
            double power = energyTime > 0 ? energy[part] / energyTime : 0;
            readings.add(new PartReading(parts[part], power, terminals.get(parts[part]), thermalResistances[part], heatings[part]));
        }
        java.util.Arrays.fill(energy, 0);
        energyTime = 0;
        return readings;
    }

    /**
     * A trace window with a column for every part's terminal: a two-terminal device puts its current into its two
     * columns, read as one number; a model into its pins' columns by name. Devices of one part on one terminal add up.
     */
    @SuppressWarnings("unchecked")
    private TraceWindow layTraceWindow() {
        Map<String, Integer> columns = new LinkedHashMap<>();
        List<String> columnParts = new ArrayList<>();
        List<String> columnTerminals = new ArrayList<>();
        java.util.function.BiFunction<String, String, Integer> column = (part, terminal) ->
                columns.computeIfAbsent(part + '\u0000' + terminal, ignored -> {
                    columnParts.add(part);
                    columnTerminals.add(terminal);
                    return columnParts.size() - 1;
                });
        terminalColumns = new int[devices.size()][];
        modelColumns = new Map[devices.size()];
        for (int index = 0; index < devices.size(); index++) {
            int part = partOfDevice[index];
            if (part < 0 || !ownTerminals(index)) {
                continue;
            }
            Device device = devices.get(index);
            String name = parts[part];
            if (!Double.isNaN(device.current(state, companion, time))) {
                String[] names = device.currentTerminals();
                terminalColumns[index] = new int[]{column.apply(name, names[0]), column.apply(name, names[1])};
            } else {
                Map<String, Integer> byPin = new java.util.HashMap<>();
                device.terminalCurrents(state, companion, time).keySet().forEach(pin -> byPin.put(pin, column.apply(name, pin)));
                modelColumns[index] = byPin;
            }
        }
        stepCurrents = new double[columnParts.size()];
        return new TraceWindow(netlist.nodes().size(), columnParts.toArray(String[]::new), columnTerminals.toArray(String[]::new));
    }

    /** One accepted step into the trace window: every terminal column's current over it — {@code held} — without a map. */
    private void takeIntoTraceWindow(double step, double[] held, double at) {
        Arrays.fill(stepCurrents, 0);
        for (int index = 0; index < devices.size(); index++) {
            int[] twoColumns = terminalColumns[index];
            if (twoColumns != null) {
                double through = devices.get(index).current(held, companion, at);
                stepCurrents[twoColumns[0]] += through;
                stepCurrents[twoColumns[1]] -= through;
            } else if (modelColumns[index] != null) {
                for (Map.Entry<String, Double> pin : devices.get(index).terminalCurrents(held, companion, at).entrySet()) {
                    Integer column = modelColumns[index].get(pin.getKey());
                    if (column != null) {
                        stepCurrents[column] += pin.getValue();
                    }
                }
            }
        }
        traceWindow.take(held, stepCurrents, step);
    }

    /** The current into each of every part's terminals now, by part and then by terminal. */
    private Map<String, Map<String, Double>> terminalCurrents() {
        return terminalCurrents(null);
    }

    /** The terminal currents of the parts named, or of every part when none are. */
    private Map<String, Map<String, Double>> terminalCurrents(java.util.Set<String> only) {
        Map<String, Map<String, Double>> terminals = new LinkedHashMap<>();
        for (String part : parts) {
            if (only == null || only.contains(part)) {
                terminals.put(part, new LinkedHashMap<>());
            }
        }
        for (int index = 0; index < partOfDevice.length; index++) {
            int part = partOfDevice[index];
            if (part >= 0 && (only == null || only.contains(parts[part])) && ownTerminals(index)) {
                Map<String, Double> into = terminals.get(parts[part]);
                devices.get(index).terminalCurrents(state, companion, time).forEach((terminal, current) ->
                        into.merge(terminal, current, Double::sum));
            }
        }
        return terminals;
    }

    /** A part's own terminals: a primitive's, or a model's pins — not those of a pin capacitance inside it. */
    private boolean ownTerminals(int index) {
        Device device = devices.get(index);
        return device instanceof ModelDevice || parts[partOfDevice[index]].equals(device.part());
    }

    /** What the metered parts do now: the current into each terminal and the power, every device of the part summed. */
    /** What the metered parts read at the ends of the last two steps, for {@link #measuredAt}. */
    private Map<String, LiveCircuitSolver.Metered> measuredBefore;
    private double measuredBeforeTime = Double.NaN;
    private Map<String, LiveCircuitSolver.Metered> measuredAfter;
    private double measuredAfterTime = Double.NaN;

    private Map<String, LiveCircuitSolver.Metered> measured() {
        Map<String, Map<String, Double>> terminals = new LinkedHashMap<>();
        Map<String, Double> power = new LinkedHashMap<>();
        Map<String, Map<String, Double>> states = new LinkedHashMap<>();
        for (int index = 0; index < partOfDevice.length; index++) {
            int part = partOfDevice[index];
            if (part < 0 || !metered.contains(parts[part])) {
                continue;
            }
            Device device = devices.get(index);
            power.merge(parts[part], device.power(state, time), Double::sum);
            states.computeIfAbsent(parts[part], ignored -> new LinkedHashMap<>()).putAll(device.stateValues(state));
            Map<String, Double> into = terminals.computeIfAbsent(parts[part], ignored -> new LinkedHashMap<>());
            if (ownTerminals(index)) {
                device.terminalCurrents(state, companion, time).forEach((terminal, current) -> into.merge(terminal, current, Double::sum));
            }
        }
        Map<String, LiveCircuitSolver.Metered> measured = new LinkedHashMap<>();
        power.forEach((part, watts) -> measured.put(part, new LiveCircuitSolver.Metered(terminals.get(part), watts, states.get(part))));
        return measured;
    }

    /** A device inside a part (`U1.trigger.Cin`, a model's unit `A1.b`) belongs to that part (`U1`, `A1`). */
    private static String partName(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.indexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private double firstSignalEdge(double from, double to) {
        double first = Double.NaN;
        for (SourceSignal signal : signals) {
            double edge = signal.edgeWithinStep(from, to, crossingResolution);
            if (!Double.isNaN(edge) && (Double.isNaN(first) || edge < first)) {
                first = edge;
            }
        }
        return first;
    }

    /**
     * Newton as SPICE runs it at a time point — ten iterations, and null if they do not settle while the step
     * {@code mayCut}, so that it is cut —; only a cut step that still does not settle is given a longer run, then a line
     * search. A hard instant (a transistor letting go of a coil's current) needed some fifty iterations; ten for every
     * step would make every easy one wait for it.
     *
     * <p>Cut once, because a shorter step starts closer to its answer: an NE555 switching drags its pins across the flat
     * of a saturation, and four PWM blocks spent a third of all their iterations rescuing such steps that, cut, settle
     * in a few. Not twice, because where the cut step does not settle either, the length is not what is wrong: a
     * converter's first edge settled plainly at no length down to 10⁻¹⁶ s, and cutting it towards that ran out of
     * steps.</p>
     */
    private double[] newton(Assembly assembly, double[] start, boolean mayCut) {
        double[] plain = newton(assembly, start, false, NEWTON_ITERATIONS);
        if (plain != null || !nonlinear || mayCut) {
            return plain;
        }
        double[] longer = newton(assembly, start, false, LONGER_ITERATIONS);
        return longer != null ? longer : newton(assembly, start, true, SEARCH_ITERATIONS);
    }

    /**
     * One Newton–Raphson run. With {@code searching}, a line search — the fallback when plain Newton did not settle: a
     * step is kept only where the equations hold better than where it started; otherwise it is halved, back towards
     * the point it left. A junction inside a model that turns on within a step (a relay driver's transistor turning
     * off, the coil's current forced into the flyback diode) was handed a ten-volt step and its exponential threw the
     * supply's current to 10¹⁰ A: no step was short enough for plain Newton, and the run stopped.
     */
    private double[] newton(Assembly assembly, double[] start, boolean searching, int iterations) {
        if (netlist.size() == 0) {
            return new double[0];
        }
        double[] guess = start;
        double[] accepted = null;
        double[] direction = null;
        double acceptedResidual = Double.POSITIVE_INFINITY;
        double fraction = 1;
        for (int iteration = 0; iteration < iterations; iteration++) {
            // What does not read the guess is the same in every iteration of the attempt: stamped once, then copied.
            if (iteration == 0) {
                if (!invariantStamped) {
                    assembly.begin(guess);
                    for (Device device : invariantDevices) {
                        device.stamp(assembly);
                    }
                    assembly.keepAsConstant();
                    invariantStamped = true;
                }
                assembly.beginFromConstant(guess);
                for (Device device : linearDevices) {
                    device.stamp(assembly);
                }
                assembly.keepAsBase();
            } else {
                assembly.beginFromBase(guess);
            }
            for (Device device : nonlinearDevices) {
                device.stamp(assembly);
            }
            if (searching) {
                double residual = assembly.residual();
                if (accepted != null && !(residual < acceptedResidual) && fraction > SMALLEST_FRACTION) {
                    fraction /= 2;
                    guess = along(accepted, direction, fraction);
                    continue;
                }
                accepted = guess;
                acceptedResidual = residual;
                fraction = 1;
            }
            double[] next = assembly.solve();
            if (next == null) {
                if (iteration == 0) {
                    throw new IllegalArgumentException("The circuit has no unique solution.");
                }
                return null;
            }
            if (!nonlinear) {
                return next;
            }
            // Already on the first iteration: it is linearized exactly at the prediction — no junction is limited
            // before it has a previous voltage — so an answer that did not move from there is off by the square of a
            // move below the tolerance. SPICE's second, confirming iteration buys nothing here.
            if (settled(guess, next)) {
                return next;
            }
            for (double value : next) {
                if (!Double.isFinite(value)) {
                    return null;
                }
            }
            double[] stepped = damped(guess, next);
            if (searching) {
                direction = new double[stepped.length];
                for (int index = 0; index < stepped.length; index++) {
                    direction[index] = stepped[index] - guess[index];
                }
            }
            guess = stepped;
        }
        return null;
    }

    /** The point a share of the way along a Newton step. */
    private static double[] along(double[] from, double[] direction, double fraction) {
        double[] point = new double[from.length];
        for (int index = 0; index < from.length; index++) {
            point[index] = from[index] + direction[index] * fraction;
        }
        return point;
    }

    private double[] damped(double[] guess, double[] next) {
        return NewtonSteps.limited(guess, next, netlist.nodes().size(), netlist.branches().size());
    }

    private boolean settled(double[] guess, double[] next) {
        for (int index = 0; index < next.length; index++) {
            if (Math.abs(next[index] - guess[index]) > SETTLED_RELATIVE * Math.abs(next[index]) + ABSOLUTE_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /**
     * How much of the allowed local error the step used: the largest share any device reports for what it stores
     * (see {@link Truncation}). At most 1 is accurate. The error of the prediction grows with a power of the step,
     * which is what {@link #stepFactor} inverts.
     */
    private double truncationRatio(double[] solution, double[] predicted) {
        double largest = 0;
        for (Device device : devices) {
            double share = device.truncation(solution, predicted);
            largest = Math.max(largest, share);
        }
        return largest;
    }

    /**
     * The next step as a multiple of this one, from how much of the allowed error it used: a step that used a quarter
     * may double, one that used it all stays a little smaller, one that used too much shrinks just enough — never the
     * blind halving and doubling that saws between two sizes and throws away a third of its work.
     */
    private static double stepFactor(double error, double order) {
        if (error <= 0) {
            return GROWTH;
        }
        return Math.max(0.2, Math.min(GROWTH, 0.9 * Math.pow(error, -1 / order)));
    }

    /** The value at {@code step} past now on the parabola through the last three accepted states. */
    private static double[] parabola(double[] oldest, double oldestStep, double[] earlier, double earlierStep, double[] state, double step) {
        double near = earlierStep;
        double far = earlierStep + oldestStep;
        double atState = (step + near) * (step + far) / (near * far);
        double atEarlier = -step * (step + far) / (near * oldestStep);
        double atOldest = step * (step + near) / (far * oldestStep);
        double[] result = new double[state.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = atState * state[index] + atEarlier * earlier[index] + atOldest * oldest[index];
        }
        return result;
    }

    private static double[] extrapolated(double[] earlier, double earlierStep, double[] state, double step) {
        double[] result = new double[state.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = state[index] + (state[index] - earlier[index]) * step / earlierStep;
        }
        return result;
    }
}
