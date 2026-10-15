package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Symbols;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One expression a component writes over itself, compiled once for one part of a circuit and read at any moment: each
 * pin's name is its voltage, {@code current(pin)} the current into that pin, a state's name its value, a parameter's
 * name its value — the part's overrides and the bench's {@code temperature} and {@code realistic} included —, and
 * {@code power} and {@code time} what they say. What an instrument reads is one of these; nothing here knows any
 * instrument, so a meter nobody has thought of yet is written in its own document.
 */
final class PartExpression {

    /** {@code current(pin)}, as written — the current into that pin, in a measure as in an equation. */
    static final Pattern PIN_CURRENT = Pattern.compile("\\bcurrent\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\)");
    /** What {@code current(pin)} is read as inside the compiled expression: a name no document can write. */
    static final String CURRENT_PREFIX = "current__";
    private static final String POWER = "power";
    private static final String TIME = "time";

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
     * {@code expression}, as {@code model} writes it, for {@code element} of {@code object}'s circuit.
     *
     * @throws IllegalArgumentException when it does not read — naming the component and what it wrote
     */
    static PartExpression of(CompiledObject object, CircuitElement element, ComponentModel model, String expression) {
        List<String> pins = model.pins().stream().map(ComponentModel.Pin::name).toList();
        Matcher currents = PIN_CURRENT.matcher(expression);
        StringBuilder rewritten = new StringBuilder();
        while (currents.find()) {
            if (!pins.contains(currents.group(1))) {
                throw new IllegalArgumentException(model.name() + ": '" + currents.group() + "' — '" + currents.group(1)
                        + "' is not one of its pins (" + String.join(", ", pins) + ").");
            }
            currents.appendReplacement(rewritten, CURRENT_PREFIX + currents.group(1));
        }
        currents.appendTail(rewritten);
        List<String> pinNodes = pins.stream().map(pin -> element.pins() == null ? ObjectDefinition.Circuit.GROUND
                : element.pins().getOrDefault(pin, ObjectDefinition.Circuit.GROUND)).toList();
        List<String> states = model.states().stream().map(ComponentModel.State::name).toList();
        Map<String, Double> parameters = parametersOf(object, element, model);
        List<String> names = new ArrayList<>(pins);
        pins.forEach(pin -> names.add(CURRENT_PREFIX + pin));
        names.addAll(states);
        names.add(POWER);
        names.add(TIME);
        names.addAll(parameters.keySet());
        try {
            RealFunction function = RealCompiler.compile(FormulaLanguage.standard().parse(rewritten.toString()),
                    Symbols.of(List.of(), names), expression);
            return new PartExpression(element.id(), function, pinNodes, pins, states,
                    parameters.values().stream().mapToDouble(Double::doubleValue).toArray());
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException(model.name() + ": '" + expression + "' does not read: " + unreadable.getMessage(), unreadable);
        }
    }

    String part() {
        return part;
    }

    /** The nodes the expression reads voltages from, pin by pin. */
    List<String> pinNodes() {
        return pinNodes;
    }

    /**
     * Its value at a moment: the voltage of each of its pins (in {@link #pinNodes()} order), the part's metered
     * currents, states and power, and the time.
     */
    double read(double[] pinVoltages, LiveCircuitSolver.Metered metered, double time) {
        double[] arguments = new double[pins.size() * 2 + states.size() + 2 + parameters.length];
        int position = 0;
        for (double voltage : pinVoltages) {
            arguments[position++] = voltage;
        }
        for (String pin : pins) {
            Double current = metered == null ? null : metered.terminals().get(pin);
            arguments[position++] = current == null ? Double.NaN : current;
        }
        for (String state : states) {
            Double value = metered == null ? null : metered.states().get(state);
            arguments[position++] = value == null ? Double.NaN : value;
        }
        arguments[position++] = metered == null ? Double.NaN : metered.power();
        arguments[position++] = time;
        System.arraycopy(parameters, 0, arguments, position, parameters.length);
        return function.evaluate(arguments);
    }

    /** The bench's temperature and realism, then the component's parameters, then the part's overrides that are constants. */
    private static Map<String, Double> parametersOf(CompiledObject object, CircuitElement element, ComponentModel model) {
        Map<String, Double> parameters = new LinkedHashMap<>();
        boolean realistic = object.definition() instanceof ObjectDefinition.Circuit circuit && circuit.conditions() != null
                && circuit.conditions().realistic();
        double celsius = object.definition() instanceof ObjectDefinition.Circuit circuit && circuit.conditions() != null
                ? circuit.conditions().temperature() : ObjectDefinition.BenchConditions.NOMINAL_TEMPERATURE;
        parameters.put(Netlist.TEMPERATURE, celsius + 273.15);
        parameters.put(Netlist.REALISTIC, realistic ? 1.0 : 0.0);
        model.parameters().forEach(parameter -> parameters.put(parameter.name(), parameter.value()));
        overrideConstants(object, element, parameters);
        return parameters;
    }

    /** Writes the part's overrides of these parameters over them, those that are constants; any other leaves the component's own. */
    static void overrideConstants(CompiledObject object, CircuitElement element, Map<String, Double> parameters) {
        if (element.overrides() == null) {
            return;
        }
        element.overrides().forEach((name, expression) -> {
            if (parameters.containsKey(name)) {
                try {
                    parameters.put(name, object.constant(expression));
                } catch (RuntimeException notConstant) {
                    // A value that is not a constant leaves the component's own.
                }
            }
        });
    }
}
