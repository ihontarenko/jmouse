package org.jmouse.parallax.circuit;

import org.jmouse.parallax.numerics.SparseSystem;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The integration of one piece of a circuit through time: SPICE's step control — a step as long as its local error
 * allows, cut where Newton does not settle —, landing on every edge a generator or a digital event has, predicted
 * through the last three accepted points. It knows nothing of what anyone reads; {@link LiveIntegrator} reads it.
 *
 * <p>Each accepted step is reported, at the one point a step is whole — every device has accepted it, nothing has moved
 * on yet —, to the {@link AcceptedStep} the caller gives: what a part's energy and heat are gathered from.</p>
 */
final class TransientStepper {

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
    /** Corrector minus a three-point predictor is about thirteen trapezoidal errors; ten keeps a margin. */
    private static final double MILNE               = 10;
    /** How much a step may grow at once when it used little of its allowed error. */
    private static final double GROWTH              = 2;

    /** What hears of each accepted step, while it is whole. */
    interface AcceptedStep {

        /**
         * One accepted step.
         *
         * @param solution the state at its end
         * @param at       its end, seconds
         * @param duration how long it was
         */
        void accepted(double[] solution, double at, double duration);
    }

    private final Netlist             netlist;
    /** The one linear system every Newton iteration assembles into. */
    private final SparseSystem        system;
    private final EventSimulation     events;
    private final List<Device>        devices;
    /** The devices nothing in a run changes, stamped once for it ({@link Device#invariant}). */
    private final List<Device>        invariantDevices;
    private boolean                   invariantStamped;
    /** The devices whose stamp does not read the Newton guess, stamped once per attempt; and those that do. */
    private final List<Device>        linearDevices;
    private final List<Device>        nonlinearDevices;
    /** The devices that carry something from step to step ({@link Device#stores}): all a step's error and acceptance read. */
    private final List<Device>        storingDevices;
    private final boolean             nonlinear;
    private final boolean             digital;
    /** Newton on this piece's system, settling as a transient step does. */
    private final CircuitNewton       newton;
    /** How finely this piece is solved: the bench's level of detail. */
    private final Precision           precision;
    /** The generators' signals, whose edges a step lands on when it would cross one. */
    private final List<SourceSignal>  signals;
    /** The longest step the devices allow — a generator's edges —; infinite when none limits it. */
    private final double              deviceMaximumStep;
    /** The longest step now: the devices' limit, and the stretch the caller allows. */
    private double                    maximumStep;
    private final double              minimumStep;
    private final double              crossingResolution;

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

    /**
     * @param interval the time between two samples: the longest step is one, until the caller allows more
     */
    TransientStepper(Netlist netlist, List<Device> devices, EventSimulation events, double interval, Precision precision) {
        this.netlist = netlist;
        this.system = new SparseSystem(netlist.size());
        this.events = events;
        this.devices = List.copyOf(devices);
        this.invariantDevices = this.devices.stream().filter(device -> !device.nonlinear() && device.invariant()).toList();
        this.linearDevices = this.devices.stream().filter(device -> !device.nonlinear() && !device.invariant()).toList();
        this.nonlinearDevices = this.devices.stream().filter(Device::nonlinear).toList();
        this.storingDevices = this.devices.stream().filter(Device::stores).toList();
        this.nonlinear = !nonlinearDevices.isEmpty();
        this.precision = precision;
        this.newton = new CircuitNewton(netlist, nonlinear, precision.settling());
        this.digital = events.active();
        this.signals = netlist.elements().stream().map(Netlist.Element::signal).filter(Objects::nonNull).toList();
        double limit = Double.POSITIVE_INFINITY;
        for (Device device : this.devices) {
            limit = Math.min(limit, device.maximumStep());
        }
        this.deviceMaximumStep = limit;
        this.maximumStep = Math.min(limit, interval);
        this.minimumStep = interval * 1e-9;
        this.crossingResolution = Math.max(interval * 1e-3, minimumStep * 10);
        this.state = new double[netlist.size()];
        this.companion = new double[this.devices.size()];
        for (Device device : this.devices) {
            device.initialize(state);
        }
        if (digital) {
            events.start(state);
        }
        for (Device device : this.devices) {
            device.begin(state);
        }
        this.step = maximumStep / 8;
    }

    /**
     * Where the integration stands, kept apart from it: its time, its unknowns and companions, its nodes, its digital
     * life. Taking one copies a few arrays — cheap enough for every frame of a live run.
     */
    record Snapshot(double time, double[] state, double[] companion, List<String> nodes, EventSimulation.Snapshot events) {
    }

    Snapshot snapshot() {
        return new Snapshot(time, state.clone(), companion.clone(), netlist.nodes(), digital ? events.snapshot() : null);
    }

    /**
     * Goes on from where it stood — the same time, node voltages, companions and digital life —, the integration
     * restarting as at a switch edge: each device takes its history from the state, as at the start.
     *
     * @throws IllegalArgumentException when the piece's parts are not the ones it had
     */
    void continueFrom(Snapshot previous) {
        if (digital != (previous.events() != null) || state.length != previous.state().length
                || companion.length != previous.companion().length || !netlist.nodes().equals(previous.nodes())) {
            throw new IllegalArgumentException("The circuit changed its parts; it starts again.");
        }
        if (digital) {
            events.continueFrom(previous.events());
        }
        state = previous.state().clone();
        companion = previous.companion().clone();
        time = previous.time();
        restart = true;
        earlier = null;
        oldest = null;
        for (Device device : devices) {
            device.begin(state);
        }
    }

    /** Lets a step be up to {@code free} seconds — no longer than the devices allow. */
    void allow(double free) {
        maximumStep = Math.min(deviceMaximumStep, free);
    }

    double time() {
        return time;
    }

    double minimumStep() {
        return minimumStep;
    }

    double[] state() {
        return state;
    }

    /** The accepted state before the present one; null at the start and after a restart. */
    double[] earlier() {
        return earlier;
    }

    double[] companion() {
        return companion;
    }

    List<Device> devices() {
        return devices;
    }

    Netlist netlist() {
        return netlist;
    }

    /** The level every digital node stands at now; none when the piece has no digital part. */
    Map<String, LogicLevel> levels() {
        return digital ? Map.copyOf(events.levels()) : Map.of();
    }

    /**
     * The state at a time the integration has passed: on the parabola through the last three accepted points, or the
     * line through the last two. The solver steps as its error allows and stops only at edges and events — a sample is
     * read off the solution, it does not force a step to land on it.
     */
    double[] valuesAt(double at) {
        double back = at - time;
        if (back >= -minimumStep || earlier == null) {
            return state;
        }
        boolean throughThree = oldest != null && back >= -(earlierStep + oldestStep);
        return Prediction.ahead(state, earlier, earlierStep, throughThree ? oldest : null, oldestStep, back).values();
    }

    /**
     * One accepted step of the integration, retried as often as its error or a crossing asks.
     *
     * @return how long the accepted step was
     */
    double step(AcceptedStep listener) {
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
            // A generator's edge inside the step, located to the shortest step. The step ends at the last
            // instant before the jump, solved with the level before it — an ordinary step, held to the error test like
            // any other —, and the jump is carried by the next, as short as the edge is located to: SPICE's breakpoint.
            // Solved with the level after the jump instead, the step that landed was a ramp as long as itself — up to a
            // whole sample —, and a transistor switched by it turned within that one implicit step: its gate charge, its
            // Miller plateau and the drain's overshoot were there at a fine sample interval and gone at a coarse one.
            SourceSignal.Edge edge = firstSignalEdge(time, time + attempt);
            boolean carriesJump = false;
            if (edge != null && edge.before() - time > crossingResolution) {
                attempt = edge.before() - time;
                stop = edge.before();
                landsOnStop = true;
                stopIsEdge = true;
            } else if (edge != null) {
                // The jump is at this step's start: it ends at the new level, by backward Euler from where the last one
                // left off, and is not held to the smoothness test — crossed as a smooth step, the jump failed the test
                // at every length, and the step shrank towards the edge until the run gave up.
                carriesJump = true;
            }
            if (attempt <= 0) {
                throw new IllegalArgumentException("The circuit cannot advance past t = " + Moments.written(time) + ".");
            }

            boolean fresh = restart || carriesJump;
            Assembly.Integration integration = fresh ? Assembly.Integration.BACKWARD_EULER : Assembly.Integration.TRAPEZOIDAL;
            double[] companionTrial = companion.clone();
            Assembly assembly = Assembly.transientStep(system, state, companionTrial, time + attempt, attempt, integration);
            boolean quadratic = !fresh && earlier != null && oldest != null;
            Prediction prediction = fresh ? Prediction.ahead(state, null, 0, null, 0, attempt)
                    : Prediction.ahead(state, earlier, earlierStep, oldest, oldestStep, attempt);
            boolean mayCut = !unsettledOnce && attempt / UNSETTLED_CUT >= minimumStep;
            double[] solution = newton(assembly, prediction.values(), mayCut);
            unsettledOnce |= solution == null;
            if (solution != null) {
                for (Device device : storingDevices) {
                    device.measure(solution, time + attempt);
                }
            }
            // Through three points the prediction misses by about thirteen times the trapezoidal rule's own error (Milne);
            // through two it is a coarser bound and is taken as it is.
            double error = solution == null || earlier == null || fresh ? 0
                    : truncationRatio(solution, prediction) / (quadratic ? MILNE : 1) / precision.errorBudget();
            double order = quadratic ? 3 : 2;
            boolean accurate = solution != null && error <= 1;
            // A crossing inside the step: aim the next attempt just past it, on the line between the step's ends —
            // an edge is found in one or two attempts instead of halving towards it a dozen times.
            double share = accurate && digital && attempt > crossingResolution
                    ? events.crossingShare(state, time, solution, time + attempt) : 1;
            boolean crossingUnresolved = share < 1 && (1 - share) * attempt > crossingResolution;
            if (!accurate || crossingUnresolved) {
                step = crossingUnresolved ? Math.max(crossingResolution / 2, share * attempt + crossingResolution / 2)
                        : solution == null ? attempt / UNSETTLED_CUT : attempt * stepFactor(error, order);
                if (step < minimumStep) {
                    throw new IllegalArgumentException("The circuit does not settle at t = " + Moments.written(time) + ".");
                }
                continue;
            }
            for (Device device : storingDevices) {
                device.accept(assembly, solution);
            }
            companion = companionTrial;
            listener.accepted(solution, time + attempt, attempt);
            oldest = restart ? null : earlier;
            oldestStep = earlierStep;
            earlier = state;
            earlierStep = attempt;
            state = solution;
            time = landsOnStop ? stop : time + attempt;
            // Before a jump or just past one, the next step starts afresh and short.
            restart = landsOnStop && stopIsEdge || carriesJump;
            if (digital && events.advance(state, time)) {
                restart = true;
            }
            step = restart ? Math.min(maximumStep, crossingResolution * 4) : Math.min(attempt * stepFactor(error, order), maximumStep);
            return attempt;
        }
    }

    /**
     * The first generator's edge within a step, or null; located to the shortest step, so that a sample at the edge's
     * own instant reads the level before it, as SPICE reads a breakpoint, never a share of the jump.
     */
    private SourceSignal.Edge firstSignalEdge(double from, double to) {
        SourceSignal.Edge first = null;
        for (SourceSignal signal : signals) {
            SourceSignal.Edge edge = signal.edgeWithinStep(from, to, minimumStep);
            if (edge != null && (first == null || edge.after() < first.after())) {
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
        double[] plain = newton.solve(assembly, start, NEWTON_ITERATIONS, false, stamping);
        if (plain != null || !nonlinear || mayCut) {
            return plain;
        }
        double[] longer = newton.solve(assembly, start, LONGER_ITERATIONS, false, stamping);
        return longer != null ? longer : newton.solve(assembly, start, SEARCH_ITERATIONS, true, stamping);
    }

    /**
     * A step's system at a guess. What does not read the guess is the same in every iteration of an attempt: the
     * devices nothing changes are stamped once for the whole run, those that do not read the guess once per attempt,
     * and only the nonlinear ones in every iteration. A junction inside a model that turns on within a step (a relay
     * driver's transistor turning off, the coil's current forced into the flyback diode) was handed a ten-volt step and
     * its exponential threw the supply's current to 10¹⁰ A: the line search is what saves that step.
     */
    private final CircuitNewton.Linearization stamping = new CircuitNewton.Linearization() {

        @Override
        public void assemble(Assembly assembly, double[] guess, int iteration) {
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
        }

        /**
         * Whether a system singular where Newton started is the circuit's own — no unique solution anywhere — rather
         * than the point's. At the first moment it is the circuit: there is no earlier solution to have strayed from.
         * Later, an unknown no row or column mentions is the circuit too (a node a switch has just left floating).
         * Anything else is the guess's: a step predicted far past where the circuit can go — a Zener's breakdown, a
         * predictor that put its cathode at fifteen volts on a twelve-volt bench — linearizes to a matrix no pivot
         * survives, and the step is refused and cut instead of the run being stopped.
         */
        @Override
        public void singularAtStart(Assembly assembly) {
            if (!assembly.isTransient() || assembly.time() <= 0 || assembly.unconstrained() >= 0) {
                throw new IllegalArgumentException("The circuit has no unique solution.");
            }
        }
    };

    /**
     * How much of the allowed local error the step used: the largest share any device reports for what it stores
     * (see {@link Truncation}). At most 1 is accurate. The error of the prediction grows with a power of the step,
     * which is what {@link #stepFactor} inverts.
     */
    private double truncationRatio(double[] solution, Prediction prediction) {
        double largest = 0;
        for (Device device : storingDevices) {
            largest = Math.max(largest, device.truncation(solution, prediction));
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
}
