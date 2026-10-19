package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * One element as the solver sees it: what it adds to the system. Every analysis — operating point, transient, small
 * signal, sweep — goes through the same stamps, so an element is described once and every analysis agrees about it.
 * A nonlinear device stamps its linearization at the current Newton guess.
 *
 * <p>Besides what it stamps, a device that is a part of the circuit ({@link #part}) reports what it does: the power it
 * takes, the current into each of its terminals, its states. The solver's own devices — a node's leak, a failed part's
 * stand-in, a logic pin's capacitance — report nothing: they are numerics, not parts.</p>
 */
sealed interface Device permits Device.Resistor, Device.Capacitor, Device.VoltageSource,
        Device.CurrentSource, ModelDevice, OutputBridge, NoiseInjection {

    /** Stamps the device into a DC or transient iteration. */
    void stamp(Assembly assembly);

    /** Stamps the device's small-signal admittance at the operating point. */
    void stampPhasor(PhasorAssembly assembly);

    /**
     * The phasor of the current through a two-terminal device, positive to negative, in a small-signal solution at
     * {@code angular} rad/s about {@code operatingPoint}; null for a device that does not say — what a phasor diagram
     * draws, from the same admittance the device stamped.
     */
    default Complex phasorCurrent(Complex[] solution, double angular, double[] operatingPoint) {
        return null;
    }

    /** The phasor across a two-terminal device, first terminal less second; null for a device that does not say. */
    default Complex phasorVoltage(Complex[] solution) {
        return null;
    }

    /** Whether it drives the circuit — a source, a generator —, rather than being driven by it. */
    default boolean source() {
        return false;
    }

    /** Writes the device's own unknowns' starting values into the state a transient starts from. */
    default void initialize(double[] state) {
    }

    /** Takes its history from the state a transient starts from. */
    default void begin(double[] state) {
    }

    /** Called once a transient point is accepted, so a device can carry what it needs into the next step. */
    default void accept(Assembly assembly, double[] state) {
    }

    default boolean nonlinear() {
        return false;
    }

    /**
     * Whether its stamp reads nothing that changes during a run — not the guess, the time, the step nor its history:
     * a resistor. Such a device is stamped once for the whole run, as SPICE loads its constant matrix once.
     */
    default boolean invariant() {
        return false;
    }

    /**
     * How much of its allowed local error a transient step used, from the step's solution and the prediction it is
     * measured against (see {@link Truncation}); 0 for a device that stores nothing.
     */
    default double truncation(double[] solution, double[] predicted) {
        return 0;
    }

    /** The longest step that still resolves the device's drive. */
    default double maximumStep() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * The random currents the device adds at the operating point, for a noise analysis; none for a device that is
     * noiseless — a source, a capacitor. A model reads its own temperature in its own densities.
     */
    default List<NoiseCurrent> noise(double[] operatingPoint) {
        return List.of();
    }

    /**
     * Its own heat, as its component's {@code heat} law writes it ({@link PartHeat}); null for a device that does not
     * warm. The engine advances it after every accepted step from {@link #power}; the device reads the temperature it
     * makes where its bench is realistic.
     */
    default PartHeat heat() {
        return null;
    }

    /** The part of the circuit this device is, as a reader names it — an element id —, or null for none of them. */
    default String part() {
        return null;
    }

    /**
     * The power the device takes from the circuit at a solved state, in watts: dissipated as heat by a model's currents;
     * negative for a source that delivers it. A capacitance only stores it and takes none, so it answers 0.
     */
    default double power(double[] state, double time) {
        return 0;
    }

    /**
     * The terminals it reports a current into, in the order {@link #terminalCurrents(double[], double[], double, double[])}
     * writes them: a model's analog pins, a generator's own pins, or "+" and "−"; none for a device that is no part.
     */
    default String[] reportedTerminals() {
        return new String[0];
    }

    /**
     * The current into each of its {@link #reportedTerminals} at an accepted state — the devices' memories
     * ({@code companion}) as they were accepted with it —, written into {@code into} in that order: what a schematic
     * draws moving along the wires. Written into an array, as a trace reads it on every step.
     */
    default void terminalCurrents(double[] state, double[] companion, double time, double[] into) {
    }

    /** The same by terminal name, in terminal order. */
    default Map<String, Double> terminalCurrents(double[] state, double[] companion, double time) {
        String[] terminals = reportedTerminals();
        if (terminals.length == 0) {
            return Map.of();
        }
        double[] currents = new double[terminals.length];
        terminalCurrents(state, companion, time, currents);
        Map<String, Double> byTerminal = new LinkedHashMap<>();
        for (int terminal = 0; terminal < terminals.length; terminal++) {
            byTerminal.merge(terminals[terminal], currents[terminal], Double::sum);
        }
        return byTerminal;
    }

    /**
     * Its states at an accepted state, by name — a model's continuous states and its numeric discrete ones —; what an
     * instrument written over them reads. None for a device with no states of its own.
     */
    default Map<String, Double> stateValues(double[] state) {
        return Map.of();
    }

    /**
     * Every device a netlist is solved with, in the one order every solver builds them: its elements, each by
     * {@code primitive} (given its slot), its models' units, its digital pins' capacitances, its digital outputs. A
     * solver that builds them otherwise would stamp another circuit.
     */
    static List<Device> of(Netlist netlist, EventSimulation events, BiFunction<Netlist.Element, Integer, Device> primitive) {
        List<Device> built = new ArrayList<>();
        for (Netlist.Element element : netlist.elements()) {
            built.add(primitive.apply(element, built.size()));
        }
        for (Netlist.ModelUnit unit : netlist.units()) {
            built.add(new ModelDevice(unit, events.discrete(unit.label())));
        }
        for (Netlist.Element capacitor : events.pinCapacitances()) {
            built.add(new Capacitor(capacitor, built.size()));
        }
        built.addAll(events.bridges());
        return built;
    }

    /**
     * The engine's own device for an element it supplies itself: a conductance (a node's leak, a failed part's
     * stand-in), a logic pin's capacitance, an ideal source a generator's equation drives. Every part written on a sheet
     * is its document's model ({@link ModelDevice}), never one of these.
     */
    static Device of(Netlist.Element element, int slot) {
        return switch (element.kind()) {
            case RESISTOR -> new Resistor(element);
            case CAPACITOR -> new Capacitor(element, slot);
            case VOLTAGE_SOURCE -> new VoltageSource(element);
            case CURRENT_SOURCE -> new CurrentSource(element);
        };
    }

    /**
     * A conductance the solver adds of itself: a node's leak to ground (SPICE's gmin), a failed part's stand-in. It has
     * no tolerance, no drift, no heat and no noise — it is numerics, not a part —, and is stamped once for the whole run.
     */
    record Resistor(Netlist.Element element) implements Device {

        public void stamp(Assembly assembly) {
            assembly.conductance(element.positive(), element.negative(), 1 / element.value());
        }

        @Override
        public boolean invariant() {
            return true;
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.admittance(element.positive(), element.negative(), Complex.of(1 / element.value()));
        }

        @Override
        public Complex phasorVoltage(Complex[] solution) {
            return phasorAcross(solution, element);
        }

        @Override
        public Complex phasorCurrent(Complex[] solution, double angular, double[] operatingPoint) {
            return phasorAcross(solution, element).divide(Complex.of(element.value()));
        }
    }

    /**
     * A logic pin's capacitance. Open at DC. In a transient, its companion: i = g · v − history, where backward Euler has
     * g = C/h and history g · v(previous), and the trapezoidal rule g = 2C/h and history g · v(previous) + i(previous).
     */
    record Capacitor(Netlist.Element element, int slot, ConductanceSlots slots) implements Device {

        Capacitor(Netlist.Element element, int slot) {
            this(element, slot, new ConductanceSlots());
        }

        public void stamp(Assembly assembly) {
            if (!assembly.isTransient()) {
                return;
            }
            double conductance = conductance(assembly);
            assembly.conductance(slots, element.positive(), element.negative(), conductance);
            assembly.current(element.positive(), element.negative(), -history(assembly, conductance));
        }

        public void accept(Assembly assembly, double[] state) {
            double conductance = conductance(assembly);
            assembly.rememberCompanion(slot, conductance * across(state, element) - history(assembly, conductance));
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.admittance(element.positive(), element.negative(), new Complex(0, assembly.angular() * element.value()));
        }

        @Override
        public Complex phasorVoltage(Complex[] solution) {
            return phasorAcross(solution, element);
        }

        @Override
        public Complex phasorCurrent(Complex[] solution, double angular, double[] operatingPoint) {
            return phasorAcross(solution, element).multiply(new Complex(0, angular * element.value()));
        }

        public double truncation(double[] solution, double[] predicted) {
            double across = across(solution, element);
            double expected = across(predicted, element);
            return Truncation.share(element.value() * (across - expected), element.value() * across, Truncation.CHARGE);
        }

        private double conductance(Assembly assembly) {
            double conductance = element.value() / assembly.step();
            return assembly.integration() == Assembly.Integration.TRAPEZOIDAL ? 2 * conductance : conductance;
        }

        private double history(Assembly assembly, double conductance) {
            double previous = assembly.previousVoltage(element.positive()) - assembly.previousVoltage(element.negative());
            double history = conductance * previous;
            return assembly.integration() == Assembly.Integration.TRAPEZOIDAL ? history + assembly.companion(slot) : history;
        }
    }

    /** An ideal voltage source: a generator, whose voltage its document writes as a function of time ({@link SourceSignal}). */
    record VoltageSource(Netlist.Element element) implements Device {

        @Override
        public boolean source() {
            return true;
        }

        public void stamp(Assembly assembly) {
            assembly.branch(element.positive(), element.negative(), element.branch());
            assembly.branchRightSide(element.branch(), drive(element, assembly));
        }

        public String part() {
            return element.part();
        }

        /** A generator's current goes into its own pins, by name; a bare source's into "+" and "−". */
        @Override
        public String[] reportedTerminals() {
            return element.signal() == null ? element.terminals() : element.signal().pinNames();
        }

        /** Its branch current flows into the positive terminal and through the source. */
        @Override
        public void terminalCurrents(double[] state, double[] companion, double time, double[] into) {
            through(state[element.branch()], into);
        }

        public double power(double[] state, double time) {
            return across(state, element) * state[element.branch()];
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.branch(element.positive(), element.negative(), element.branch());
            assembly.branchRightSide(element.branch(), element.value());
        }

        @Override
        public Complex phasorVoltage(Complex[] solution) {
            return phasorAcross(solution, element);
        }

        @Override
        public Complex phasorCurrent(Complex[] solution, double angular, double[] operatingPoint) {
            return solution[element.branch()];
        }

        public double maximumStep() {
            return sourceMaximumStep(element);
        }
    }

    /** An ideal current source: pushes its value out of its positive terminal into the circuit. */
    record CurrentSource(Netlist.Element element) implements Device {

        @Override
        public boolean source() {
            return true;
        }

        public void stamp(Assembly assembly) {
            assembly.current(element.positive(), element.negative(), -drive(element, assembly));
        }

        public String part() {
            return element.part();
        }

        @Override
        public String[] reportedTerminals() {
            return element.signal() == null ? element.terminals() : element.signal().pinNames();
        }

        /** It pushes its value out of its positive terminal: through it, from positive to negative, flows minus that. */
        @Override
        public void terminalCurrents(double[] state, double[] companion, double time, double[] into) {
            through(-element.valueAt(time), into);
        }

        public double power(double[] state, double time) {
            return across(state, element) * -element.valueAt(time);
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.sourceCurrent(element.positive(), element.negative(), element.value());
        }

        @Override
        public Complex phasorVoltage(Complex[] solution) {
            return phasorAcross(solution, element);
        }

        @Override
        public Complex phasorCurrent(Complex[] solution, double angular, double[] operatingPoint) {
            return Complex.of(-element.value());
        }

        public double maximumStep() {
            return sourceMaximumStep(element);
        }
    }

    /** A two-terminal device's current, positive to negative: into its first terminal, out of its second. */
    private static void through(double current, double[] into) {
        into[0] = current;
        into[1] = -current;
    }

    private static double drive(Netlist.Element element, Assembly assembly) {
        return assembly.isTransient() ? element.valueAt(assembly.time()) : element.steadyValue() * assembly.sourceScale();
    }

    private static double sourceMaximumStep(Netlist.Element element) {
        if (element.signal() != null) {
            return element.signal().maximumStep();
        }
        return Double.POSITIVE_INFINITY;
    }

    /** The phasor across a two-terminal element, positive less negative. */
    private static Complex phasorAcross(Complex[] solution, Netlist.Element element) {
        return phasorAt(solution, element.positive()).subtract(phasorAt(solution, element.negative()));
    }

    /** A node's phasor; ground's is nought. */
    static Complex phasorAt(Complex[] solution, int node) {
        return node == Netlist.GROUND ? Complex.of(0) : solution[node];
    }

    private static double voltage(double[] state, int node) {
        return node == Netlist.GROUND ? 0 : state[node];
    }

    /** The voltage across a two-terminal element, positive minus negative. */
    private static double across(double[] state, Netlist.Element element) {
        return voltage(state, element.positive()) - voltage(state, element.negative());
    }
}
