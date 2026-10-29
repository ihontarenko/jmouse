package org.jmouse.cad.simulation.engine;

import org.jmouse.numerics.SparseSystem;

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
 *   <li><strong>Transient</strong> — not here: the transient run, the one engine the bench, a computed graph and a
 *       datasheet check share.</li>
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
    private static final int    SOURCE_STEPS        = 20;
    /** The conductance gmin stepping starts every node with, and the last it withdraws. */
    private static final double LARGEST_SHUNT       = 1e-2;
    private static final double SMALLEST_SHUNT      = 1e-12;

    private final Netlist         netlist;
    /** The one linear system every Newton iteration of this solve assembles into. */
    private final SparseSystem    system;
    private final List<Device>    devices;
    private final CircuitNewton   newton;

    CircuitSolver(Netlist netlist) {
        this(netlist, Device.of(netlist, new EventSimulation(netlist)));
    }

    /** The circuit solved with these devices: its own, and what an event simulation running beside it adds. */
    CircuitSolver(Netlist netlist, List<Device> devices) {
        this(netlist, new SparseSystem(netlist.size()), List.copyOf(devices));
    }

    private CircuitSolver(Netlist netlist, SparseSystem system, List<Device> devices) {
        this.netlist = netlist;
        this.system = system;
        this.devices = devices;
        this.newton = new CircuitNewton(netlist, devices.stream().anyMatch(Device::nonlinear), CircuitNewton.Settling.OPERATING_POINT);
    }

    /**
     * The same circuit with one source held at a value — a sweep's next point: the same system and the same devices but
     * that source's, which is built again at the value. Nothing else is rebuilt; the points of a sweep are solved one
     * after another, never at once.
     */
    CircuitSolver withSourceValue(String id, double value) {
        Netlist changed = netlist.withValue(id, value);
        List<Device> swapped = new ArrayList<>(devices);
        for (int index = 0; index < changed.elements().size(); index++) {
            if (changed.elements().get(index).id().equals(id)) {
                swapped.set(index, Device.of(changed.elements().get(index), index));
            }
        }
        return new CircuitSolver(changed, system, List.copyOf(swapped));
    }

    List<Device> devices() {
        return devices;
    }

    boolean nonlinear() {
        return newton.nonlinear();
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
    List<NoiseCurrent> noiseSources(double[] operatingPoint) {
        return devices.stream().flatMap(device -> device.noise(operatingPoint).stream()).toList();
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
        Stamped stamped = new Stamped(shunt);
        double[] plain = newton.solve(assembly, start, NEWTON_ITERATIONS, false, stamped);
        return plain != null || !newton.nonlinear() ? plain : newton.solve(assembly, start, NEWTON_ITERATIONS, true, stamped);
    }

    /**
     * The circuit at a guess: every device stamped, and every node held to ground by {@code shunt} siemens while gmin is
     * stepped. Singular where Newton started, the circuit itself has no unique solution; singular later, at a guess
     * Newton wandered to, it is a run that did not settle — the line search may still find the answer.
     */
    private final class Stamped implements CircuitNewton.Linearization {

        private final double shunt;

        Stamped(double shunt) {
            this.shunt = shunt;
        }

        @Override
        public void assemble(Assembly assembly, double[] guess, int iteration) {
            assembly.begin(guess);
            for (Device device : devices) {
                device.stamp(assembly);
            }
            if (shunt > 0) {
                for (int node = 0; node < netlist.nodes().size(); node++) {
                    assembly.conductance(node, Netlist.GROUND, shunt);
                }
            }
        }

        @Override
        public void singularAtStart(Assembly assembly) {
            throw new IllegalArgumentException(UNSOLVABLE + unconstrained(assembly.unconstrained()));
        }
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
            return "the current of " + netlist.branches().get(index).name();
        }
        return netlist.states().get(index - netlist.branches().size());
    }
}
