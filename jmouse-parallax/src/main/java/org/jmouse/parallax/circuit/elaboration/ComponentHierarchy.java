package org.jmouse.parallax.circuit.elaboration;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.ObjectDefinition;

import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ComponentVocabulary;
import org.jmouse.parallax.circuit.elaboration.PartParameters;
import org.jmouse.parallax.component.ComponentModel.SubcircuitLine;
import org.jmouse.parallax.script.Quantity;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.Symbols;
import org.jmouse.formula.el.FormulaLanguage;
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
import java.util.TreeMap;

/** Resolves a circuit's complete pinned jMC dependency tree and flattens subcircuit instances for the solver. */
public final class ComponentHierarchy {

    private ComponentHierarchy() {
    }

    public static Map<String, String> sources(CompiledObject object) {
        if (!(object.definition() instanceof Circuit circuit)) {
            return Map.of();
        }
        Map<String, String> sources = new TreeMap<>();
        for (CircuitElement element : circuit.elements()) {
            if (element.type() == CircuitElementType.MODEL) {
                collect(object, element.component(), sources, new LinkedHashSet<>());
            }
        }
        return sources;
    }

    private static void collect(CompiledObject object, String reference, Map<String, String> sources, Set<String> path) {
        enterComponent(path, reference);
        String source = object.componentSource(reference);
        sources.put(reference, source);
        ComponentModel model = ComponentDocuments.read(source, reference, object.library());
        for (SubcircuitLine line : model.subcircuit()) {
            if (line.type() == CircuitElementType.MODEL) {
                collect(object, line.reference(), sources, path);
            }
        }
        path.remove(reference);
    }

    /** Steps into a component on the way down its hierarchy, refusing one that is already on the way. */
    private static void enterComponent(Set<String> path, String reference) {
        if (!path.add(reference)) {
            throw new IllegalArgumentException("Recursive component: " + String.join(" -> ", path) + " -> " + reference);
        }
    }

    /**
     * A circuit as it is simulated, and what it was on the way.
     *
     * @param object      the circuit as it is simulated: its bench conditions applied, every part made of a subcircuit
     *                    replaced by the parts it is made of
     * @param conditioned the parts as written, each on the bench: what a part's own readings — its instruments, its
     *                    displays, its limits — are read over
     * @param assemblies  the parts on the sheet simulated as the parts they are made of; none for a circuit without one
     */
    public record Expansion(CompiledObject object, Circuit conditioned, List<Assembly> assemblies) {
        public Expansion {
            assemblies = List.copyOf(assemblies);
        }
    }

    /**
     * The circuit as it is simulated: its bench conditions applied ({@link BenchConditioning}), then every part made of
     * a subcircuit replaced by the parts it is made of — in one pass, which also says what each such part became.
     */
    public static Expansion expand(CompiledObject written) {
        Circuit writtenCircuit = (Circuit) written.definition();
        writtenCircuit.requireSolvable();
        CompiledObject object = conditioned(written, writtenCircuit);
        Circuit circuit = (Circuit) object.definition();
        if (!hasSubcircuitPart(object)) {
            return new Expansion(object, circuit, List.of());
        }
        Builder builder = built(object);
        Circuit flattened = new Circuit(builder.elements, circuit.time());
        List<Assembly> assemblies = new ArrayList<>();
        builder.members.forEach((top, members) -> {
            if (!members.containsKey(top.id())) {
                assemblies.add(new Assembly(top.id(), top.pins(), members));
            }
        });
        return new Expansion(DefinitionCompiler.compile(flattened, object.parameters(), object.parameterValues(), object.library()),
                circuit, assemblies);
    }

    /** The circuit with its bench conditions applied, each part's tolerance as its component writes it. */
    private static CompiledObject conditioned(CompiledObject written, Circuit writtenCircuit) {
        if (writtenCircuit.conditions() == null) {
            return written;
        }
        return written.withDefinition(BenchConditioning.apply(writtenCircuit));
    }

    /** Whether any component the circuit uses, however deep, is made of a subcircuit. */
    private static boolean hasSubcircuitPart(CompiledObject object) {
        return sources(object).values().stream().anyMatch(source ->
                !ComponentDocuments.read(source, "component", object.library()).subcircuit().isEmpty());
    }

    /** Every part of the circuit flattened, each remembered under the part on the sheet it came from. */
    private static Builder built(CompiledObject object) {
        Builder builder = new Builder(object);
        for (CircuitElement element : ((Circuit) object.definition()).elements()) {
            builder.top = element;
            builder.add(element, object.parameterValues(), new LinkedHashSet<>());
        }
        return builder;
    }

    /**
     * A part on the sheet that is simulated as the parts it is made of — a subcircuit of several —: the nets its pins
     * are on, and the terminals of every part it is made of, each with its net. What it reports is theirs, added up.
     *
     * @param pins    each of its pins → the net it is on
     * @param members each part it is made of → each of that part's terminals → its net
     */
    public record Assembly(String part, Map<String, String> pins, Map<String, Map<String, String>> members) {
        public Assembly {
            pins = Map.copyOf(pins);
            members = Map.copyOf(members);
        }

        /**
         * The current into each of its pins: what flows into the terminals of its parts that sit on that pin's net —
         * the current that crosses its boundary there. A terminal on an inner net crosses nothing and counts for no pin.
         */
        public Map<String, Double> pinCurrents(Map<String, Map<String, Double>> terminals) {
            Map<String, Double> currents = new LinkedHashMap<>();
            members.forEach((member, nets) -> {
                Map<String, Double> into = terminals.get(member);
                if (into == null) {
                    return;
                }
                nets.forEach((terminal, net) -> {
                    Double current = into.get(terminal);
                    String pin = pinOn(net);
                    if (current != null && pin != null) {
                        currents.merge(pin, current, Double::sum);
                    }
                });
            });
            return currents;
        }

        private String pinOn(String net) {
            return pins.entrySet().stream().filter(pin -> pin.getValue().equals(net)).map(Map.Entry::getKey).sorted().findFirst().orElse(null);
        }
    }

    /** Each terminal of a flattened part → the net it is on, by the names its readings use. */
    private static Map<String, String> terminalNets(CircuitElement element) {
        if (element.type() == CircuitElementType.MODEL || !element.pins().isEmpty()) {
            return element.pins();
        }
        Map<String, String> nets = new LinkedHashMap<>();
        nets.put(ObjectDefinition.PRIMITIVE_POSITIVE, element.positive());
        nets.put(ObjectDefinition.PRIMITIVE_NEGATIVE, element.negative());
        return nets;
    }


    private static final class Builder {
        final CompiledObject object;
        final List<CircuitElement> elements = new ArrayList<>();
        final Set<String> ids = new HashSet<>();
        /** The part on the sheet being flattened now, and what each such part became — its flattened parts' terminals. */
        CircuitElement top;
        final Map<CircuitElement, Map<String, Map<String, String>>> members = new LinkedHashMap<>();
        int nextId = 1;

        Builder(CompiledObject object) {
            this.object = object;
            ((Circuit) object.definition()).elements().forEach(element -> ids.add(element.id()));
        }

        /**
         * The id a part of a subcircuit takes: a component that is one primitive keeps its own part's id — R1 written as
         * the resistor component is the resistor R1, its current reported as R1's —; a part of a larger subcircuit is
         * named after its parent and its own line (`BR1_D1`), never a bare number, so what it carries can be traced back.
         */
        String childId(CircuitElement parent, ComponentModel model, SubcircuitLine line) {
            boolean alone = model.subcircuit().size() == 1 && model.equations().isEmpty() && model.behaviour() == null
                    && model.states().isEmpty();
            if (alone) {
                return parent.id();
            }
            String candidate = parent.id() + "_" + line.id();
            while (!ids.add(candidate)) {
                candidate = parent.id() + "_" + line.id() + "_" + nextId++;
            }
            return candidate;
        }

        /** A part as it is simulated, remembered under the part on the sheet it came from. */
        void keep(CircuitElement element) {
            elements.add(element);
            members.computeIfAbsent(top, ignored -> new LinkedHashMap<>()).put(element.id(), terminalNets(element));
        }

        void add(CircuitElement element, Map<String, Double> environment, Set<String> path) {
            if (element.type() != CircuitElementType.MODEL) {
                keep(element);
                return;
            }
            String reference = element.component();
            enterComponent(path, reference);
            ComponentModel model = ComponentDocuments.read(object.componentSource(reference), reference, object.library());
            if (model.subcircuit().isEmpty()) {
                keep(element);
                path.remove(reference);
                return;
            }
            if (model.unitNames().size() > 1) {
                throw new IllegalArgumentException(reference + ": a subcircuit is one unit; split multi-unit packages into single-unit models");
            }
            // The bench's figures are every component's too: a part made of parts on a bench at 85 °C is told so, as its
            // own parts are below.
            Map<String, Double> parameters = PartParameters.resolve(model, element, "", expression -> value(expression, environment));
            if (!model.equations().isEmpty() || model.behaviour() != null || !model.states().isEmpty()) {
                keep(element);
            }
            for (SubcircuitLine line : model.subcircuit()) {
                if (line.type() == CircuitElementType.MODEL) {
                    String childReference = line.reference();
                    ComponentModel child = ComponentDocuments.read(object.componentSource(childReference), childReference, object.library());
                    Map<String, String> pins = new LinkedHashMap<>();
                    Map<String, String> overrides = new LinkedHashMap<>();
                    line.bindings().forEach((name, expression) -> {
                        String plain = name.contains(".") ? name.substring(name.indexOf('.') + 1) : name;
                        if (child.pin(plain).isPresent()) {
                            pins.put(name, node(element, expression));
                        } else if (child.parameter(plain).isPresent()) {
                            overrides.put(name, Double.toString(value(expression, parameters)));
                        } else {
                            throw new IllegalArgumentException(childReference + ": '" + name + "' is neither a pin nor parameter");
                        }
                    });
                    // Its parts stand on the same bench as it does.
                    ComponentVocabulary.nominalBench().keySet().forEach(name -> {
                        if (element.overrides().containsKey(name)) {
                            overrides.putIfAbsent(name, element.overrides().get(name));
                        }
                    });
                    add(new CircuitElement(childId(element, model, line), CircuitElementType.MODEL, null, null, null,
                            childReference, pins, overrides), parameters, path);
                } else {
                    CircuitElementType kind = line.type();
                    String positive = node(element, line.positive());
                    String negative = node(element, line.negative());
                    String magnitude = line.value();
                    // A primitive that is the whole component keeps the component's pin names, so its current is
                    // reported into terminal_one and terminal_two, as the part is drawn and as a meter names them.
                    String id = childId(element, model, line);
                    Map<String, String> named = id.equals(element.id()) && !line.positive().equals(line.negative())
                            ? Map.of(line.positive(), positive, line.negative(), negative) : null;
                    keep(new CircuitElement(id, kind, positive, negative,
                            Double.toString(value(magnitude, parameters)), null, named, null));
                }
            }
            path.remove(reference);
        }

        private static String node(CircuitElement parent, String local) {
            if (local.equals(Circuit.GROUND)) {
                return Circuit.GROUND;
            }
            return parent.pins().getOrDefault(local, parent.id() + "." + local);
        }

        private static double value(String expression, Map<String, Double> parameters) {
            if (Quantity.isQuantity(expression)) {
                return Quantity.valueOf(expression);
            }
            List<String> names = List.copyOf(parameters.keySet());
            double[] values = names.stream().mapToDouble(parameters::get).toArray();
            return RealCompiler.compile(FormulaLanguage.standard().parse(expression),
                    Symbols.of(List.of(), names), expression).evaluate(values);
        }
    }
}
