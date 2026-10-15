package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Symbols;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What each part shows on its symbol while it runs, as its component writes it ({@code display { level = … }}): every
 * figure compiled once over the part's pins — each pin's name is its voltage —, its parameters and {@code power}, and
 * read at a moment from the circuit's node voltages. Nothing here knows any component: a capacitor's charge, a lamp's
 * glow and anything a new component writes are all one expression each.
 */
public final class PartDisplays {

    private static final Map<CircuitElementType, String> PRIMITIVE_COMPONENTS = Map.of(
            CircuitElementType.RESISTOR, "resistor", CircuitElementType.CAPACITOR, "capacitor",
            CircuitElementType.INDUCTOR, "inductor", CircuitElementType.DIODE, "diode",
            CircuitElementType.VOLTAGE_SOURCE, "voltage-source", CircuitElementType.CURRENT_SOURCE, "current-source");

    /** One figure of one part: its expression, the node each pin name reads, and the part's parameters. */
    private record Figure(String part, String name, RealFunction function, List<String> pinNodes, double[] parameters) {
    }

    private final List<Figure> figures;

    private PartDisplays(List<Figure> figures) {
        this.figures = List.copyOf(figures);
    }

    public static PartDisplays of(CompiledObject object) {
        List<Figure> figures = new ArrayList<>();
        if (object.definition() instanceof ObjectDefinition.Circuit circuit) {
            for (CircuitElement element : circuit.elements()) {
                figuresOf(object, element, figures);
            }
        }
        return new PartDisplays(figures);
    }

    public boolean isEmpty() {
        return figures.isEmpty();
    }

    private static void figuresOf(CompiledObject object, CircuitElement element, List<Figure> into) {
        String reference = element.type() == CircuitElementType.MODEL ? element.component() : PRIMITIVE_COMPONENTS.get(element.type());
        if (reference == null) {
            return;
        }
        ComponentModel model;
        try {
            model = ComponentDocuments.read(object.componentSource(reference), reference, object.library());
        } catch (IllegalArgumentException unreadable) {
            return;
        }
        if (model.displays().isEmpty()) {
            return;
        }
        // A primitive's first terminal is its component's first pin, its second the second.
        List<String> pins = model.pins().stream().map(ComponentModel.Pin::name).toList();
        List<String> pinNodes = new ArrayList<>();
        for (int index = 0; index < pins.size(); index++) {
            String node = element.type() == CircuitElementType.MODEL ? element.pins().get(pins.get(index))
                    : index == 0 ? element.positive() : index == 1 ? element.negative() : null;
            pinNodes.add(node);
        }
        Map<String, Double> parameters = new LinkedHashMap<>();
        model.parameters().forEach(parameter -> parameters.put(parameter.name(), parameter.value()));
        PartExpression.overrideConstants(object, element, parameters);
        List<String> names = new ArrayList<>(pins);
        names.add("power");
        names.addAll(parameters.keySet());
        double[] parameterValues = parameters.values().stream().mapToDouble(Double::doubleValue).toArray();
        model.displays().forEach((name, expression) -> {
            try {
                RealFunction function = RealCompiler.compile(FormulaLanguage.standard().parse(expression),
                        Symbols.of(List.of(), names), expression);
                into.add(new Figure(element.id(), name, function, pinNodes, parameterValues));
            } catch (RuntimeException unwritten) {
                throw new IllegalArgumentException(model.name() + ": its display '" + name + " = " + expression
                        + "' does not read: " + unwritten.getMessage(), unwritten);
            }
        });
    }

    /** Every figure at a moment: the node voltages then, and each part's power. */
    public Map<String, Map<String, Double>> read(Map<String, Double> voltages, Map<String, PartLoad> loads) {
        Map<String, Map<String, Double>> shown = new LinkedHashMap<>();
        for (Figure figure : figures) {
            double[] arguments = new double[figure.pinNodes().size() + 1 + figure.parameters().length];
            for (int index = 0; index < figure.pinNodes().size(); index++) {
                String node = figure.pinNodes().get(index);
                arguments[index] = node == null || node.equals(ObjectDefinition.Circuit.GROUND) ? 0 : voltages.getOrDefault(node, 0.0);
            }
            PartLoad load = loads.get(figure.part());
            arguments[figure.pinNodes().size()] = load == null || Double.isNaN(load.power()) ? 0 : load.power();
            System.arraycopy(figure.parameters(), 0, arguments, figure.pinNodes().size() + 1, figure.parameters().length);
            double value = figure.function().evaluate(arguments);
            if (Double.isFinite(value)) {
                shown.computeIfAbsent(figure.part(), ignored -> new LinkedHashMap<>()).put(figure.name(), value);
            }
        }
        return shown;
    }
}
