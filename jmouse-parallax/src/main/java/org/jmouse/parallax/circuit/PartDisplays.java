package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;

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

    /** A primitive's terminals as its current is reported when they are not named after its component's pins. */
    private static final List<String> PRIMITIVE_TERMINALS = List.of(ObjectDefinition.PRIMITIVE_POSITIVE, ObjectDefinition.PRIMITIVE_NEGATIVE);

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

    /** What every part of a circuit shows, each figure compiled over the part as it stands on the bench. */
    static PartDisplays of(ElaboratedCircuit circuit) {
        List<Figure> figures = new ArrayList<>();
        for (CircuitElement element : circuit.writtenParts()) {
            figuresOf(circuit, element, figures);
        }
        return new PartDisplays(figures);
    }

    public boolean isEmpty() {
        return figures.isEmpty();
    }

    private static void figuresOf(ElaboratedCircuit circuit, CircuitElement element, List<Figure> into) {
        boolean primitive = element.type() != CircuitElementType.MODEL;
        ComponentModel model = circuit.componentOf(element).orElse(null);
        if (model == null || model.displays().isEmpty()) {
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
                into.add(new Figure(element.id(), name, PartExpression.of(circuit, element, model, expression), pinNodes,
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

    /**
     * Every stress figure ({@link PartLoad#STRESSES}) at its worst over these samples: a limit is broken at the moment it
     * is passed, never on average — a 25 V capacitor on a sine of 50 V peak is at 200 %, though its mean voltage is
     * nought. Each sample's node voltages are read, and the part's currents where the sample traced them, else the
     * frame's.
     */
    public Map<String, Map<String, Double>> worstStresses(List<LiveSample> samples, List<String> order,
                                                          Map<String, PartLoad> loads, Map<String, Map<String, Double>> states) {
        Map<String, Map<String, Double>> worst = new LinkedHashMap<>();
        for (Figure figure : figures) {
            if (!PartLoad.STRESSES.contains(figure.name())) {
                continue;
            }
            List<String> pins = figure.expression().pins();
            int[] nodes = figure.pinNodes().stream().mapToInt(node -> node == null ? -1 : order.indexOf(node)).toArray();
            PartLoad load = loads.get(figure.part());
            Map<String, Double> partStates = states.getOrDefault(figure.part(), Map.of());
            double power = load == null || Double.isNaN(load.power()) ? 0 : load.power();
            double highest = Double.NEGATIVE_INFINITY;
            double[] pinVoltages = new double[pins.size()];
            for (LiveSample sample : samples) {
                for (int index = 0; index < pins.size(); index++) {
                    pinVoltages[index] = nodes[index] < 0 ? 0 : sample.voltages()[nodes[index]];
                }
                Map<String, Double> traced = sample.terminals() == null ? null : sample.terminals().get(figure.part());
                Map<String, Double> currents = traced != null ? pinCurrents(traced, pins, figure.terminals())
                        : pinCurrents(load == null ? Map.of() : load.terminals(), pins, figure.terminals());
                double value = figure.expression().read(pinVoltages, currents, partStates, power, sample.time());
                if (Double.isFinite(value)) {
                    highest = Math.max(highest, value);
                }
            }
            if (highest > Double.NEGATIVE_INFINITY) {
                worst.computeIfAbsent(figure.part(), ignored -> new LinkedHashMap<>()).put(figure.name(), highest);
            }
        }
        return worst;
    }

    /** The current into each pin, reported under the pin's own name or its primitive terminal. */
    private static Map<String, Double> pinCurrents(PartLoad load, List<String> pins, List<String> terminals) {
        return load == null ? new LinkedHashMap<>() : pinCurrents(load.terminals(), pins, terminals);
    }

    private static Map<String, Double> pinCurrents(Map<String, Double> reported, List<String> pins, List<String> terminals) {
        Map<String, Double> currents = new LinkedHashMap<>();
        for (int index = 0; index < pins.size(); index++) {
            Double current = reported.get(pins.get(index));
            if (current == null) {
                current = reported.get(terminals.get(index));
            }
            if (current != null) {
                currents.put(pins.get(index), current);
            }
        }
        return currents;
    }
}
