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
            if (primitive) {
                pinNodes.add(index == 0 ? element.positive() : index == 1 ? element.negative() : null);
                terminals.add(index < PRIMITIVE_TERMINALS.size() ? PRIMITIVE_TERMINALS.get(index) : pins.get(index));
                continue;
            }
            // On a part of several blocks, the drawn block's pin: what its symbol shows.
            String key = PartExpression.wiredKey(element, model, model.pins().get(index));
            pinNodes.add(key == null ? null : element.pins().get(key));
            terminals.add(key == null ? pins.get(index) : key);
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
     * Every figure over a frame: the frame's mean node voltages, each part's mean terminal currents over the samples that
     * traced them, its power and states, and the time. A figure is read from means, never from the frame's last instant:
     * an LED driven at 1 kHz shows the brightness it has on average, not whichever half of a period the frame ended in. A
     * part the samples did not trace reads its currents at the last sample. A figure reading what its part does not
     * report — a current not metered — is NaN, and so not shown.
     */
    public Map<String, Map<String, Double>> read(Map<String, Double> voltages, Map<String, PartLoad> loads,
                                                 List<LiveSample> samples, Map<String, Map<String, Double>> states,
                                                 double time) {
        Map<String, Map<String, Double>> meanCurrents = meanTerminals(samples);
        Map<String, Map<String, Double>> shown = new LinkedHashMap<>();
        for (Figure figure : figures) {
            List<String> pins = figure.expression().pins();
            double[] pinVoltages = new double[pins.size()];
            for (int index = 0; index < pins.size(); index++) {
                String node = figure.pinNodes().get(index);
                pinVoltages[index] = node == null || node.equals(ObjectDefinition.Circuit.GROUND) ? 0 : voltages.getOrDefault(node, 0.0);
            }
            PartLoad load = loads.get(figure.part());
            Map<String, Double> terminals = meanCurrents.getOrDefault(figure.part(), load == null ? Map.of() : load.terminals());
            double value = figure.expression().read(pinVoltages, pinCurrents(terminals, pins, figure.terminals()),
                    states.getOrDefault(figure.part(), Map.of()), load == null || Double.isNaN(load.power()) ? 0 : load.power(),
                    time, load != null && load.failure() != null);
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
                double value = figure.expression().read(pinVoltages, currents, partStates, power, sample.time(),
                        load != null && load.failure() != null);
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

    /** Each part's mean current into each terminal over the samples that traced it; empty when none did. */
    private static Map<String, Map<String, Double>> meanTerminals(List<LiveSample> samples) {
        Map<String, Map<String, Double>> sums = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (LiveSample sample : samples) {
            if (sample.terminals() == null) {
                continue;
            }
            sample.terminals().forEach((part, terminals) -> {
                Map<String, Double> sum = sums.computeIfAbsent(part, ignored -> new LinkedHashMap<>());
                terminals.forEach((terminal, current) -> sum.merge(terminal, current, Double::sum));
                counts.merge(part, 1, Integer::sum);
            });
        }
        sums.forEach((part, sum) -> sum.replaceAll((terminal, total) -> total / counts.get(part)));
        return sums;
    }

    /** The current into each pin, reported under the pin's own name or its primitive terminal. */
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
