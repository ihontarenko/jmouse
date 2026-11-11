package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.Formula;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.State;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;
import org.jmouse.cad.simulation.elaboration.ElaboratedPart;


import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What each part shows on its symbol while it runs, as its component writes it ({@code display { level = … }}): every
 * figure is a {@link PartExpression} — the same names an instrument's {@code measure} reads, so a display and a meter
 * can never disagree about what a component may say —, read at a moment from the circuit's node voltages and the
 * part's load. Nothing here knows any component: a capacitor's charge, a lamp's glow and anything a new component
 * writes are all one expression each.
 *
 * <p>A figure written as the bare name of a row state — {@code screen = framebuffer} — is a picture: the row's bytes,
 * handed on whole as hexadecimal, for a symbol's {@code bitmap} to draw. A figure that reads only discrete states and
 * parameters is a moment ({@link PartExpression#moment}), never to be averaged with another frame's.</p>
 */
public final class PartDisplays {

    /**
     * One figure of one part: its expression, the node each pin reads, and the terminal each pin's current may be
     * reported under instead of the pin's own name.
     */
    private record Figure(String part, String name, PartExpression expression, List<String> pinNodes,
                          List<String> terminals) {
    }

    /** A picture of one part: the row state it shows, by the names of its slots — {@code ddram_0} … —, a byte each. */
    private record Picture(String part, String name, List<String> slots) {

        int length() {
            return slots.size();
        }
    }

    private final List<Figure> figures;
    private final List<Picture> pictures;

    private PartDisplays(List<Figure> figures, List<Picture> pictures) {
        this.figures = List.copyOf(figures);
        this.pictures = List.copyOf(pictures);
    }

    /** What every part of a circuit shows, each figure compiled over the part as it stands on the bench. */
    static PartDisplays of(ElaboratedCircuit circuit) {
        List<Figure> figures = new ArrayList<>();
        List<Picture> pictures = new ArrayList<>();
        for (ElaboratedPart part : circuit.written().parts()) {
            figuresOf(circuit, part, figures, pictures);
        }
        return new PartDisplays(figures, pictures);
    }

    public boolean isEmpty() {
        return figures.isEmpty() && pictures.isEmpty();
    }

    /** Whether a part shows a figure of that name. */
    public boolean shows(String part, String name) {
        return figures.stream().anyMatch(figure -> figure.part().equals(part) && figure.name().equals(name));
    }

    /** The figures of a part that are values at a moment, by name. */
    public Set<String> momentsOf(String part) {
        Set<String> moments = new LinkedHashSet<>();
        figures.stream().filter(figure -> figure.part().equals(part) && figure.expression().moment())
                .forEach(figure -> moments.add(figure.name()));
        return moments;
    }

    /**
     * Every picture as it stands: each part's rows read slot by slot from its states, two hexadecimal digits a byte; a
     * slot not known — X, Z, a value outside a byte — is 00, an unlit stretch rather than a hole.
     */
    public Map<String, Map<String, String>> pictures(Map<String, Map<String, Double>> states) {
        Map<String, Map<String, String>> shown = new LinkedHashMap<>();
        HexFormat hexadecimal = HexFormat.of();
        for (Picture picture : pictures) {
            Map<String, Double> partStates = states.getOrDefault(picture.part(), Map.of());
            byte[] bytes = new byte[picture.length()];
            for (int index = 0; index < bytes.length; index++) {
                Double value = partStates.get(picture.slots().get(index));
                bytes[index] = value == null || !(value >= 0 && value <= 255) ? 0 : (byte) value.intValue();
            }
            shown.computeIfAbsent(picture.part(), ignored -> new LinkedHashMap<>()).put(picture.name(), hexadecimal.formatHex(bytes));
        }
        return shown;
    }

    private static void figuresOf(ElaboratedCircuit circuit, ElaboratedPart part, List<Figure> into, List<Picture> pictures) {
        SimulatedComponent model = part.component();
        Map<String, Formula> displays = model.displays();
        if (displays.isEmpty()) {
            return;
        }
        List<String> pins = model.pins().stream().map(Pin::name).toList();
        List<String> pinNodes = new ArrayList<>();
        List<String> terminals = new ArrayList<>();
        for (int index = 0; index < pins.size(); index++) {
            // On a part of several blocks, the drawn block's pin: what its symbol shows.
            String key = PartExpression.wiredKey(part, model.pins().get(index));
            pinNodes.add(key == null ? null : part.connections().get(key));
            terminals.add(key == null ? pins.get(index) : key);
        }
        displays.forEach((name, expression) -> {
            State row = rowNamed(model, expression.written().strip());
            if (row != null) {
                pictures.add(new Picture(part.reference(), name, row.index().names(row.name())));
                return;
            }
            try {
                into.add(new Figure(part.reference(), name, PartExpression.of(circuit, part, expression.written()), pinNodes,
                        terminals));
            } catch (IllegalArgumentException unwritten) {
                throw new IllegalArgumentException(unwritten.getMessage() + " (its display '" + name + "')", unwritten);
            }
        });
    }

    /** The row state a figure names whole, or null when it names none. */
    private static State rowNamed(SimulatedComponent model, String written) {
        return model.states().stream().filter(state -> state.name().equals(written) && state.index() != null)
                .findFirst().orElse(null);
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
                pinVoltages[index] = node == null || node.equals(Circuit.GROUND) ? 0 : voltages.getOrDefault(node, 0.0);
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
            // A sample that traced no currents reads the frame's: worked out once, not once a sample.
            double[] frameCurrents = new double[pins.size()];
            fillCurrents(load == null ? Map.of() : load.terminals(), pins, figure.terminals(), frameCurrents);
            double[] sampleCurrents = new double[pins.size()];
            boolean failed = load != null && load.failure() != null;
            for (LiveSample sample : samples) {
                for (int index = 0; index < pins.size(); index++) {
                    pinVoltages[index] = nodes[index] < 0 ? 0 : sample.voltages()[nodes[index]];
                }
                Map<String, Double> traced = sample.terminals() == null ? null : sample.terminals().get(figure.part());
                double[] currents = frameCurrents;
                if (traced != null) {
                    fillCurrents(traced, pins, figure.terminals(), sampleCurrents);
                    currents = sampleCurrents;
                }
                double value = figure.expression().read(pinVoltages, currents, partStates, power, sample.time(), failed);
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
    /** The current into each pin, in the pins' order, from what a part reported by pin or by terminal; NaN for none. */
    private static void fillCurrents(Map<String, Double> reported, List<String> pins, List<String> terminals, double[] into) {
        for (int index = 0; index < pins.size(); index++) {
            Double current = reported.get(pins.get(index));
            if (current == null) {
                current = reported.get(terminals.get(index));
            }
            into[index] = current == null ? Double.NaN : current;
        }
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
