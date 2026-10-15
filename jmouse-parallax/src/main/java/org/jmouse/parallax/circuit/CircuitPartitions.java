package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentHierarchy;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A circuit cut into the parts that do not affect one another, so each can be solved on its own, with its own step.
 *
 * <p>The cut is exact, not an approximation. A node an ideal voltage source holds against ground has that source's
 * voltage whatever is connected to it — so the parts that meet only there, and at ground, are independent: what one
 * draws from the node changes nothing the other sees. Each part gets its own copy of every such source it touches.</p>
 *
 * <p>Why it matters: a circuit is solved with one time step for all of it, and every edge anywhere shortens that step
 * everywhere. Ten timers on one supply cost ten edges' worth of small steps for each of them — a hundred times one
 * timer, where they should cost ten. A global-step simulator (SPICE among them) pays that; split, it is linear.</p>
 */
final class CircuitPartitions {

    private CircuitPartitions() {
    }

    /** The parts of the circuit — the circuit itself when it does not split. */
    static List<CompiledObject> of(CompiledObject original) {
        CompiledObject object = ComponentHierarchy.expand(original);
        Circuit circuit = (Circuit) object.definition();
        Map<String, CircuitElement> holders = new LinkedHashMap<>();
        for (CircuitElement element : circuit.elements()) {
            String held = heldNode(element, object);
            if (held != null) {
                holders.putIfAbsent(held, element);
            }
        }
        if (holders.isEmpty()) {
            return List.of(object);
        }

        // Union of the nodes every other element joins; held nodes and ground join nothing.
        Map<String, String> parent = new HashMap<>();
        Set<CircuitElement> sources = new HashSet<>(holders.values());
        List<CircuitElement> parts = circuit.elements().stream().filter(element -> !sources.contains(element)).toList();
        for (CircuitElement element : parts) {
            List<String> joined = nodesOf(element).stream().filter(node -> !Circuit.GROUND.equals(node) && !holders.containsKey(node)).toList();
            for (int index = 1; index < joined.size(); index++) {
                union(parent, joined.getFirst(), joined.get(index));
            }
            joined.forEach(node -> find(parent, node));
        }

        Map<String, List<CircuitElement>> groups = new LinkedHashMap<>();
        List<CircuitElement> loose = new ArrayList<>();
        for (CircuitElement element : parts) {
            String node = nodesOf(element).stream().filter(candidate -> !Circuit.GROUND.equals(candidate) && !holders.containsKey(candidate))
                    .findFirst().orElse(null);
            if (node == null) {
                loose.add(element);
            } else {
                groups.computeIfAbsent(find(parent, node), ignored -> new ArrayList<>()).add(element);
            }
        }
        if (groups.size() <= 1) {
            return List.of(object);
        }
        // An element only between held nodes and ground (a load across the supply) belongs to the first part.
        groups.values().iterator().next().addAll(loose);

        List<CompiledObject> split = new ArrayList<>();
        for (List<CircuitElement> group : groups.values()) {
            Set<String> touched = new LinkedHashSet<>();
            group.forEach(element -> touched.addAll(nodesOf(element)));
            List<CircuitElement> elements = new ArrayList<>();
            holders.forEach((node, source) -> {
                if (touched.contains(node)) {
                    elements.add(source);
                }
            });
            elements.addAll(group);
            split.add(object.withDefinition(new Circuit(elements, circuit.time(), null)));
        }
        return split;
    }

    /** The node an ideal voltage source — a primitive one or a generator — holds against ground, or null for anything else. */
    private static String heldNode(CircuitElement element, CompiledObject object) {
        String positive;
        String negative;
        if (element.type() == CircuitElementType.VOLTAGE_SOURCE) {
            positive = element.positive();
            negative = element.negative();
        } else if (element.type() == CircuitElementType.MODEL) {
            ComponentModel model = ComponentDocuments.read(object.componentSource(element.component()), element.component(), object.library());
            if (!SourceSignal.isGenerator(model)) {
                return null;
            }
            positive = element.pins().get(SourceSignal.positivePin(model));
            negative = element.pins().get(SourceSignal.negativePin(model));
        } else {
            return null;
        }
        if (Circuit.GROUND.equals(negative) && positive != null && !Circuit.GROUND.equals(positive)) {
            return positive;
        }
        if (Circuit.GROUND.equals(positive) && negative != null && !Circuit.GROUND.equals(negative)) {
            return negative;
        }
        return null;
    }

    private static List<String> nodesOf(CircuitElement element) {
        if (element.type() == CircuitElementType.MODEL) {
            return List.copyOf(element.pins().values());
        }
        return List.of(element.positive(), element.negative());
    }

    private static String find(Map<String, String> parent, String node) {
        parent.putIfAbsent(node, node);
        String root = parent.get(node);
        if (!root.equals(node)) {
            root = find(parent, root);
            parent.put(node, root);
        }
        return root;
    }

    private static void union(Map<String, String> parent, String first, String second) {
        String left = find(parent, first);
        String right = find(parent, second);
        if (!left.equals(right)) {
            parent.put(right, left);
        }
    }
}
