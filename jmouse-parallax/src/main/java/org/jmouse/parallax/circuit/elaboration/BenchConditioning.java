package org.jmouse.parallax.circuit.elaboration;

import org.jmouse.parallax.component.ComponentVocabulary;
import org.jmouse.parallax.component.TemperatureScale;
import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A circuit as it runs on a bench: its {@link BenchConditions} applied to the parts, so that what is simulated is the
 * circuit in its environment and nothing downstream needs to know a bench exists.
 *
 * <ul>
 *   <li>Every part is told the bench's figures — its temperature, whether it is realistic, the corner of its spread it
 *       stands at —; its tolerance and its drift are its component's equations', which read them.</li>
 *   <li>A part the bench gives a tolerance and a coefficient of its own has them as its own parameters.</li>
 *   <li>A source's offset is a DC source of its own: in series with a voltage source, through a node of its own;
 *       beside a current source, between the same nodes.</li>
 *   <li>A failed part is replaced by what it became: open, or shorted through the pins its component names.</li>
 * </ul>
 */
public final class BenchConditioning {

    private BenchConditioning() {
    }

    /**
     * The circuit with its conditions applied and none left on it; a circuit without any, as it is. A part's tolerance
     * and drift are its component's equations' — they read the bench's figures —; the bench only tells every part where
     * it stands, and a part the bench gives its own tolerance that tolerance.
     */
    public static Circuit apply(Circuit circuit) {
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
            BenchConditions.Failure failure = conditions.failures().get(element.id());
            if (failure != null) {
                elements.addAll(failed(element, failure, ids));
                continue;
            }
            CircuitElement placed = withEnvironment(element, conditions);
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

    /**
     * A part on this bench: the bench's figures — its temperature, whether it is realistic, the corner — and, where the
     * bench gives the part a tolerance and a coefficient of its own (in % and ppm/°C), those as the part's own
     * {@code tolerance} and {@code temperature_coefficient}, which its equations read.
     */
    private static CircuitElement withEnvironment(CircuitElement element, BenchConditions conditions) {
        BenchConditions.Tolerance own = conditions.tolerances().get(element.id());
        if (own == null) {
            return onTheBench(element, conditions);
        }
        Map<String, String> overrides = new LinkedHashMap<>(element.overrides() == null ? Map.of() : element.overrides());
        overrides.put("tolerance", Double.toString(own.percent() / 100));
        overrides.put("temperature_coefficient", Double.toString(own.coefficient() * 1e-6));
        return onTheBench(new CircuitElement(element.id(), element.type(), element.positive(), element.negative(), element.value(),
                element.component(), element.pins(), overrides, element.symbol()), conditions);
    }

    /** What a failed part's stand-ins are named after it: `R1_failed_1`. */
    private static final String STAND_IN = "_failed_";

    /** Whether an element is what a failed part became — a stand-in, not a part of the circuit as it was written. */
    public static boolean isStandIn(String id) {
        return id.contains(STAND_IN);
    }

    /**
     * A model as the bench tells it what it is on: its equations read the bench's temperature, in kelvin, and whether the
     * bench is realistic (1) or ideal (0) — an instrument's document decides from that how much it loads what it measures.
     */
    private static CircuitElement onTheBench(CircuitElement element, BenchConditions conditions) {
        if (conditions.temperature() == BenchConditions.NOMINAL_TEMPERATURE && !conditions.realistic() && conditions.corner() == 0
                && conditions.detail() == BenchConditions.Detail.STANDARD) {
            return element;
        }
        Map<String, String> overrides = new LinkedHashMap<>(element.overrides() == null ? Map.of() : element.overrides());
        if (conditions.temperature() != BenchConditions.NOMINAL_TEMPERATURE) {
            overrides.put(ComponentVocabulary.TEMPERATURE, Double.toString(TemperatureScale.kelvin(conditions.temperature())));
        }
        if (conditions.realistic()) {
            overrides.put(ComponentVocabulary.REALISTIC, "1");
        }
        if (conditions.corner() != 0) {
            overrides.put(ComponentVocabulary.TOLERANCE_CORNER, Integer.toString(conditions.corner()));
        }
        if (conditions.detail() != BenchConditions.Detail.STANDARD) {
            overrides.put(ComponentVocabulary.DETAIL, Integer.toString(conditions.detail().figure()));
        }
        return new CircuitElement(element.id(), element.type(), element.positive(), element.negative(), element.value(),
                element.component(), element.pins(), overrides, element.symbol());
    }

    /** How far apart a failed-open part leaves its pins, and how close a failed-short one ties them, in ohms. */
    private static final String OPEN_OHMS = "1e9";
    private static final String SHORT_OHMS = "1e-3";

    /**
     * A part as it is after it failed, as its component says it fails: open, each of its nodes left on a gigaohm to
     * ground so none floats; short, the pins it names — every pin when it names none — tied to the first of them
     * through a milliohm, and the others left open. A part whose nodes are all ground leaves nothing. The ohms are the
     * solver's: a break and a bridge it can hold in a matrix, not a part's physics.
     */
    private static List<CircuitElement> failed(CircuitElement element, BenchConditions.Failure failure, Set<String> ids) {
        Map<String, String> nodes = new LinkedHashMap<>();
        if (element.type() == CircuitElementType.MODEL) {
            nodes.putAll(element.pins());
        } else {
            nodes.put("positive", element.positive());
            nodes.put("negative", element.negative());
        }
        List<String> tied = new ArrayList<>();
        if (failure.shorts()) {
            // A part written in pin order has its two terminals and no names for them: a short ties both.
            boolean named = element.type() == CircuitElementType.MODEL && !failure.pins().isEmpty();
            for (String pin : named ? failure.pins() : nodes.keySet()) {
                String node = nodes.get(pin);
                if (node == null) {
                    throw new IllegalArgumentException(element.id() + " fails short through '" + pin + "', which it has no pin of.");
                }
                tied.add(node);
            }
        }
        List<CircuitElement> replacement = new ArrayList<>();
        Set<String> placed = new LinkedHashSet<>();
        int number = 1;
        for (String node : nodes.values()) {
            // A node is placed once: tied to the short's first node, or left open on its leak.
            if (!placed.add(node)) {
                continue;
            }
            boolean bridged = tied.contains(node);
            if (bridged ? node.equals(tied.getFirst()) : node.equals(Circuit.GROUND)) {
                continue;
            }
            String id = element.id() + STAND_IN + number++;
            ids.add(id);
            replacement.add(bridged ? direct(id, CircuitElementType.RESISTOR, tied.getFirst(), node, SHORT_OHMS)
                    : direct(id, CircuitElementType.RESISTOR, node, Circuit.GROUND, OPEN_OHMS));
        }
        return replacement;
    }

    private static CircuitElement withPositive(CircuitElement element, String positive) {
        return new CircuitElement(element.id(), element.type(), positive, element.negative(), element.value(),
                element.component(), element.pins(), element.overrides(),
                element.symbol());
    }

    private static CircuitElement direct(String id, CircuitElementType type, String positive, String negative, String value) {
        return new CircuitElement(id, type, positive, negative, value, null, null, null);
    }
}
