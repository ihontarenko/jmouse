package org.jmouse.parallax.circuit.elaboration;

import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A circuit as the engine simulates it, worked out once from what was written: its bench applied
 * ({@link BenchConditioning}), every part made of a subcircuit replaced by the parts it is made of
 * ({@link ComponentHierarchy}), every part written in pin order bound as its document's model, and every component it
 * uses read. A netlist, the pieces it is cut into, a part's limits, its displays and its instruments are all built from
 * this one — none of them reads a document or applies a bench again.
 */
public final class ElaboratedCircuit {

    private final CompiledObject              written;
    /** The circuit flattened, in whose parameters every written value is read. */
    private final CompiledObject              object;
    /** What is solved: the flattened parts, each bound as its document's model or left to the engine. */
    private final Circuit                     circuit;
    /** The parts as written, each on the bench: what a part's own readings are read over. */
    private final Circuit                     conditioned;
    private final List<ComponentHierarchy.Assembly> assemblies;
    /** Every component the circuit uses, by reference; shared by every piece of it. */
    private final Map<String, ComponentModel> models;
    /** What each part written in pin order reports its pins' currents under: the terminals it is drawn with. */
    private final Map<String, Map<String, String>> reportedAs;

    private ElaboratedCircuit(CompiledObject written, CompiledObject object, Circuit circuit, Circuit conditioned,
                              List<ComponentHierarchy.Assembly> assemblies, Map<String, ComponentModel> models,
                              Map<String, Map<String, String>> reportedAs) {
        this.written = written;
        this.object = object;
        this.circuit = circuit;
        this.conditioned = conditioned;
        this.assemblies = assemblies;
        this.models = models;
        this.reportedAs = reportedAs;
    }

    /** The circuit {@code written} as it is simulated. */
    public static ElaboratedCircuit of(CompiledObject written) {
        ComponentHierarchy.Expansion expansion = ComponentHierarchy.expand(written);
        CompiledObject object = expansion.object();
        Map<String, ComponentModel> models = new LinkedHashMap<>();
        Map<String, Map<String, String>> reportedAs = new LinkedHashMap<>();
        ElaboratedCircuit reading = new ElaboratedCircuit(written, object, null, expansion.conditioned(),
                expansion.assemblies(), models, reportedAs);
        Circuit circuit = reading.asModels((Circuit) object.definition());
        for (CircuitElement element : circuit.elements()) {
            if (element.type() == CircuitElementType.MODEL) {
                reading.model(element.component());
            }
        }
        return new ElaboratedCircuit(written, object, circuit, expansion.conditioned(), expansion.assemblies(), models,
                reportedAs);
    }

    /** One piece of this circuit — some of its parts — on the same bench, reading the same components. */
    public ElaboratedCircuit piece(List<CircuitElement> elements) {
        return new ElaboratedCircuit(written, object, new Circuit(elements, circuit.time(), null), conditioned, assemblies,
                models, reportedAs);
    }

    /** The circuit as it was written, its bench on it. */
    public CompiledObject written() {
        return written;
    }

    /** What is solved. */
    public Circuit circuit() {
        return circuit;
    }

    /** The parts as written, each on the bench — a failed part as what it became. */
    public Circuit conditioned() {
        return conditioned;
    }

    /** The parts on the sheet simulated as the parts they are made of. */
    public List<ComponentHierarchy.Assembly> assemblies() {
        return assemblies;
    }

    /** The bench the circuit was written on; null for an ideal one at room temperature. */
    public BenchConditions bench() {
        return ((Circuit) written.definition()).conditions();
    }

    /** A written value read in the circuit's parameters: a quantity, or an expression of them. */
    public double value(String written) {
        return object.value(written);
    }

    /** A component the circuit uses, read once. */
    public ComponentModel model(String reference) {
        ComponentModel known = models.get(reference);
        if (known != null) {
            return known;
        }
        ComponentModel model = ComponentDocuments.read(object.componentSource(reference), reference, object.library());
        models.put(reference, model);
        return model;
    }

    /** A logic family a component names, read once; empty when the library does not hold it. */
    public Optional<ComponentModel> family(String name) {
        ComponentModel known = models.get(name);
        if (known != null) {
            return Optional.of(known);
        }
        return object.library().source(name).map(source -> {
            ComponentModel family = ComponentDocuments.read(source, name, object.library());
            models.put(name, family);
            return family;
        });
    }

    /** What a part written in pin order reports its pins' currents under; empty for any other. */
    public Map<String, String> reportedAs(String part) {
        return reportedAs.getOrDefault(part, Map.of());
    }

    /** One unit's parameters, as {@link PartParameters} reads them in this circuit's parameters. */
    public Map<String, Double> parameters(ComponentModel model, CircuitElement element, String unit) {
        return PartParameters.resolve(model, element, unit, this::value);
    }

    /** A part's text parameters, its overrides' braces read over the circuit's parameters as its numbers are. */
    public Map<String, String> texts(ComponentModel model, CircuitElement element) {
        return PartParameters.texts(model, element, this::value);
    }

    /**
     * Every part as written, on the bench: its own readings — instruments, displays, limits — are read over these. A
     * failed part is there as it was written, its readings what they were.
     */
    public List<CircuitElement> writtenParts() {
        Map<String, CircuitElement> parts = new LinkedHashMap<>();
        ((Circuit) written.definition()).elements().forEach(element -> parts.put(element.id(), element));
        conditioned.elements().forEach(element -> parts.computeIfPresent(element.id(), (id, writtenPart) -> element));
        return List.copyOf(parts.values());
    }

    /** The component a written part is: its own, or a primitive's document; empty for a primitive without one. */
    public Optional<ComponentModel> componentOf(CircuitElement element) {
        String reference = element.type() == CircuitElementType.MODEL ? element.component() : element.type().primitiveComponent();
        if (reference == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(model(reference));
        } catch (IllegalArgumentException unreadable) {
            return Optional.empty();
        }
    }

    /**
     * The circuit with every primitive whose document writes its physics — a resistor, a capacitor — bound as a part of
     * that document: its pins in order on the two nodes it was written on, its value as the document's first parameter.
     * A failed part's stand-ins are the engine's own, and stay.
     */
    private Circuit asModels(Circuit flattened) {
        Map<CircuitElementType, Optional<ComponentModel>> documents = new EnumMap<>(CircuitElementType.class);
        List<CircuitElement> elements = new ArrayList<>();
        for (CircuitElement element : flattened.elements()) {
            Optional<ComponentModel> document = element.type() == CircuitElementType.MODEL || BenchConditioning.isStandIn(element.id())
                    ? Optional.empty()
                    : documents.computeIfAbsent(element.type(), this::modelledDocument);
            if (document.isEmpty()) {
                elements.add(element);
                continue;
            }
            ComponentModel model = document.get();
            requireValue(element, value(element.value()));
            String first = model.pins().get(0).name();
            String second = model.pins().get(1).name();
            Map<String, String> pins = new LinkedHashMap<>();
            pins.put(first, element.positive());
            pins.put(second, element.negative());
            Map<String, String> overrides = new LinkedHashMap<>(element.overrides() == null ? Map.of() : element.overrides());
            // A setting named for the first parameter wins over the written value, as on a model: 'with R1.resistance = …'.
            overrides.putIfAbsent(model.parameters().getFirst().name(), element.value());
            String[] terminals = terminalNamesOf(element);
            reportedAs.put(element.id(), Map.of(first, terminals[0], second, terminals[1]));
            elements.add(new CircuitElement(element.id(), CircuitElementType.MODEL, null, null, null,
                    element.type().primitiveComponent(), pins, overrides, element.symbol()));
        }
        return new Circuit(elements, flattened.time(), flattened.layout(), flattened.conditions(), flattened.probes());
    }

    /** A written value that can stand: finite, and above nought for a part that is not a source. */
    public static void requireValue(CircuitElement element, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("The value of '" + element.id() + "' is not a finite number.");
        }
        boolean passive = !element.type().isSource();
        if (passive && value <= 0) {
            throw new IllegalArgumentException("'" + element.id() + "' needs a positive value; it is " + value + ".");
        }
    }

    /**
     * The names a primitive's terminals are reported by: the component's pins when it stands for one — the pin on its
     * positive node first —, else "+" and "−".
     */
    private static String[] terminalNamesOf(CircuitElement element) {
        String[] primitive = {ObjectDefinition.PRIMITIVE_POSITIVE, ObjectDefinition.PRIMITIVE_NEGATIVE};
        if (element.pins() == null || element.pins().size() != 2) {
            return primitive;
        }
        String first = element.pins().entrySet().stream().filter(pin -> pin.getValue().equals(element.positive()))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        String second = element.pins().keySet().stream().filter(name -> !name.equals(first)).findFirst().orElse(null);
        return first == null || second == null ? primitive : new String[]{first, second};
    }

    /** A primitive's document, when it writes the physics of its two pins. */
    private Optional<ComponentModel> modelledDocument(CircuitElementType type) {
        ComponentModel model = model(type.primitiveComponent());
        return model.equations().isEmpty() || model.pins().size() != 2 || model.parameters().isEmpty()
                ? Optional.empty() : Optional.of(model);
    }
}
