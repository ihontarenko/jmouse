package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.State;
import org.jmouse.cad.simulation.EngineName;
import org.jmouse.cad.simulation.ModelLanguage;
import org.jmouse.cad.simulation.PinQuantities;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.elaboration.ElaboratedPart;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;

import org.jmouse.formula.Formula;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * One expression a component writes over itself, compiled once for one part of a circuit and read at any moment: each
 * pin's name is its voltage, {@code current(pin)} the current into that pin, a state's name its value, a parameter's
 * name its value — the part's overrides and the bench's {@code temperature} and {@code realistic} included —, and
 * {@code power} and {@code time} what they say. What an instrument reads is one of these; nothing here knows any
 * instrument, so a meter nobody has thought of yet is written in its own document.
 */
final class PartExpression {

    private final String part;
    private final RealFunction function;
    /** The node each pin's voltage is read from, in the order of the pins; ground for a pin wired to nothing. */
    private final List<String> pinNodes;
    private final List<String> pins;
    private final List<String> states;
    private final double[] parameters;
    /** Whether it reads only what changes in steps — discrete states, parameters —, so it is a value at a moment. */
    private final boolean moment;

    private PartExpression(String part, RealFunction function, List<String> pinNodes, List<String> pins, List<String> states,
                           double[] parameters, boolean moment) {
        this.part = part;
        this.moment = moment;
        this.function = function;
        this.pinNodes = List.copyOf(pinNodes);
        this.pins = List.copyOf(pins);
        this.states = List.copyOf(states);
        this.parameters = parameters;
    }

    /**
     * {@code expression}, as the part's component writes it, for the part of {@code circuit} as it stands on its bench.
     *
     * @throws IllegalArgumentException when it does not read — naming the component and what it wrote
     */
    static PartExpression of(ElaboratedCircuit circuit, ElaboratedPart part, String expression) {
        SimulatedComponent model = part.component();
        List<String> pins = model.pins().stream().map(Pin::name).toList();
        List<String> pinNodes = model.pins().stream().map(pin -> {
            String key = wiredKey(part, pin);
            return key == null ? Circuit.GROUND : part.connections().get(key);
        }).toList();
        Map<String, Double> parameters = circuit.parameters(part, "");
        try {
            // The component's own definitions — its libraries', the constants among them — read here as in its equations.
            List<Formula.Definition> library = ModelLanguage.definitions(model.title(), model.formulas());
            Formula read = ModelLanguage.readings(model.title(), expression, library);
            // What the expression reads, the definitions it calls included: a pin's voltage and current name the pin.
            Set<String> named = read.variables();
            // A row state is read slot by slot — `ddram_5` —; a display cannot take a whole row. Only the slots the
            // expression names are bound: a 256-byte memory would otherwise be 256 arguments of every figure it shows.
            List<String> states = model.states().stream().flatMap(state -> state.index() != null
                    ? state.index().names(state.name()).stream().filter(named::contains)
                    : Stream.of(state.name())).toList();
            // The arguments: each pin's current in a slot of its own, by the pin's position; then each pin's voltage by
            // its name, the states, the power, the time, whether it failed and the parameters.
            List<String> names = new ArrayList<>(pins);
            names.addAll(states);
            names.add(EngineName.POWER.written());
            names.add(EngineName.TIME.written());
            names.add(EngineName.FAILED.written());
            names.addAll(parameters.keySet());
            Formula members = PinQuantities.bind(read, pins, written -> model.title() + ": '" + written + "'");
            Formula bound = PinQuantities.bindCurrents(members, pins, pin -> new Formula.Slot(pins.indexOf(pin), pin + ".current"));
            RealFunction function = RealCompiler.compile(bound, Symbols.withSlots(pins.size(), names, List.of()), expression);
            boolean moment = !PinQuantities.readsCurrent(members) && readsOnlySteps(model, named, pins);
            return new PartExpression(part.reference(), function, pinNodes, pins, states,
                    parameters.values().stream().mapToDouble(Double::doubleValue).toArray(), moment);
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException(model.title() + ": '" + expression + "' does not read: " + unreadable.getMessage(), unreadable);
        }
    }

    /**
     * How a part wires one of its component's pins, as the key of its pin map: the pin's own name, or — on a part of
     * several blocks, a pin each block has — the drawn block's, `A.output`. The drawn block is the first one wired: a
     * dual comparator is drawn as one symbol, and its figures read that block, not a name no block is wired by. Null for
     * a pin the part leaves unwired.
     */
    static String wiredKey(ElaboratedPart part, Pin pin) {
        SimulatedComponent model = part.component();
        Map<String, String> wired = part.connections();
        if (wired.containsKey(pin.name()) || model.shared(pin) || model.units().isEmpty()) {
            return wired.containsKey(pin.name()) ? pin.name() : null;
        }
        for (String unit : model.units()) {
            String key = unit + ElaboratedCircuit.UNIT_SEPARATOR + pin.name();
            boolean drawn = wired.keySet().stream().anyMatch(wiredKey -> wiredKey.startsWith(unit + ElaboratedCircuit.UNIT_SEPARATOR));
            if (drawn) {
                return wired.containsKey(key) ? key : null;
            }
        }
        return null;
    }

    /**
     * Whether an expression reads nothing that moves continuously: no pin's voltage or current, no power, no time, no
     * continuous state — only discrete states, their slots and parameters. Such a figure — a character, a count — is a
     * value at a moment: averaged with another, a space and an 'l' make a 'T'.
     */
    private static boolean readsOnlySteps(SimulatedComponent model, Set<String> named, List<String> pins) {
        if (named.contains(EngineName.POWER.written()) || named.contains(EngineName.TIME.written())
                || pins.stream().anyMatch(named::contains)) {
            return false;
        }
        return model.states().stream().filter(State::continuous)
                .noneMatch(state -> named.contains(state.name()));
    }

    /** Whether it is a value at a moment ({@link #readsOnlySteps}), never to be averaged over a frame. */
    boolean moment() {
        return moment;
    }

    String part() {
        return part;
    }

    /** Its component's pins, in the order {@link #read} takes their voltages. */
    List<String> pins() {
        return pins;
    }

    /** The nodes the expression reads voltages from, pin by pin. */
    List<String> pinNodes() {
        return pinNodes;
    }

    /**
     * Its value at a moment: the voltage of each of its pins (in {@link #pinNodes()} order), the part's metered
     * currents, states and power, and the time.
     */
    double read(double[] pinVoltages, MeteredPart metered, double time) {
        return metered == null ? read(pinVoltages, Map.of(), Map.of(), Double.NaN, time, false)
                : read(pinVoltages, metered.terminals(), metered.states(), metered.power(), time, false);
    }

    /**
     * Its value from what is known of the part: the voltage of each pin, the current into each pin and each state by
     * name — NaN for one not given —, its power, the time and whether it has failed. A part that heats itself reports its own
     * {@code temperature} with its states ({@link ModelDevice#stateValues}); it is read in place of the bench's.
     */
    double read(double[] pinVoltages, Map<String, Double> pinCurrents, Map<String, Double> stateValues, double power,
                double time, boolean failed) {
        double[] currents = new double[pins.size()];
        for (int index = 0; index < currents.length; index++) {
            Double current = pinCurrents.get(pins.get(index));
            currents[index] = current == null ? Double.NaN : current;
        }
        return read(pinVoltages, currents, stateValues, power, time, failed);
    }

    /**
     * The same, the current into each pin given in the order of {@link #pins} — NaN for one not given —: what a reading
     * over many samples fills in place, sample after sample, without a map for each.
     */
    double read(double[] pinVoltages, double[] pinCurrents, Map<String, Double> stateValues, double power, double time,
                boolean failed) {
        double[] arguments = new double[pins.size() * 2 + states.size() + 3 + parameters.length];
        int position = 0;
        for (double current : pinCurrents) {
            arguments[position++] = current;
        }
        for (double voltage : pinVoltages) {
            arguments[position++] = voltage;
        }
        for (String state : states) {
            Double value = stateValues.get(state);
            arguments[position++] = value != null ? value : Double.NaN;
        }
        arguments[position++] = power;
        arguments[position++] = time;
        arguments[position++] = failed ? 1 : 0;
        System.arraycopy(parameters, 0, arguments, position, parameters.length);
        Double ownTemperature = stateValues.get(EngineName.TEMPERATURE.written());
        if (ownTemperature != null) {
            // The bench's temperature leads the parameters (parametersOf).
            arguments[position] = ownTemperature;
        }
        return function.evaluate(arguments);
    }

}
