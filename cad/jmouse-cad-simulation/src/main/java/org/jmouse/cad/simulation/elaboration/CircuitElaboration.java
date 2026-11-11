package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.Setting;
import org.jmouse.cad.circuit.Analysis;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.simulation.SimulatedFamily;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationReading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A circuit as it is written turned, once, into the circuit as it is simulated ({@link ElaboratedCircuit}): its own
 * parameters evaluated, every part's component found in the catalogue and compiled — once per component, however many
 * parts are of it —, every part's pins wired, by name or in its component's pin order, its bench applied
 * ({@link BenchConditioning}), and every part made of parts replaced by them — a sheet placed as a block
 * ({@link SheetHierarchy}) before anything is read, a component made of a subcircuit ({@link ComponentHierarchy}) after.
 */
public final class CircuitElaboration {

    private CircuitElaboration() {
    }

    /** The circuit as it is simulated on a bench, its components found in {@code catalogue}. */
    public static ElaboratedCircuit of(Circuit circuit, CatalogueDocuments catalogue, BenchConditions bench) {
        return of(circuit, catalogue, bench, List.of());
    }

    /**
     * The same, with what a bench fixture puts in beside the parts — a current into a diode, a voltage across it — as
     * elements of the engine's own.
     */
    public static ElaboratedCircuit of(Circuit circuit, CatalogueDocuments catalogue, BenchConditions bench,
                                       List<EngineElement> fixtures) {
        String title = circuit.head().title();
        Map<String, Double> values = new LinkedHashMap<>();
        for (EvaluatedParameter parameter : ParameterEvaluation.evaluate(title, circuit.parameters())) {
            values.put(parameter.name(), parameter.value());
        }
        SheetHierarchy.Unfolded unfolded = SheetHierarchy.unfold(circuit, values, catalogue);
        Map<String, SimulatedComponent> compiled = new HashMap<>();
        Map<String, SimulatedFamily> families = new HashMap<>();
        List<ElaboratedPart> parts = new ArrayList<>();
        for (Part part : unfolded.circuit().parts()) {
            SimulatedComponent component = compiled.computeIfAbsent(part.component(), slug -> compile(part, slug, catalogue));
            SimulatedFamily family = family(part, component, catalogue, families);
            parts.add(new ElaboratedPart(part.reference(), component, connections(part, component), family, part.settings(),
                    Map.of(), reportedAs(part, component)));
        }
        double duration = circuit.run().map(Analysis::duration).orElse(0.0);
        ElaboratedCircuit conditioned = BenchConditioning.apply(new ElaboratedCircuit(parts, fixtures, values, bench, duration,
                new ElaboratedCircuit.Written(parts, fixtures), List.of()));
        ElaboratedCircuit flattened = ComponentHierarchy.flatten(conditioned, new ComponentHierarchy.Parts() {
            @Override
            public SimulatedComponent component(Part part, String slug) {
                return compiled.computeIfAbsent(slug, ignored -> compile(part, slug, catalogue));
            }

            @Override
            public ElaboratedPart elaborated(Part part, SimulatedComponent component, Map<String, Double> conditioned,
                                             List<Setting> settings) {
                return new ElaboratedPart(part.reference(), component, connections(part, component),
                        family(part, component, catalogue, families), settings, conditioned, reportedAs(part, component));
            }
        });
        return withBlocks(flattened, unfolded.blocks());
    }

    /**
     * The circuit with each block a placed sheet became reported as the parts it is made of, as a part made of a subcircuit
     * is: every part it became — however deep, a part that is itself made of parts by those —, each with its pins' nets.
     */
    private static ElaboratedCircuit withBlocks(ElaboratedCircuit circuit, List<SheetHierarchy.Block> blocks) {
        if (blocks.isEmpty()) {
            return circuit;
        }
        Map<String, Assembly> assembled = new LinkedHashMap<>();
        circuit.assemblies().forEach(assembly -> assembled.put(assembly.part(), assembly));
        Map<String, ElaboratedPart> byReference = new LinkedHashMap<>();
        circuit.parts().forEach(part -> byReference.put(part.reference(), part));
        List<Assembly> assemblies = new ArrayList<>(circuit.assemblies());
        for (SheetHierarchy.Block block : blocks) {
            Map<String, Map<String, String>> members = new LinkedHashMap<>();
            for (String member : block.members()) {
                Assembly inner = assembled.get(member);
                if (inner != null) {
                    members.putAll(inner.members());
                } else if (byReference.containsKey(member)) {
                    members.put(member, byReference.get(member).connections());
                }
            }
            Assembly assembly = new Assembly(block.block(), block.pins(), members);
            assemblies.add(assembly);
            assembled.put(block.block(), assembly);
        }
        return new ElaboratedCircuit(circuit.parts(), circuit.elements(), circuit.values(), circuit.bench(), circuit.duration(),
                circuit.written(), assemblies);
    }

    private static SimulatedComponent compile(Part part, String slug, CatalogueDocuments catalogue) {
        DocumentNode document = catalogue.find(slug).orElseThrow(() -> new IllegalArgumentException(
                "'" + part.reference() + "' is a '" + slug + "', which the catalogue does not hold."));
        return ComponentCompilation.compile(SimulationReading.component(document), catalogue);
    }

    /** The logic family a part with digital pins follows, read once; null for a part with none. */
    private static SimulatedFamily family(Part part, SimulatedComponent component, CatalogueDocuments catalogue, Map<String, SimulatedFamily> families) {
        if (component.pins().stream().noneMatch(Pin::digital)) {
            return null;
        }
        String slug = component.logicFamily().orElseThrow(() -> new IllegalArgumentException(
                "'" + part.reference() + "': " + component.title() + " has digital pins and names no logic family."));
        return families.computeIfAbsent(slug, name -> ComponentCompilation.family(SimulationReading.family(catalogue.find(name).orElseThrow(() ->
                new IllegalArgumentException(component.title() + " names the logic family '" + name + "', which does not exist."))), catalogue));
    }

    /**
     * What the circuit calls each pin of a part, where that is not the pin's name: a two-terminal part wired in pin order
     * — {@code R1 resistor (in, out)} — is reached as {@code R1.positive} and {@code R1.negative}
     * ({@link SimulationVocabulary#IN_ORDER_TERMINALS}), its component's first pin and its second; a part wired by name
     * is reached by its pins' own names.
     */
    private static Map<String, String> reportedAs(Part part, SimulatedComponent component) {
        List<String> terminals = SimulationVocabulary.IN_ORDER_TERMINALS;
        if (!part.inPinOrder() || part.connections().size() != terminals.size() || component.pins().size() < terminals.size()) {
            return Map.of();
        }
        Map<String, String> reported = new LinkedHashMap<>();
        for (int index = 0; index < terminals.size(); index++) {
            reported.put(component.pins().get(index).name(), terminals.get(index));
        }
        return reported;
    }

    /**
     * Each wired pin's net: as written for a part wired by name — {@code [positive: in, negative: 0]} —, in the order of
     * its component's pins for one wired in pin order — {@code [in, out]}.
     */
    private static Map<String, String> connections(Part part, SimulatedComponent component) {
        Map<String, String> connections = new LinkedHashMap<>();
        if (!part.inPinOrder()) {
            for (Connection connection : part.connections()) {
                connections.put(connection.pin(), connection.net());
            }
            return connections;
        }
        List<Pin> pins = component.pins();
        if (part.connections().size() > pins.size()) {
            throw new IllegalArgumentException("'" + part.reference() + "' is wired to " + part.connections().size() + " nets, and "
                    + component.title() + " has " + pins.size() + " pins.");
        }
        for (int index = 0; index < part.connections().size(); index++) {
            connections.put(pins.get(index).name(), part.connections().get(index).net());
        }
        return connections;
    }
}
