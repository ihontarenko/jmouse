package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What each part shows on its symbol while it runs, as its component writes it ({@code display { level = … }}): every
 * figure is a {@link PartExpression} — the same names an instrument's {@code measure} reads, so a display and a meter
 * can never disagree about what a component may say —, read at a moment from the circuit's node voltages and the
 * part's load. Nothing here knows any component: a capacitor's charge, a lamp's glow and anything a new component
 * writes are all one expression each.
 */
public final class PartDisplays {

    private static final Map<CircuitElementType, String> PRIMITIVE_COMPONENTS = Map.of(
            CircuitElementType.RESISTOR, "resistor", CircuitElementType.CAPACITOR, "capacitor",
            CircuitElementType.INDUCTOR, "inductor", CircuitElementType.DIODE, "diode",
            CircuitElementType.VOLTAGE_SOURCE, "voltage-source", CircuitElementType.CURRENT_SOURCE, "current-source");

    /** A primitive's terminals as its current is reported when they are not named after its component's pins. */
    private static final List<String> PRIMITIVE_TERMINALS = List.of("+", "−");

    /**
     * One figure of one part: its expression, the node each pin reads, and the terminal each pin's current may be
     * reported under instead of the pin's own name.
     */
    private record Figure(String part, String name, PartExpression expression, List<String> pinNodes,
                          List<String> terminals) {
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
        boolean primitive = element.type() != CircuitElementType.MODEL;
        String reference = primitive ? PRIMITIVE_COMPONENTS.get(element.type()) : element.component();
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
        List<String> terminals = new ArrayList<>();
        for (int index = 0; index < pins.size(); index++) {
            String node = !primitive ? element.pins().get(pins.get(index))
                    : index == 0 ? element.positive() : index == 1 ? element.negative() : null;
            pinNodes.add(node);
            terminals.add(primitive && index < PRIMITIVE_TERMINALS.size() ? PRIMITIVE_TERMINALS.get(index) : pins.get(index));
        }
        model.displays().forEach((name, expression) -> {
            try {
                into.add(new Figure(element.id(), name, PartExpression.of(object, element, model, expression), pinNodes,
                        terminals));
            } catch (IllegalArgumentException unwritten) {
                throw new IllegalArgumentException(unwritten.getMessage() + " (its display '" + name + "')", unwritten);
            }
        });
    }

    /**
     * Every figure at a moment: the node voltages then, each part's terminal currents, power and states, and the time. A
     * figure reading what its part does not report — a current not metered — is NaN, and so not shown.
     */
    public Map<String, Map<String, Double>> read(Map<String, Double> voltages, Map<String, PartLoad> loads,
                                                 Map<String, Map<String, Double>> states, double time) {
        Map<String, Map<String, Double>> shown = new LinkedHashMap<>();
        for (Figure figure : figures) {
            List<String> pins = figure.expression().pins();
            double[] pinVoltages = new double[pins.size()];
            for (int index = 0; index < pins.size(); index++) {
                String node = figure.pinNodes().get(index);
                pinVoltages[index] = node == null || node.equals(ObjectDefinition.Circuit.GROUND) ? 0 : voltages.getOrDefault(node, 0.0);
            }
            PartLoad load = loads.get(figure.part());
            double value = figure.expression().read(pinVoltages, pinCurrents(load, pins, figure.terminals()), states.getOrDefault(figure.part(), Map.of()),
                    load == null || Double.isNaN(load.power()) ? 0 : load.power(), time);
            if (Double.isFinite(value)) {
                shown.computeIfAbsent(figure.part(), ignored -> new LinkedHashMap<>()).put(figure.name(), value);
            }
        }
        return shown;
    }

    /** The current into each pin, reported under the pin's own name or its primitive terminal. */
    private static Map<String, Double> pinCurrents(PartLoad load, List<String> pins, List<String> terminals) {
        Map<String, Double> currents = new LinkedHashMap<>();
        if (load == null) {
            return currents;
        }
        for (int index = 0; index < pins.size(); index++) {
            Double current = load.terminals().get(pins.get(index));
            if (current == null) {
                current = load.terminals().get(terminals.get(index));
            }
            if (current != null) {
                currents.put(pins.get(index), current);
            }
        }
        return currents;
    }
}
