package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;

import org.jmouse.parallax.component.ComponentModel.Pin;
import org.jmouse.parallax.component.ComponentModel.PinNature;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A circuit with its values evaluated for the chosen parameters and every unknown numbered: what the solver reads.
 *
 * <p>Unknowns are, in order: the analog nodes (the circuit's own, then the internal nodes of models), the branches
 * that carry their own current (a generator's, a model's voltage-defined equations), and the continuous states of
 * models. Node {@link Circuit#GROUND} is the reference and has no number.</p>
 *
 * <p>A node that only digital pins touch is not an unknown at all: it is a digital node, resolved from its drivers by
 * the event simulation. A digital pin on an analog node gets a bridge — a threshold detector for an input, a source
 * with an edge and a resistance for an output — from its logic family.</p>
 */
record Netlist(List<String> nodes, List<Element> elements, List<Branch> branches, List<String> states,
               List<ModelUnit> units, List<String> digitalNodes, double duration) {

    static final int GROUND = -1;

    /** The resistance behind gmin: 1e-12 S from every analog node a model touches to ground. */
    private static final double LEAK_RESISTANCE = 1e12;

    /** What the solver's own leaks to ground are named: elements of the solution, never parts of the circuit. */
    private static final String LEAK_PREFIX = "gmin_";

    /**
     * One element the engine supplies itself — a model's equations are its {@link ModelUnit}s —: its terminals as node
     * numbers ({@link #GROUND} for the reference) and its branch row, or -1. A generator — a component whose voltage is a
     * function of time ({@link SourceSignal}) — is here as a source carrying its {@code signal}.
     *
     * @param part the part of the circuit it is — a generator's id —, or null for what the solver and the bench put in:
     *             a node's leak to ground, a failed part's stand-in, a logic pin's capacitance
     */
    record Element(String id, Kind kind, String part, int positive, int negative, double value, int branch,
                   SourceSignal signal, String[] terminalNames) {

        /** A bare primitive's terminals: "+" and "−". */
        static final String[] PRIMITIVE_TERMINALS = {ObjectDefinition.PRIMITIVE_POSITIVE, ObjectDefinition.PRIMITIVE_NEGATIVE};

        /** What the engine's own devices are ({@link Device#of(Element, int)}). */
        enum Kind {
            RESISTOR, CAPACITOR, VOLTAGE_SOURCE, CURRENT_SOURCE;

            boolean isSource() {
                return this == VOLTAGE_SOURCE || this == CURRENT_SOURCE;
            }
        }

        /** An element that is no part: a leak, a stand-in, a pin's capacitance. */
        Element(String id, Kind kind, int positive, int negative, double value) {
            this(id, kind, null, positive, negative, value, -1, null, PRIMITIVE_TERMINALS);
        }

        /** What its two terminals are called when its current is reported: a component's pins, or "+" and "−". */
        String[] terminals() {
            return terminalNames == null ? PRIMITIVE_TERMINALS : terminalNames;
        }

        /** A source's value at time t; the circuit is at rest before 0 and every source switches on at 0. */
        double valueAt(double time) {
            if (signal != null) {
                return signal.valueAt(time);
            }
            return value;
        }

        /** A source's constant part: what the operating point sees. A periodic source contributes nothing. */
        double steadyValue() {
            if (signal != null) {
                return signal.valueAt(0);
            }
            return value;
        }
    }

    /**
     * An unknown current: a generator's own ({@code partCurrent}), reported under the part's name; or one a model's
     * {@code voltage} equation carries inside the part, named for a reader of a refusal and reported by the part's pins
     * instead.
     */
    record Branch(String name, boolean partCurrent) {
    }

    /** Where one pin of one unit landed. {@code analog} is a node number or {@link #GROUND}; else it is digital. */
    record PinBinding(Pin pin, int analog, String digitalNode) {
        boolean isDigitalNode() {
            return digitalNode != null;
        }
    }

    /**
     * One block of one model in the circuit, bound: its parameters, where each pin went, the rows of its internal
     * nodes, voltage branches and continuous states.
     *
     * @param part       the part of the circuit it is a unit of: {@code A1} for {@code A1.b}
     * @param reportedAs what each pin's current is reported under, where that is not the pin's own name: a part written
     *                   in pin order (`R1 resistor (a, b)`) keeps the "+" and "−" it is drawn with, though its document
     *                   names them terminal_one and terminal_two
     */
    record ModelUnit(String label, String part, ComponentModel model, ComponentModel family, Map<String, Double> parameters,
                     Map<String, String> texts,
                     Map<String, PinBinding> pins, Map<String, Integer> internalNodes, Map<String, String> internalDigitalNodes,
                     List<Integer> voltageBranches, Map<String, Integer> stateRows, Map<String, String> reportedAs) {

        ModelUnit {
            // In their written order: a unit's states are reported, and its parameters laid out, as the document has them.
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
            texts = Collections.unmodifiableMap(new LinkedHashMap<>(texts));
            stateRows = Collections.unmodifiableMap(new LinkedHashMap<>(stateRows));
        }
    }

    /** The netlist of a circuit as written: elaborated first ({@link ElaboratedCircuit}). */
    static Netlist of(CompiledObject written) {
        return of(ElaboratedCircuit.of(written));
    }

    /** The netlist of an elaborated circuit, or of one piece of it. */
    static Netlist of(ElaboratedCircuit elaborated) {
        Circuit circuit = elaborated.circuit();

        // Which circuit nodes are analog: any node an engine element or a non-digital pin touches.
        Set<String> analog = new LinkedHashSet<>();
        Set<String> touched = new LinkedHashSet<>();
        // The names each model's equations reach, worked out once per model of the build.
        Map<ComponentModel, Set<String>> reached = new IdentityHashMap<>();
        for (CircuitElement element : circuit.elements()) {
            if (element.type() == CircuitElementType.MODEL) {
                ComponentModel model = elaborated.model(element.component());
                Set<String> reachedByEquations = reached.computeIfAbsent(model, ComponentModel::namesReachedByEquations);
                element.pins().forEach((key, node) -> {
                    Pin pin = pinOf(model, element.id(), key);
                    touched.add(node);
                    if (pin.nature() != PinNature.DIGITAL || reachedByEquations.contains(pin.name())) {
                        analog.add(node);
                    }
                });
            } else {
                analog.add(element.positive());
                analog.add(element.negative());
            }
        }
        analog.remove(Circuit.GROUND);

        Map<String, Integer> numbers = new LinkedHashMap<>();
        analog.forEach(node -> numbers.put(node, numbers.size()));
        List<String> digitalNodes = new ArrayList<>();
        for (String node : touched) {
            if (!analog.contains(node) && !node.equals(Circuit.GROUND)) {
                digitalNodes.add(node);
            }
        }

        // Bind every unit's pins; an unconnected pin gets a private node, analog or digital by its nature.
        List<UnitDraft> drafts = new ArrayList<>();
        // A generator is no unit: it is an ideal source whose voltage its document writes as a function of time.
        Map<CircuitElement, SourceSignal> generators = new LinkedHashMap<>();
        for (CircuitElement element : circuit.elements()) {
            if (element.type() != CircuitElementType.MODEL) {
                continue;
            }
            ComponentModel model = elaborated.model(element.component());
            if (model.isGenerator()) {
                Optional<SourceSignal> signal = SourceSignal.of(model, elaborated.parameters(model, element, ""));
                if (signal.isPresent()) {
                    generators.put(element, signal.get());
                    continue;
                }
            }
            drafts.addAll(unitsOf(elaborated, element, model, numbers, digitalNodes,
                    reached.computeIfAbsent(model, ComponentModel::namesReachedByEquations)));
        }

        List<Branch> branches = new ArrayList<>();
        List<Element> elements = new ArrayList<>();
        // The engine's own elements: what the bench left of a failed part.
        for (CircuitElement element : circuit.elements()) {
            if (element.type() != CircuitElementType.MODEL) {
                double value = elaborated.value(element.value());
                ElaboratedCircuit.requireValue(element, value);
                elements.add(new Element(element.id(), standInKind(element), nodeNumber(numbers, element.positive()),
                        nodeNumber(numbers, element.negative()), value));
            }
        }
        // Every analog node a model touches gets the solver's minimum conductance to ground (SPICE's gmin): a pin
        // that only a switched-off model drives must not leave the system singular.
        Set<String> leaked = new LinkedHashSet<>();
        for (UnitDraft draft : drafts) {
            draft.connections().forEach((pin, node) -> {
                if (numbers.containsKey(node) && leaked.add(node)) {
                    elements.add(new Element(LEAK_PREFIX + leaked.size(), Element.Kind.RESISTOR, numbers.get(node), GROUND,
                            LEAK_RESISTANCE));
                }
            });
        }
        generators.forEach((element, signal) -> {
            ComponentModel model = elaborated.model(element.component());
            Element.Kind kind = SourceSignal.kindOf(model);
            int branch = -1;
            if (kind == Element.Kind.VOLTAGE_SOURCE) {
                branch = numbers.size() + branches.size();
                branches.add(new Branch(element.id(), true));
            }
            elements.add(new Element(element.id(), kind, element.id(),
                    nodeNumber(numbers, element.pins().get(model.generatorPositive())),
                    nodeNumber(numbers, element.pins().get(model.generatorNegative())), 1, branch, signal,
                    Element.PRIMITIVE_TERMINALS));
        });

        List<List<Integer>> voltageBranches = new ArrayList<>();
        for (UnitDraft draft : drafts) {
            List<Integer> own = new ArrayList<>();
            for (ComponentModel.Equation equation : draft.model().equations()) {
                if (equation.kind() == ComponentModel.EquationKind.VOLTAGE) {
                    own.add(numbers.size() + branches.size());
                    // Inside the model: named for a reader of a refusal, never a column of what a circuit reports.
                    branches.add(new Branch(draft.label() + ": voltage " + equation.positive() + " -> " + equation.negative(), false));
                }
            }
            voltageBranches.add(own);
        }
        int stateBase = numbers.size() + branches.size();
        List<ModelUnit> units = new ArrayList<>();
        List<String> states = new ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            UnitDraft draft = drafts.get(index);
            Map<String, Integer> stateRows = new LinkedHashMap<>();
            for (ComponentModel.State state : draft.model().states()) {
                if (!state.discrete()) {
                    stateRows.put(state.name(), stateBase + states.size());
                    states.add(draft.label() + "." + state.name());
                }
            }
            units.add(new ModelUnit(draft.label(), draft.part(), draft.model(), draft.family(), draft.parameters(), draft.texts(),
                    bindings(draft, numbers), draft.internal(), draft.internalDigital(), voltageBranches.get(index), stateRows,
                    draft.reportedAs()));
        }
        return new Netlist(List.copyOf(numbers.keySet()), List.copyOf(elements), List.copyOf(branches), List.copyOf(states),
                List.copyOf(units), List.copyOf(digitalNodes), circuit.time().maximum());
    }

    /** What a failed part's stand-in is to the engine: a conductance, as the bench writes every one of them. */
    private static Element.Kind standInKind(CircuitElement element) {
        if (element.type() != CircuitElementType.RESISTOR) {
            throw new IllegalStateException("'" + element.id() + "' is a " + element.type() + " the netlist has no device for: "
                    + "every part is its document's model, and the bench leaves only conductances.");
        }
        return Element.Kind.RESISTOR;
    }

    /**
     * Every unit of one model part that is wired into the circuit, its pins bound. A multi-unit package contributes only
     * the units actually wired: an idle amplifier with floating input pins has no defined operating point, and
     * inventing private nodes for it would make the whole circuit impossible to solve.
     */
    private static List<UnitDraft> unitsOf(ElaboratedCircuit elaborated, CircuitElement element, ComponentModel model,
                                           Map<String, Integer> numbers, List<String> digitalNodes, Set<String> reached) {
        // Only a part with digital pins reads a logic family; any other names the component it is a member of.
        ComponentModel family = model.familyName().filter(name -> model.hasDigitalPins())
                .map(name -> elaborated.family(name).orElseThrow(() -> new IllegalArgumentException(
                        "'" + model.name() + "' names the logic family '" + name + "', which does not exist.")))
                .orElse(null);
        if (model.hasDigitalPins() && family == null) {
            throw new IllegalArgumentException("'" + model.name() + "' has digital pins and names no logic family.");
        }
        List<UnitDraft> drafts = new ArrayList<>();
        for (String unit : model.unitNames()) {
            if (!unit.isEmpty() && model.unitNames().size() > 1
                    && element.pins().keySet().stream().noneMatch(pin -> pin.startsWith(unit + "."))) {
                continue;
            }
            String label = unit.isEmpty() ? element.id() : element.id() + "." + unit;
            Map<String, String> connections = new LinkedHashMap<>();
            for (Pin pin : model.pins()) {
                String key = pin.shared() || unit.isEmpty() ? pin.name() : unit + "." + pin.name();
                String node = element.pins().get(key);
                if (node == null) {
                    node = label + "." + pin.name();
                    if (pin.nature() == PinNature.DIGITAL && !reached.contains(pin.name())) {
                        digitalNodes.add(node);
                    } else {
                        numbers.put(node, numbers.size());
                    }
                }
                connections.put(pin.name(), node);
            }
            Map<String, Integer> internal = new LinkedHashMap<>();
            for (String node : model.nodes()) {
                String name = label + "." + node;
                numbers.put(name, numbers.size());
                internal.put(node, numbers.get(name));
            }
            Map<String, String> internalDigital = new LinkedHashMap<>();
            for (String node : model.digitalNodes()) {
                String name = label + "." + node;
                digitalNodes.add(name);
                internalDigital.put(node, name);
            }
            drafts.add(new UnitDraft(label, element.id(), model, family, elaborated.parameters(model, element, unit),
                    elaborated.texts(model, element),
                    connections, internal, internalDigital, elaborated.reportedAs(element.id())));
        }
        return drafts;
    }

    int size() {
        return nodes.size() + branches.size() + states.size();
    }

    /**
     * The same circuit with one element held at a value — what a sweep steps through. A generator swept is held there
     * too: its signal goes with the sweep, or it would win over the value and every point would be the same.
     */
    Netlist withValue(String id, double value) {
        List<Element> changed = elements.stream()
                .map(element -> element.id().equals(id) ? new Element(element.id(), element.kind(), element.part(),
                        element.positive(), element.negative(), value, element.branch(), null, element.terminalNames()) : element)
                .toList();
        return new Netlist(nodes, changed, branches, states, units, digitalNodes, duration);
    }

    private record UnitDraft(String label, String part, ComponentModel model, ComponentModel family,
                             Map<String, Double> parameters, Map<String, String> texts, Map<String, String> connections,
                             Map<String, Integer> internal,
                             Map<String, String> internalDigital, Map<String, String> reportedAs) {
    }

    private static Map<String, PinBinding> bindings(UnitDraft draft, Map<String, Integer> numbers) {
        Map<String, PinBinding> bindings = new LinkedHashMap<>();
        for (Pin pin : draft.model().pins()) {
            String node = draft.connections().get(pin.name());
            boolean digital = !node.equals(Circuit.GROUND) && !numbers.containsKey(node);
            bindings.put(pin.name(), digital
                    ? new PinBinding(pin, GROUND, node)
                    : new PinBinding(pin, nodeNumber(numbers, node), null));
        }
        return bindings;
    }

    /**
     * The names a primitive's terminals are reported by: the component's pins when it stands for one — the pin on its
     * positive node first —, else "+" and "−".
     */
    static String[] terminalNamesOf(CircuitElement element) {
        if (element.pins() == null || element.pins().size() != 2) {
            return Element.PRIMITIVE_TERMINALS;
        }
        String first = element.pins().entrySet().stream().filter(pin -> pin.getValue().equals(element.positive()))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        String second = element.pins().keySet().stream().filter(name -> !name.equals(first)).findFirst().orElse(null);
        return first == null || second == null ? Element.PRIMITIVE_TERMINALS : new String[]{first, second};
    }

    private static Pin pinOf(ComponentModel model, String element, String key) {
        String name = key.contains(".") ? key.substring(key.indexOf('.') + 1) : key;
        return model.pin(name).orElseThrow(() ->
                new IllegalArgumentException("'" + element + "' connects '" + key + "', which is not a pin of " + model.name() + "."));
    }

    private static int nodeNumber(Map<String, Integer> numbers, String node) {
        return node.equals(Circuit.GROUND) ? GROUND : numbers.get(node);
    }

    /** A written value that can stand: finite, and above nought for a part that is not a source. */
    static void requireValue(CircuitElement element, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("The value of '" + element.id() + "' is not a finite number.");
        }
        boolean passive = !element.type().isSource();
        if (passive && value <= 0) {
            throw new IllegalArgumentException("'" + element.id() + "' needs a positive value; it is " + value + ".");
        }
    }
}
