package org.jmouse.parallax.circuit;

import org.jmouse.parallax.numerics.SparseSystem;

import org.jmouse.formula.Complex;

import java.util.ArrayList;
import java.util.List;

/**
 * Every analysis of a {@link Netlist}, through one path: devices stamp themselves into a modified-nodal system, and
 * Newton–Raphson solves it — once, for a linear circuit; until it settles, for a nonlinear one.
 *
 * <ul>
 *   <li><strong>Operating point</strong> — capacitors open, inductors shorted. When Newton does not settle from rest,
 *       the sources are raised to their value in steps, each solution starting the next.</li>
 *   <li><strong>Transient</strong> — not here: {@link TransientRun}, by the live engine ({@link LiveIntegrator}), the one
 *       engine the bench, a computed graph and a datasheet check share.</li>
 *   <li><strong>Small signal</strong> — every device linearized at the operating point.</li>
 * </ul>
 *
 * <p>⚠️ A refusal says where: a circuit that does not settle is refused with the time it failed at, never answered
 * with whatever the last iteration held.</p>
 */
final class CircuitSolver {

    private static final String UNSOLVABLE =
            "The circuit has no unique solution: a node that nothing ties to node 0, or a loop of voltage sources.";

    private static final int    NEWTON_ITERATIONS   = 300;
    /** How short the line search lets a Newton step become before it takes it anyway. */
    private static final double SMALLEST_FRACTION   = 1.0 / 1024;
    private static final double VOLTAGE_TOLERANCE   = 1e-6;
    private static final int    SOURCE_STEPS        = 20;
    /** The conductance gmin stepping starts every node with, and the last it withdraws. */
    private static final double LARGEST_SHUNT       = 1e-2;
    private static final double SMALLEST_SHUNT      = 1e-12;

    private final Netlist         netlist;
    /** The one linear system every Newton iteration of this solve assembles into. */
    private final SparseSystem    system;
    private final List<Device>    devices;
    private final boolean         nonlinear;
    private final EventSimulation events;

    CircuitSolver(Netlist netlist) {
        this.netlist = netlist;
        this.system = new SparseSystem(netlist.size());
        this.events = new EventSimulation(netlist);
        List<Device> built = new ArrayList<>();
        for (Netlist.Element element : netlist.elements()) {
            built.add(Device.of(element, built.size()));
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
        this.devices = List.copyOf(built);
        this.nonlinear = devices.stream().anyMatch(Device::nonlinear) || !events.bridges().isEmpty();
    }

    List<Device> devices() {
        return devices;
    }

    boolean nonlinear() {
        return nonlinear;
    }

    double[] operatingPoint() {
        return operatingPoint(new double[netlist.size()]);
    }

    /** The operating point, Newton starting from {@code start} — a neighbouring solution, in a sweep. */
    double[] operatingPoint(double[] start) {
        double[] direct = newton(Assembly.operatingPoint(system, devices.size(), 1), start);
        if (direct != null) {
            return direct;
        }
        double[] raised = sourcesRaised();
        if (raised != null) {
            return raised;
        }
        double[] shunted = shuntsWithdrawn();
        if (shunted == null) {
            throw new IllegalArgumentException("The operating point does not settle, even with the sources raised gradually"
                    + " or every node first held to ground.");
        }
        return shunted;
    }

    /** SPICE's source stepping: every source raised from zero in steps, each solved from the one before; null if one is not. */
    private double[] sourcesRaised() {
        double[] state = new double[netlist.size()];
        for (int step = 1; step <= SOURCE_STEPS && state != null; step++) {
            state = newton(Assembly.operatingPoint(system, devices.size(), (double) step / SOURCE_STEPS), state, 0);
        }
        return state;
    }

    /**
     * SPICE's gmin stepping: every node held to ground by a conductance that is withdrawn a decade at a time, each
     * stage solved from the one before. A large one makes every node an easy answer; a junction it then lets go reaches
     * its knee in steps instead of in one Newton jump — a Zener fed by a current source jumped from 0 to 10 V, five
     * volts past its breakdown, and back, and never settled. Null if a stage does not settle.
     */
    private double[] shuntsWithdrawn() {
        double[] state = new double[netlist.size()];
        for (double shunt = LARGEST_SHUNT; shunt >= SMALLEST_SHUNT && state != null; shunt /= 10) {
            state = newton(Assembly.operatingPoint(system, devices.size(), 1), state, shunt);
        }
        return state == null ? null : newton(Assembly.operatingPoint(system, devices.size(), 1), state, 0);
    }

    /** The small-signal response at one frequency, every device linearized at {@code operatingPoint}. */
    Complex[] phasors(double frequency, double[] operatingPoint) {
        return smallSignal(frequency, operatingPoint).solve();
    }

    /** Every device's noise current at the operating point. */
    List<NoiseCurrent> noiseSources(double[] operatingPoint, double temperature) {
        return devices.stream().flatMap(device -> device.noise(operatingPoint, temperature).stream()).toList();
    }

    /**
     * Each node's noise density at one frequency, V²/Hz: every source driven alone through the small-signal circuit,
     * the power it reaches the node with added — the sources are uncorrelated.
     */
    double[] noiseDensities(double frequency, double[] operatingPoint, List<NoiseCurrent> sources) {
        PhasorAssembly assembly = smallSignal(frequency, operatingPoint);
        double[] densities = new double[netlist.nodes().size()];
        for (NoiseCurrent source : sources) {
            assembly.clearInjections();
            assembly.sourceCurrent(source.positive(), source.negative(), 1);
            Complex[] response = assembly.solve();
            for (int node = 0; node < densities.length; node++) {
                double transfer = response[node].modulus();
                densities[node] += transfer * transfer * source.densityAt(frequency);
            }
        }
        return densities;
    }

    private PhasorAssembly smallSignal(double frequency, double[] operatingPoint) {
        PhasorAssembly assembly = new PhasorAssembly(netlist.size(), 2 * Math.PI * frequency, operatingPoint);
        for (Device device : devices) {
            device.stampPhasor(assembly);
        }
        return assembly;
    }

    /**
     * Newton–Raphson from {@code start}; null when it does not settle. A linear circuit is solved in one pass. Plain
     * Newton first, as every circuit always was; only where it does not settle, again with a line search.
     */
    private double[] newton(Assembly assembly, double[] start) {
        return newton(assembly, start, 0);
    }

    /** The same, every node held to ground by {@code shunt} siemens as well. */
    private double[] newton(Assembly assembly, double[] start, double shunt) {
        double[] plain = newton(assembly, start, false, shunt);
        return plain != null || !nonlinear ? plain : newton(assembly, start, true, shunt);
    }

    /**
     * One Newton–Raphson run. With {@code searching}, a line search: a step is kept only where the equations hold better
     * than where it started; otherwise it is halved, back towards the point it left. A saturating model — an op-amp's
     * slew, s / √(1 + s²) — sends a full Newton step from s to −s³ once |s| > 1, and plain Newton bounced further out
     * every iteration. Not the first resort: where a diode is off, its rows are a few picosiemens, and the residual they
     * give is too rough a measure for a circuit plain Newton solves at once.
     */
    private double[] newton(Assembly assembly, double[] start, boolean searching, double shunt) {
        if (netlist.size() == 0) {
            return new double[0];
        }
        double[] guess = start;
        double[] accepted = null;
        double[] direction = null;
        double acceptedResidual = Double.POSITIVE_INFINITY;
        double fraction = 1;
        for (int iteration = 0; iteration < NEWTON_ITERATIONS; iteration++) {
            assembly.begin(guess);
            for (Device device : devices) {
                device.stamp(assembly);
            }
            if (shunt > 0) {
                for (int node = 0; node < netlist.nodes().size(); node++) {
                    assembly.conductance(node, Netlist.GROUND, shunt);
                }
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
                // Singular where Newton started: the circuit itself has no unique solution. Singular later, at a guess
                // Newton wandered to, it is a run that did not settle — the line search may still find the answer.
                if (iteration == 0) {
                    throw new IllegalArgumentException(UNSOLVABLE + unconstrained(assembly.unconstrained()));
                }
                return null;
            }
            if (!nonlinear) {
                return next;
            }
            if (iteration > 0 && settled(guess, next)) {
                return next;
            }
            for (double value : next) {
                if (!Double.isFinite(value)) {
                    return null;
                }
            }
            double[] stepped = damped(guess, next);
            if (searching) {
                if (direction == null) {
                    direction = new double[stepped.length];
                }
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

    /** Names an unknown whose row or column is empty — the usual reason a system has no unique solution. */
    private String unconstrained(int index) {
        return index < 0 ? "" : " Nothing determines '" + unknownName(index) + "'.";
    }

    private String unknownName(int index) {
        if (index < netlist.nodes().size()) {
            return "the voltage of " + netlist.nodes().get(index);
        }
        index -= netlist.nodes().size();
        if (index < netlist.branches().size()) {
            return "the current of " + netlist.branches().get(index);
        }
        return netlist.states().get(index - netlist.branches().size());
    }

    private boolean settled(double[] guess, double[] next) {
        for (int index = 0; index < next.length; index++) {
            double tolerance = VOLTAGE_TOLERANCE * (1 + Math.abs(next[index]));
            if (Math.abs(next[index] - guess[index]) > tolerance) {
                return false;
            }
        }
        return true;
    }
}
