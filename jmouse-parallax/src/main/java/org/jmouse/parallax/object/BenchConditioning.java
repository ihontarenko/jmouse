package org.jmouse.parallax.object;

import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.parallax.object.ObjectDefinition.SourceWaveform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A circuit as it runs on a bench: its {@link BenchConditions} applied to the parts, so that what is simulated is the
 * circuit in its environment and nothing downstream needs to know a bench exists.
 *
 * <ul>
 *   <li>A resistor's and a capacitor's value moves to the chosen corner of its tolerance and by its temperature
 *       coefficient away from 25 °C: {@code value · (1 + corner · tolerance) · (1 + coefficient · (T − 25))}.</li>
 *   <li>A modelled part reads the temperature in its own equations: it is given it, and its drifts — written in its
 *       component — follow.</li>
 *   <li>A source's offset is a DC source of its own: in series with a voltage source, through a node of its own;
 *       beside a current source, between the same nodes.</li>
 * </ul>
 */
public final class BenchConditioning {

    private BenchConditioning() {
    }

    /** The circuit with its conditions applied, every part's own tolerance the default of its kind. */
    public static Circuit apply(Circuit circuit) {
        return apply(circuit, element -> BenchConditions.Tolerance.defaultFor(element.type()));
    }

    /**
     * The circuit with its conditions applied and none left on it; a circuit without any, as it is.
     *
     * @param written a part's tolerance and temperature coefficient as its component writes them — what it has when
     *                the bench says nothing of its own
     */
    public static Circuit apply(Circuit circuit, Function<CircuitElement, BenchConditions.Tolerance> written) {
        BenchConditions conditions = circuit.conditions();
        if (conditions == null) {
            return circuit;
        }
        Set<String> ids = new HashSet<>();
        Set<String> nodes = new HashSet<>();
        for (CircuitElement element : circuit.elements()) {
            ids.add(element.id());
            if (element.type() == CircuitElementType.MODEL) {
                nodes.addAll(element.pins().values());
            } else {
                nodes.add(element.positive());
                nodes.add(element.negative());
            }
        }
        List<CircuitElement> elements = new ArrayList<>();
        int number = 1;
        for (CircuitElement element : circuit.elements()) {
            String failure = conditions.failures().get(element.id());
            if (failure != null) {
                elements.addAll(failed(element, failure, ids));
                continue;
            }
            CircuitElement placed = withEnvironment(element, conditions, written);
            String offset = conditions.offsets().get(element.id());
            if (offset == null || !element.type().isSource()) {
                elements.add(placed);
                continue;
            }
            while (ids.contains("O" + number) || nodes.contains("offset_" + number)) {
                number++;
            }
            String id = "O" + number;
            String node = "offset_" + number;
            ids.add(id);
            nodes.add(node);
            if (element.type() == CircuitElementType.VOLTAGE_SOURCE) {
                elements.add(withPositive(placed, node));
                elements.add(direct(id, CircuitElementType.VOLTAGE_SOURCE, element.positive(), node, offset));
            } else {
                elements.add(placed);
                elements.add(direct(id, CircuitElementType.CURRENT_SOURCE, element.positive(), element.negative(), offset));
            }
        }
        return new Circuit(elements, circuit.time(), circuit.layout(), null);
    }

    private static CircuitElement withEnvironment(CircuitElement element, BenchConditions conditions,
                                                  Function<CircuitElement, BenchConditions.Tolerance> written) {
        if (element.type() == CircuitElementType.MODEL) {
            return onTheBench(element, conditions);
        }
        if (element.type() != CircuitElementType.RESISTOR && element.type() != CircuitElementType.CAPACITOR) {
            return element;
        }
        BenchConditions.Tolerance tolerance = conditions.tolerances().containsKey(element.id())
                ? conditions.tolerances().get(element.id()) : written.apply(element);
        double factor = (1 + conditions.corner() * tolerance.percent() / 100)
                * (1 + tolerance.coefficient() * 1e-6 * (conditions.temperature() - BenchConditions.NOMINAL_TEMPERATURE));
        if (factor == 1) {
            return element;
        }
        return new CircuitElement(element.id(), element.type(), element.positive(), element.negative(),
                "(" + element.value() + ") * " + factor, element.waveform(), element.frequency(), element.component(),
                element.pins(), element.overrides(), element.symbol());
    }

    /**
     * A model as the bench tells it what it is on: its equations read the bench's temperature, in kelvin, and whether the
     * bench is realistic (1) or ideal (0) — an instrument's document decides from that how much it loads what it measures.
     */
    private static CircuitElement onTheBench(CircuitElement element, BenchConditions conditions) {
        if (conditions.temperature() == BenchConditions.NOMINAL_TEMPERATURE && !conditions.realistic() && conditions.corner() == 0) {
            return element;
        }
        Map<String, String> overrides = new LinkedHashMap<>(element.overrides());
        if (conditions.temperature() != BenchConditions.NOMINAL_TEMPERATURE) {
            overrides.put("temperature", Double.toString(conditions.temperature() + 273.15));
        }
        if (conditions.realistic()) {
            overrides.put("realistic", "1");
        }
        if (conditions.corner() != 0) {
            overrides.put("tolerance_corner", Integer.toString(conditions.corner()));
        }
        return new CircuitElement(element.id(), element.type(), element.positive(), element.negative(), element.value(),
                element.waveform(), element.frequency(), element.component(), element.pins(), overrides, element.symbol());
    }

    /** How far apart a failed-open part leaves its pins, and how close a failed-short one ties them, in ohms. */
    private static final String OPEN_OHMS = "1e9";
    private static final String SHORT_OHMS = "1e-3";

    /**
     * A part as it is after it failed, whatever it was: open, each of its nodes left on a gigaohm to ground so none
     * floats; short, every node tied to its first through a milliohm. A part whose nodes are all ground leaves nothing.
     * Only its terminals are read — what the part was does not matter.
     */
    private static List<CircuitElement> failed(CircuitElement element, String failure, Set<String> ids) {
        List<String> pins = element.type() == CircuitElementType.MODEL
                ? List.copyOf(new LinkedHashSet<>(element.pins().values()))
                : List.of(element.positive(), element.negative());
        List<CircuitElement> replacement = new ArrayList<>();
        boolean open = failure.equals("open");
        for (int index = 0; index < pins.size(); index++) {
            // A pin already on ground needs no leak; a short ties every other node to the first, never one to itself.
            if (open ? pins.get(index).equals(Circuit.GROUND) : index == 0 || pins.get(index).equals(pins.getFirst())) {
                continue;
            }
            String id = element.id() + "_failed_" + (index + 1);
            ids.add(id);
            replacement.add(direct(id, CircuitElementType.RESISTOR, open ? pins.get(index) : pins.getFirst(),
                    open ? Circuit.GROUND : pins.get(index), open ? OPEN_OHMS : SHORT_OHMS));
        }
        return replacement;
    }

    private static CircuitElement withPositive(CircuitElement element, String positive) {
        return new CircuitElement(element.id(), element.type(), positive, element.negative(), element.value(),
                element.waveform(), element.frequency(), element.component(), element.pins(), element.overrides(),
                element.symbol());
    }

    private static CircuitElement direct(String id, CircuitElementType type, String positive, String negative, String value) {
        return new CircuitElement(id, type, positive, negative, value, type.isSource() ? SourceWaveform.DC : null, null, null, null, null);
    }
}
