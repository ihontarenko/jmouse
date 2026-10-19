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

    private PartExpression(String part, RealFunction function, List<String> pinNodes, List<String> pins, List<String> states,
                           double[] parameters) {
        this.part = part;
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
        List<String> pinNodes = pins.stream().map(pin -> element.pins() == null ? ObjectDefinition.Circuit.GROUND
                : element.pins().getOrDefault(pin, ObjectDefinition.Circuit.GROUND)).toList();
        List<String> states = model.states().stream().map(ComponentModel.State::name).toList();
        Map<String, Double> parameters = circuit.parameters(model, element, "");
        // The arguments: each pin's current in a slot of its own, by the pin's position; then each pin's voltage by its
        // name, the states, the power, the time and the parameters.
        List<String> names = new ArrayList<>(pins);
        names.addAll(states);
        names.add(ComponentVocabulary.POWER);
        names.add(ComponentVocabulary.TIME);
        names.addAll(parameters.keySet());
        try {
            // The component's own definitions — its libraries', the constants among them — read here as in its equations.
            List<Formula.Definition> library = ModelLanguage.LANGUAGE.definitions(ModelLanguage.libraryText(model));
            Formula read = ModelLanguage.readings(model.name(), expression, library);
            Formula members = PinQuantities.bind(read, pins, written -> model.name() + ": '" + written + "'");
            Formula bound = PinQuantities.bindCurrents(members, pins, pin -> new Formula.Slot(pins.indexOf(pin), pin + ".current"));
            RealFunction function = RealCompiler.compile(bound, Symbols.withSlots(pins.size(), names, List.of()), expression);
            return new PartExpression(element.id(), function, pinNodes, pins, states,
                    parameters.values().stream().mapToDouble(Double::doubleValue).toArray());
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException(model.name() + ": '" + expression + "' does not read: " + unreadable.getMessage(), unreadable);
        }
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
        return metered == null ? read(pinVoltages, Map.of(), Map.of(), Double.NaN, time)
                : read(pinVoltages, metered.terminals(), metered.states(), metered.power(), time);
    }

    /**
     * Its value from what is known of the part: the voltage of each pin, the current into each pin and each state by
     * name — NaN for one not given —, its power and the time. A part that heats itself reports its own
     * {@code temperature} with its states ({@link ModelDevice#stateValues}); it is read in place of the bench's.
     */
    double read(double[] pinVoltages, Map<String, Double> pinCurrents, Map<String, Double> stateValues, double power,
                double time) {
        double[] arguments = new double[pins.size() * 2 + states.size() + 2 + parameters.length];
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
        System.arraycopy(parameters, 0, arguments, position, parameters.length);
        Double ownTemperature = stateValues.get(ComponentVocabulary.TEMPERATURE);
        if (ownTemperature != null) {
            // The bench's temperature leads the parameters (parametersOf).
            arguments[position] = ownTemperature;
        }
        return function.evaluate(arguments);
    }

}
