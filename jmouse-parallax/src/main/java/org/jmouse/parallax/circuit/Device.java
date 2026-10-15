package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One element as the solver sees it: what it adds to the system. Every analysis — operating point, transient, small
 * signal, sweep — goes through the same stamps, so an element is described once and every analysis agrees about it.
 * A nonlinear device stamps its linearization at the current Newton guess.
 */
sealed interface Device permits Device.Resistor, Device.Capacitor, Device.Inductor, Device.VoltageSource,
        Device.CurrentSource, Device.Diode, ModelDevice, OutputBridge, NoiseInjection {

    /** Stamps the device into a DC or transient iteration. */
    void stamp(Assembly assembly);

    /** Stamps the device's small-signal admittance at the operating point. */
    void stampPhasor(PhasorAssembly assembly);

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

    /** Instants where the device's drive jumps; a transient lands exactly on them and restarts with backward Euler. */
    default void discontinuities(double duration, List<Double> into) {
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
     * noiseless — a source, a capacitor, an inductor.
     *
     * @param temperature the bench's temperature, kelvin
     */
    default List<NoiseCurrent> noise(double[] operatingPoint, double temperature) {
        return List.of();
    }

    /** How far its own heat has raised it above the bench, kelvin, in a solved state; NaN for a device that keeps none. */
    default double heating(double[] state) {
        return Double.NaN;
    }

    /** The part of the circuit this device is, as a reader names it — an element id —, or null for none of them. */
    default String part() {
        return null;
    }

    /**
     * The power the device takes from the circuit at a solved state, in watts: dissipated as heat by a resistor, a
     * diode or a model's currents; negative for a source that delivers it. A capacitor and an inductor only store it
     * and take none on average, so they answer 0.
     */
    default double power(double[] state, double time) {
        return 0;
    }

    /** How warm the part runs per watt, junction or body to ambient, in K/W; 0 for a part that is not warmed. */
    default double thermalResistance() {
        return 0;
    }

    /**
     * The current through a two-terminal device from its positive to its negative terminal at an accepted state, the
     * devices' memories ({@code companion}) as they were accepted with it; NaN for a device with more terminals.
     */
    default double current(double[] state, double[] companion, double time) {
        return Double.NaN;
    }

    /**
     * The current flowing into each of its terminals at an accepted state, by terminal name — "+" and "−" for a
     * two-terminal device, a model's pin names for a model —; what a schematic draws moving along the wires.
     */
    default Map<String, Double> terminalCurrents(double[] state, double[] companion, double time) {
        double through = current(state, companion, time);
        if (Double.isNaN(through)) {
            return Map.of();
        }
        // In terminal order, first terminal first: which pin is "the first" is what a part's own column reads.
        String[] names = terminalNames();
        Map<String, Double> currents = new LinkedHashMap<>();
        currents.put(names[0], through);
        currents.put(names[1], -through);
        return currents;
    }

    /**
     * Its states at an accepted state, by name — a model's continuous states and its numeric discrete ones —; what an
     * instrument written over them reads. None for a device with no states of its own.
     */
    default Map<String, Double> stateValues(double[] state) {
        return Map.of();
    }

    /** What a two-terminal device's terminals are called: its component's pins, or "+" and "−". */
    default String[] terminalNames() {
        return Netlist.Element.PRIMITIVE_TERMINALS;
    }

    /**
     * The two terminals {@link #current} flows into and out of, in that order — what {@link #terminalCurrents} names
     * for a two-terminal device.
     */
    default String[] currentTerminals() {
        return terminalNames();
    }

    static Device of(Netlist.Element element, int slot) {
        return switch (element.type()) {
            case RESISTOR -> new Resistor(element, null, null);
            case CAPACITOR -> new Capacitor(element, slot);
            case INDUCTOR -> new Inductor(element);
            case VOLTAGE_SOURCE -> new VoltageSource(element);
            case CURRENT_SOURCE -> new CurrentSource(element);
            case DIODE -> new Diode(element, slot, PnJunctionLimit.of(element.value(), Diode.THERMAL_VOLTAGE));
            case MODEL -> throw new IllegalStateException("A model element is bound by the netlist, not built as a device.");
        };
    }

    /**
     * A resistor; on a realistic bench, a resistor that heats itself: {@code thermal} is its component's warming and
     * temperature coefficient, {@code heating} how far its own power has raised it above the bench — moved after each
     * accepted step, which is exact for a time constant of seconds against steps of microseconds. Its value already
     * carries the bench's temperature; the heating moves it further. With no {@code thermal} it is the plain resistor,
     * stamped once for the whole run.
     */
    record Resistor(Netlist.Element element, PartThermal thermal, double[] heating) implements Device {

        /** A resistor that heats itself on a realistic bench. */
        static Resistor heated(Netlist.Element element, PartThermal thermal) {
            return new Resistor(element, thermal, new double[1]);
        }

        /** Its resistance now: its value, moved by its own heat. */
        double resistance() {
            return thermal == null ? element.value() : element.value() * (1 + thermal.coefficient() * heating[0]);
        }

        @Override
        public double heating(double[] state) {
            return thermal == null ? Double.NaN : heating[0];
        }

        @Override
        public void accept(Assembly assembly, double[] state) {
            if (thermal == null || !(thermal.resistance() > 0)) {
                return;
            }
            double voltage = across(state, element);
            double settled = voltage * voltage / resistance() * thermal.resistance();
            double time = thermal.timeConstant() > 0 ? thermal.timeConstant() : PartThermal.DEFAULT_TIME_CONSTANT;
            heating[0] = settled + (heating[0] - settled) * Math.exp(-assembly.step() / time);
        }

        @Override
        public String[] terminalNames() {
            return element.terminals();
        }

        /** A quarter-watt through-hole body in still air. */
        static final double THERMAL_RESISTANCE = 250;

        public void stamp(Assembly assembly) {
            assembly.conductance(element.positive(), element.negative(), 1 / resistance());
        }

        @Override
        public boolean invariant() {
            return thermal == null;
        }

        /** The current from its positive terminal to its negative one, by Ohm's law at a solution. */
        double current(double[] state) {
            return across(state, element) / resistance();
        }

        public String part() {
            return element.id();
        }

        public double current(double[] state, double[] companion, double time) {
            return current(state);
        }

        public double power(double[] state, double time) {
            double voltage = across(state, element);
            return voltage * voltage / resistance();
        }

        public double thermalResistance() {
            return THERMAL_RESISTANCE;
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.admittance(element.positive(), element.negative(), Complex.of(1 / resistance()));
        }

        /** Johnson–Nyquist noise: a current of 4kT/R, white — at its own temperature, where it heats itself. */
        @Override
        public List<NoiseCurrent> noise(double[] operatingPoint, double temperature) {
            double own = thermal == null ? temperature : temperature + heating[0];
            return List.of(new NoiseCurrent(element.positive(), element.negative(),
                    4 * NoiseCurrent.BOLTZMANN * own / resistance(), 0));
        }
    }

    /**
     * Open at DC. In a transient, its companion: i = g · v − history, where backward Euler has g = C/h and history
     * g · v(previous), and the trapezoidal rule g = 2C/h and history g · v(previous) + i(previous).
     */
    record Capacitor(Netlist.Element element, int slot, ConductanceSlots slots) implements Device {

        Capacitor(Netlist.Element element, int slot) {
            this(element, slot, new ConductanceSlots());
        }

        @Override
        public String[] terminalNames() {
            return element.terminals();
        }

        public String part() {
            return element.id();
        }

        /** Its companion is the current it carried at the accepted point. */
        public double current(double[] state, double[] companion, double time) {
            return companion[slot];
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
            double across = voltage(state, element.positive()) - voltage(state, element.negative());
            assembly.rememberCompanion(slot, conductance * across - history(assembly, conductance));
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.admittance(element.positive(), element.negative(), new Complex(0, assembly.angular() * element.value()));
        }

        public double truncation(double[] solution, double[] predicted) {
            double across = voltage(solution, element.positive()) - voltage(solution, element.negative());
            double expected = voltage(predicted, element.positive()) - voltage(predicted, element.negative());
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

    /**
     * A branch of its own. Shorted at DC. In a transient: V − r · I = −r · I(previous) for backward Euler with
     * r = L/h, and V − r · I = −r · I(previous) − V(previous) for the trapezoidal rule with r = 2L/h.
     */
    record Inductor(Netlist.Element element) implements Device {
        @Override
        public String[] terminalNames() {
            return element.terminals();
        }

        public void stamp(Assembly assembly) {
            assembly.branch(element.positive(), element.negative(), element.branch());
            if (!assembly.isTransient()) {
                return;
            }
            boolean trapezoidal = assembly.integration() == Assembly.Integration.TRAPEZOIDAL;
            double reactance = (trapezoidal ? 2 : 1) * element.value() / assembly.step();
            assembly.branchCoefficient(element.branch(), -reactance);
            double right = -reactance * assembly.previousValue(element.branch());
            if (trapezoidal) {
                right -= assembly.previousVoltage(element.positive()) - assembly.previousVoltage(element.negative());
            }
            assembly.branchRightSide(element.branch(), right);
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.branch(element.positive(), element.negative(), element.branch());
            assembly.branchCoefficient(element.branch(), new Complex(0, -assembly.angular() * element.value()));
        }

        public String part() {
            return element.id();
        }

        /**
         * Its branch current, from its positive terminal through it to its negative one. Without it an inductor showed no
         * current on a sheet and was missing from a computed transient's columns.
         */
        public double current(double[] state, double[] companion, double time) {
            return state[element.branch()];
        }

        /** What it takes in — stored in its field, given back when the current falls. */
        public double power(double[] state, double time) {
            return across(state, element) * state[element.branch()];
        }

        public double truncation(double[] solution, double[] predicted) {
            double current = solution[element.branch()];
            return Truncation.share(element.value() * (current - predicted[element.branch()]), element.value() * current, Truncation.CHARGE);
        }
    }

    record VoltageSource(Netlist.Element element) implements Device {
        @Override
        public String[] terminalNames() {
            return element.terminals();
        }

        public void stamp(Assembly assembly) {
            assembly.branch(element.positive(), element.negative(), element.branch());
            assembly.branchRightSide(element.branch(), drive(element, assembly));
        }

        public String part() {
            return element.id();
        }

        /** Its branch current flows into the positive terminal and through the source. */
        public double current(double[] state, double[] companion, double time) {
            return state[element.branch()];
        }

        public String[] currentTerminals() {
            return element.signal() == null ? terminalNames() : element.signal().pinNames();
        }

        /** A generator's current goes into its own pins, by name; a primitive source's into "+" and "−". */
        public Map<String, Double> terminalCurrents(double[] state, double[] companion, double time) {
            return element.signal() == null ? Device.super.terminalCurrents(state, companion, time)
                    : element.signal().terminalCurrents(state[element.branch()]);
        }

        public double power(double[] state, double time) {
            return across(state, element) * state[element.branch()];
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.branch(element.positive(), element.negative(), element.branch());
            assembly.branchRightSide(element.branch(), element.value());
        }

        public void discontinuities(double duration, List<Double> into) {
            sourceDiscontinuities(element, duration, into);
        }

        public double maximumStep() {
            return sourceMaximumStep(element);
        }
    }

    /** Pushes its value out of its positive terminal into the circuit. */
    record CurrentSource(Netlist.Element element) implements Device {
        @Override
        public String[] terminalNames() {
            return element.terminals();
        }

        public void stamp(Assembly assembly) {
            assembly.current(element.positive(), element.negative(), -drive(element, assembly));
        }

        public String part() {
            return element.id();
        }

        /** It pushes its value out of its positive terminal: through it, from positive to negative, flows minus that. */
        public double current(double[] state, double[] companion, double time) {
            return -element.valueAt(time);
        }

        public double power(double[] state, double time) {
            return across(state, element) * current(state, null, time);
        }

        public void stampPhasor(PhasorAssembly assembly) {
            assembly.sourceCurrent(element.positive(), element.negative(), element.value());
        }

        public void discontinuities(double duration, List<Double> into) {
            sourceDiscontinuities(element, duration, into);
        }

        public double maximumStep() {
            return sourceMaximumStep(element);
        }
    }

    /**
     * The Shockley diode, anode at the positive terminal: i = Is (e^(v / Vt) − 1), Is being the element's value, at
     * 300 K with an emission coefficient of 1. Stamped as its tangent at the guess — a conductance and the current
     * that makes the tangent pass through the curve. The junction voltage is limited between iterations by
     * {@code junctionLimit} (a {@link PnJunctionLimit}), so a step that would put a volt across a junction cannot ask
     * for an astronomical current; and a
     * tiny conductance in parallel keeps a reverse-biased diode from leaving a node floating.
     */
    record Diode(Netlist.Element element, int slot, VoltageLimit junctionLimit) implements Device {

        static final double THERMAL_VOLTAGE = 8.617333e-5 * 300.15;
        static final double MINIMUM_CONDUCTANCE = 1e-12;
        /** The largest v / Vt the exponential is taken at: e⁸⁰ is finite, and no real junction gets there. */
        private static final double LARGEST_EXPONENT = 80;
        /** A small-signal glass diode, DO-35, in still air. */
        static final double THERMAL_RESISTANCE = 350;

        @Override
        public String[] terminalNames() {
            return element.terminals();
        }

        public boolean nonlinear() {
            return true;
        }

        public String part() {
            return element.id();
        }

        public double current(double[] state, double[] companion, double time) {
            return current(state);
        }

        public double power(double[] state, double time) {
            return across(state, element) * current(state, null, time);
        }

        public double thermalResistance() {
            return THERMAL_RESISTANCE;
        }

        public void stamp(Assembly assembly) {
            double saturation = element.value();
            double wanted = assembly.voltage(element.positive()) - assembly.voltage(element.negative());
            double voltage = junctionLimit.limited(wanted, assembly.junction(slot));
            assembly.rememberJunction(slot, voltage);
            double exponential = exponential(voltage);
            double current = saturation * (exponential - 1);
            double conductance = saturation * exponential / THERMAL_VOLTAGE + MINIMUM_CONDUCTANCE;
            assembly.conductance(element.positive(), element.negative(), conductance);
            assembly.current(element.positive(), element.negative(), current - (conductance - MINIMUM_CONDUCTANCE) * voltage);
        }

        public void stampPhasor(PhasorAssembly assembly) {
            double voltage = assembly.operatingVoltage(element.positive()) - assembly.operatingVoltage(element.negative());
            double conductance = element.value() * exponential(voltage) / THERMAL_VOLTAGE + MINIMUM_CONDUCTANCE;
            assembly.admittance(element.positive(), element.negative(), Complex.of(conductance));
        }

        /** The current at a solved state, for the datasets. */
        double current(double[] state) {
            return element.value() * (exponential(across(state, element)) - 1);
        }

        /** e^(v / Vt), held below overflow. */
        private static double exponential(double voltage) {
            return Math.exp(Math.min(voltage / THERMAL_VOLTAGE, LARGEST_EXPONENT));
        }
    }

    private static double drive(Netlist.Element element, Assembly assembly) {
        return assembly.isTransient() ? element.valueAt(assembly.time()) : element.steadyValue() * assembly.sourceScale();
    }

    private static void sourceDiscontinuities(Netlist.Element element, double duration, List<Double> into) {
        if (element.signal() != null) {
            // A generator's edges, found in its own expression: scanned in 65 536 spans — a pulse that starts and ends
            // inside one span is the one it can miss.
            into.addAll(element.signal().edges(duration, duration / 65536, duration * 1e-12));
        }
    }

    private static double sourceMaximumStep(Netlist.Element element) {
        if (element.signal() != null) {
            return element.signal().maximumStep();
        }
        return Double.POSITIVE_INFINITY;
    }

    private static double voltage(double[] state, int node) {
        return node == Netlist.GROUND ? 0 : state[node];
    }

    /** The voltage across a two-terminal element, positive minus negative. */
    private static double across(double[] state, Netlist.Element element) {
        return voltage(state, element.positive()) - voltage(state, element.negative());
    }
}
