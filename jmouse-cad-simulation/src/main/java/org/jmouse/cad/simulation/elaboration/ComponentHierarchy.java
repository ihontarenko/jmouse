package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Setting;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulatedComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every part made of a subcircuit replaced by the parts it is made of, however deep, in one pass that also says what
 * each such part became ({@link Assembly}). A part of a subcircuit is named after its parent and its own reference —
 * {@code BR1_D1} —, one deeper {@code BR1_D1_X1}; a net inside it is the parent's own, {@code BR1.mid}; a pin of the
 * subcircuit is the net its parent's pin is wired to; what a part inside sets is worked out over its parent's
 * parameters. A part that also does something itself — equations, states, behaviour — is kept beside its parts.
 */
final class ComponentHierarchy {

    /** What joins a parent's reference to what is its own inside it: its parts, {@code BR1_D1}. */
    private static final String CHILD_SEPARATOR = "_";

    /** Where a part's components and families are found, compiled once. */
    interface Parts {

        SimulatedComponent component(Part part, String slug);

        ElaboratedPart elaborated(Part part, SimulatedComponent component, Map<String, Double> conditioned, List<Setting> settings);
    }

    private final ElaboratedCircuit circuit;
    private final Parts parts;
    private final List<ElaboratedPart> flattened = new ArrayList<>();
    private final Set<String> references = new HashSet<>();
    /** The part on the sheet being flattened now, and what each such part became — its flattened parts' pins. */
    private final Map<String, Map<String, Map<String, String>>> members = new LinkedHashMap<>();
    private ElaboratedPart top;
    private int nextNumber = 1;

    private ComponentHierarchy(ElaboratedCircuit circuit, Parts parts) {
        this.circuit = circuit;
        this.parts = parts;
        circuit.parts().forEach(part -> references.add(part.reference()));
    }

    /** The circuit with every part made of parts flattened; as it is when it holds none. */
    static ElaboratedCircuit flatten(ElaboratedCircuit circuit, Parts parts) {
        if (circuit.parts().stream().noneMatch(part -> part.component().subcircuit())) {
            return circuit;
        }
        ComponentHierarchy hierarchy = new ComponentHierarchy(circuit, parts);
        for (ElaboratedPart part : circuit.parts()) {
            hierarchy.top = part;
            hierarchy.add(part, new LinkedHashSet<>());
        }
        List<Assembly> assemblies = new ArrayList<>();
        for (ElaboratedPart part : circuit.parts()) {
            Map<String, Map<String, String>> became = hierarchy.members.getOrDefault(part.reference(), Map.of());
            if (!became.containsKey(part.reference())) {
                assemblies.add(new Assembly(part.reference(), part.connections(), became));
            }
        }
        return new ElaboratedCircuit(hierarchy.flattened, circuit.elements(), circuit.values(), circuit.bench(),
                circuit.duration(), circuit.written(), assemblies);
    }

    private void keep(ElaboratedPart part) {
        flattened.add(part);
        members.computeIfAbsent(top.reference(), ignored -> new LinkedHashMap<>()).put(part.reference(), part.connections());
    }

    private void add(ElaboratedPart part, Set<String> path) {
        SimulatedComponent component = part.component();
        if (!component.subcircuit()) {
            keep(part);
            return;
        }
        if (!path.add(component.slug())) {
            throw new IllegalArgumentException("Recursive component: " + String.join(" -> ", path) + " -> " + component.slug());
        }
        if (component.unitNames().size() > 1) {
            throw new IllegalArgumentException(component.title() + ": a subcircuit is one unit; split a package of several "
                    + "blocks into single-block components");
        }
        Map<String, Double> parameters = circuit.parameters(part, "");
        if (component.actsItself()) {
            keep(part);
        }
        boolean alone = component.parts().size() == 1 && !component.actsItself();
        for (Part line : component.parts()) {
            SimulatedComponent child = parts.component(line, line.component());
            Map<String, Double> conditioned = new LinkedHashMap<>();
            List<Setting> texts = new ArrayList<>();
            for (Setting setting : line.settings()) {
                if (setting.value() instanceof Formula formula) {
                    conditioned.put(setting.name(), ParameterEvaluation.value(component.title() + ": " + line.reference()
                            + "'s " + setting.name(), formula, parameters));
                } else {
                    texts.add(setting);
                }
            }
            String reference = alone ? part.reference() : childReference(part, line);
            Part placed = new Part(reference, line.component(), line.connections(), texts, line.symbol(), line.footprint());
            ElaboratedPart elaborated = parts.elaborated(placed, child, conditioned, texts);
            Map<String, String> nets = new LinkedHashMap<>();
            elaborated.connections().forEach((pin, local) -> nets.put(pin, net(part, local)));
            add(new ElaboratedPart(reference, child, nets, elaborated.family(), texts, conditioned, elaborated.reportedAs()), path);
        }
        path.remove(component.slug());
    }

    /** The reference a part inside a subcircuit takes: its parent's and its own, {@code BR1_D1}, never one taken. */
    private String childReference(ElaboratedPart parent, Part line) {
        String candidate = parent.reference() + CHILD_SEPARATOR + line.reference();
        while (!references.add(candidate)) {
            candidate = parent.reference() + CHILD_SEPARATOR + line.reference() + CHILD_SEPARATOR + nextNumber++;
        }
        return candidate;
    }

    /** A net inside a subcircuit as the circuit knows it: a pin's net outside, ground, or the parent's own. */
    private static String net(ElaboratedPart parent, String local) {
        if (local.equals(Circuit.GROUND)) {
            return Circuit.GROUND;
        }
        return parent.connections().getOrDefault(local, parent.reference() + ElaboratedCircuit.UNIT_SEPARATOR + local);
    }
}
