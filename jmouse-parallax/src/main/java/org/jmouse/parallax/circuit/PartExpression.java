package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ComponentVocabulary;
import org.jmouse.parallax.component.ModelLanguage;
import org.jmouse.parallax.component.PinQuantities;
import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;

import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
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
     * {@code expression}, as {@code model} writes it, for {@code element} of {@code circuit} as it stands on its bench.
     *
     * @throws IllegalArgumentException when it does not read — naming the component and what it wrote
     */
    static PartExpression of(ElaboratedCircuit circuit, CircuitElement element, ComponentModel model, String expression) {
        List<String> pins = model.pins().stream().map(ComponentModel.Pin::name).toList();
        List<String> pinNodes = model.pins().stream().map(pin -> {
            String key = wiredKey(element, model, pin);
            return key == null ? ObjectDefinition.Circuit.GROUND : element.pins().get(key);
        }).toList();
        Map<String, Double> parameters = circuit.parameters(model, element, "");
        try {
            // The component's own definitions — its libraries', the constants among them — read here as in its equations.
            List<Formula.Definition> library = ModelLanguage.LANGUAGE.definitions(ModelLanguage.libraryText(model));
            Formula read = ModelLanguage.readings(model.name(), expression, library);
            // What the expression reads, the definitions it calls included: a pin's voltage and current name the pin.
            Set<String> named = read.variables();
            // A row state is read slot by slot — `ddram_5` —; a display cannot take a whole row. Only the slots the
            // expression names are bound: a 256-byte memory would otherwise be 256 arguments of every figure it shows.
            List<String> states = model.states().stream().flatMap(state -> state.initial() instanceof Object[] row
                    ? IntStream.range(0, row.length).mapToObj(index -> slotName(state.name(), index)).filter(named::contains)
                    : Stream.of(state.name())).toList();
            // The arguments: each pin's current in a slot of its own, by the pin's position; then each pin's voltage by
            // its name, the states, the power, the time, whether it failed and the parameters.
            List<String> names = new ArrayList<>(pins);
            names.addAll(states);
            names.add(ComponentVocabulary.POWER);
            names.add(ComponentVocabulary.TIME);
            names.add(ComponentVocabulary.FAILED);
            names.addAll(parameters.keySet());
            Formula members = PinQuantities.bind(read, pins, written -> model.name() + ": '" + written + "'");
            Formula bound = PinQuantities.bindCurrents(members, pins, pin -> new Formula.Slot(pins.indexOf(pin), pin + ".current"));
            RealFunction function = RealCompiler.compile(bound, Symbols.withSlots(pins.size(), names, List.of()), expression);
            boolean moment = !PinQuantities.readsCurrent(members) && readsOnlySteps(model, named, pins);
            return new PartExpression(element.id(), function, pinNodes, pins, states,
                    parameters.values().stream().mapToDouble(Double::doubleValue).toArray(), moment);
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException(model.name() + ": '" + expression + "' does not read: " + unreadable.getMessage(), unreadable);
        }
    }

    /**
     * How a part wires one of its component's pins, as the key of its pin map: the pin's own name, or — on a part of
     * several blocks, a pin each block has — the drawn block's, `A.output`. The drawn block is the first one wired: a
     * dual comparator is drawn as one symbol, and its figures read that block, not a name no block is wired by. Null for
     * a pin the part leaves unwired.
     */
    static String wiredKey(CircuitElement element, ComponentModel model, ComponentModel.Pin pin) {
        Map<String, String> wired = element.pins();
        if (wired == null) {
            return null;
        }
        if (wired.containsKey(pin.name()) || pin.shared() || model.units().isEmpty()) {
            return wired.containsKey(pin.name()) ? pin.name() : null;
        }
        for (String unit : model.units()) {
            boolean drawn = wired.keySet().stream().anyMatch(key -> key.startsWith(unit + "."));
            if (drawn) {
                return wired.containsKey(unit + "." + pin.name()) ? unit + "." + pin.name() : null;
            }
        }
        return null;
    }

    /**
     * Whether an expression reads nothing that moves continuously: no pin's voltage or current, no power, no time, no
     * continuous state — only discrete states, their slots and parameters. Such a figure — a character, a count — is a
     * value at a moment: averaged with another, a space and an 'l' make a 'T'.
     */
    private static boolean readsOnlySteps(ComponentModel model, Set<String> named, List<String> pins) {
        if (named.contains(ComponentVocabulary.POWER) || named.contains(ComponentVocabulary.TIME)
                || pins.stream().anyMatch(named::contains)) {
            return false;
        }
        return model.states().stream().filter(state -> !state.discrete())
                .noneMatch(state -> named.contains(state.name()));
    }

    /** Whether it is a value at a moment ({@link #readsOnlySteps}), never to be averaged over a frame. */
    boolean moment() {
        return moment;
    }

    /** The name one slot of a row state is read by: the row's name and the slot's index, `ddram_5`. */
    static String slotName(String row, int index) {
        return row + "_" + index;
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
        double[] arguments = new double[pins.size() * 2 + states.size() + 3 + parameters.length];
        int position = 0;
        for (String pin : pins) {
            Double current = pinCurrents.get(pin);
            arguments[position++] = current == null ? Double.NaN : current;
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
        Double ownTemperature = stateValues.get(ComponentVocabulary.TEMPERATURE);
        if (ownTemperature != null) {
            // The bench's temperature leads the parameters (parametersOf).
            arguments[position] = ownTemperature;
        }
        return function.evaluate(arguments);
    }

}
