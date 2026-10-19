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
         * @param landing  whether it landed on an edge: it held the level before the jump all along, and its end is the
         *                 level after
         */
        void accepted(double[] solution, double at, double duration, boolean landing);
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
    private final boolean             nonlinear;
    private final boolean             digital;
    /** Newton on this piece's system, settling as a transient step does. */
    private final CircuitNewton       newton;
    /** The generators' signals, whose edges a step lands on when it would cross one. */
    private final List<SourceSignal>  signals;
    /** The sample interval: a long step stops one of them short of an edge. */
    private final double              interval;
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
     * Whether the last step landed on an edge: it was solved with the level after the jump, so what lies inside it is the
     * level before — held, not a ramp drawn between its ends.
     */
    private boolean  landedOnEdge;

    /**
     * @param interval the time between two samples: the longest step is one, until the caller allows more
     */
    TransientStepper(Netlist netlist, List<Device> devices, EventSimulation events, double interval) {
        this.netlist = netlist;
        this.system = new SparseSystem(netlist.size());
        this.events = events;
        this.devices = List.copyOf(devices);
        this.invariantDevices = this.devices.stream().filter(device -> !device.nonlinear() && device.invariant()).toList();
        this.linearDevices = this.devices.stream().filter(device -> !device.nonlinear() && !device.invariant()).toList();
        this.nonlinearDevices = this.devices.stream().filter(Device::nonlinear).toList();
        this.nonlinear = !nonlinearDevices.isEmpty();
        this.newton = new CircuitNewton(netlist, nonlinear, CircuitNewton.Settling.TRANSIENT);
        this.digital = events.active();
        this.signals = netlist.elements().stream().map(Netlist.Element::signal).filter(Objects::nonNull).toList();
        this.interval = interval;
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
     * Goes on from where {@code previous} is — the same time, node voltages, device memories and digital life —, the
     * integration restarting as at a switch edge.
     *
     * @throws IllegalArgumentException when the piece's parts are not the ones it had
     */
    void continueFrom(TransientStepper previous) {
        if (digital != previous.digital || state.length != previous.state.length
                || companion.length != previous.companion.length || !netlist.nodes().equals(previous.netlist.nodes())) {
            throw new IllegalArgumentException("The circuit changed its parts; it starts again.");
        }
        if (digital) {
            events.continueFrom(previous.events);
        }
        state = previous.state.clone();
        companion = previous.companion.clone();
        time = previous.time;
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

    boolean landedOnEdge() {
        return landedOnEdge;
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
        if (landedOnEdge && back > -earlierStep) {
            return earlier;
        }
        if (oldest != null && back >= -(earlierStep + oldestStep)) {
            return parabola(oldest, oldestStep, earlier, earlierStep, state, back);
        }
        return extrapolated(earlier, earlierStep, state, back);
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
                step = crossingUnresolved ? Math.max(crossingResolution / 2, share * attempt + crossingResolution / 2)
                        : solution == null ? attempt / UNSETTLED_CUT : attempt * stepFactor(error, order);
                if (step < minimumStep) {
                    throw new IllegalArgumentException("The circuit does not settle at t = " + time + " s.");
                }
                continue;
            }
            for (Device device : devices) {
                device.accept(assembly, solution);
            }
            companion = companionTrial;
            listener.accepted(solution, time + attempt, attempt, landsOnEdge);
            oldest = restart ? null : earlier;
            oldestStep = earlierStep;
            earlier = state;
            earlierStep = attempt;
            state = solution;
            time = landsOnStop ? stop : time + attempt;
            restart = landsOnEdge;
            landedOnEdge = landsOnEdge;
            if (digital && events.advance(state, time)) {
                restart = true;
            }
            step = restart ? Math.min(maximumStep, crossingResolution * 4) : Math.min(attempt * stepFactor(error, order), maximumStep);
            return attempt;
        }
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
    private double truncationRatio(double[] solution, double[] predicted) {
        double largest = 0;
        for (Device device : devices) {
            largest = Math.max(largest, device.truncation(solution, predicted));
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
