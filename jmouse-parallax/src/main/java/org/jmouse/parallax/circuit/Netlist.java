package org.jmouse.parallax.circuit;

import org.jmouse.parallax.circuit.ComponentModel.Pin;
import org.jmouse.parallax.circuit.ComponentModel.PinNature;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentHierarchy;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.parallax.object.ObjectDefinition.SourceWaveform;

import java.util.ArrayList;
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
 * that carry their own current (voltage sources, inductors, a model's voltage-defined equations), and the continuous
 * states of models. Node {@link Circuit#GROUND} is the reference and has no number.</p>
 *
 * <p>A node that only digital pins touch is not an unknown at all: it is a digital node, resolved from its drivers by
 * the event simulation. A digital pin on an analog node gets a bridge — a threshold detector for an input, a source
 * with an edge and a resistance for an output — from its logic family.</p>
 */
record Netlist(List<String> nodes, List<Element> elements, List<String> branches, List<String> states,
               List<ModelUnit> units, List<String> digitalNodes, double duration) {

    static final int GROUND = -1;

    /** The resistance behind gmin: 1e-12 S from every analog node a model touches to ground. */
    private static final double LEAK_RESISTANCE = 1e12;

    /**
     * One element: its terminals as node numbers ({@link #GROUND} for the reference) and its branch row, or -1. A
     * generator — a component whose voltage is a function of time ({@link SourceSignal}) — is here as a voltage
     * source carrying its {@code signal}.
     */
    record Element(String id, CircuitElementType type, int positive, int negative, double value, int branch,
                   SourceSignal signal, String[] terminalNames) {

        /** A bare primitive's terminals: "+" and "−". */
        static final String[] PRIMITIVE_TERMINALS = {ObjectDefinition.PRIMITIVE_POSITIVE, ObjectDefinition.PRIMITIVE_NEGATIVE};

        Element(String id, CircuitElementType type, int positive, int negative, double value, int branch) {
            this(id, type, positive, negative, value, branch, null, PRIMITIVE_TERMINALS);
        }

        Element(String id, CircuitElementType type, int positive, int negative, double value, int branch, SourceSignal signal) {
            this(id, type, positive, negative, value, branch, signal, PRIMITIVE_TERMINALS);
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

    /** Where one pin of one unit landed. {@code analog} is a node number or {@link #GROUND}; else it is digital. */
    record PinBinding(Pin pin, int analog, String digitalNode) {
        boolean isDigitalNode() {
            return digitalNode != null;
        }
    }

    /**
     * One block of one model in the circuit, bound: its parameters, where each pin went, the rows of its internal
     * nodes, voltage branches and continuous states.
     */
    record ModelUnit(String label, ComponentModel model, ComponentModel family, Map<String, Double> parameters,
                     Map<String, PinBinding> pins, Map<String, Integer> internalNodes, Map<String, String> internalDigitalNodes, List<Integer> voltageBranches,
                     Map<String, Integer> stateRows) {
    }

    static Netlist of(CompiledObject original) {
        CompiledObject object = ComponentHierarchy.expand(original);
        Circuit circuit = (Circuit) object.definition();
        Map<String, ComponentModel> models = new LinkedHashMap<>();
        for (CircuitElement element : circuit.elements()) {
            if (element.type() == CircuitElementType.MODEL) {
                models.computeIfAbsent(element.component(), reference -> ComponentDocuments.read(object.componentSource(reference), reference, object.library()));
            }
        }

        // Which circuit nodes are analog: any node a standard element or a non-digital pin touches.
        Set<String> analog = new LinkedHashSet<>();
        Set<String> touched = new LinkedHashSet<>();
        for (CircuitElement element : circuit.elements()) {
            if (element.type() == CircuitElementType.MODEL) {
                ComponentModel model = models.get(element.component());
                element.pins().forEach((key, node) -> {
                    Pin pin = pinOf(model, element.id(), key);
                    touched.add(node);
                    if (pin.nature() != PinNature.DIGITAL) {
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
        List<CircuitElement> synthetic = new ArrayList<>();
        // A generator is no unit: it is an ideal source whose voltage its document writes as a function of time.
        Map<CircuitElement, SourceSignal> generators = new LinkedHashMap<>();
        for (CircuitElement element : circuit.elements()) {
            if (element.type() != CircuitElementType.MODEL) {
                continue;
            }
            ComponentModel model = models.get(element.component());
            if (SourceSignal.isGenerator(model)) {
                Optional<SourceSignal> signal = SourceSignal.of(model, parameters(object, model, element, ""));
                if (signal.isPresent()) {
                    generators.put(element, signal.get());
                    continue;
                }
            }
            // Only a part with digital pins reads a logic family; any other names the component it is a member of.
            ComponentModel family = model.familyName().filter(name -> model.hasDigitalPins())
                    .map(name -> ComponentDocuments.read(object.library().source(name).orElseThrow(() ->
                            new IllegalArgumentException("'" + model.name() + "' names the logic family '" + name + "', which does not exist.")), name, object.library()))
                    .orElse(null);
            if (model.hasDigitalPins() && family == null) {
                throw new IllegalArgumentException("'" + model.name() + "' has digital pins and names no logic family.");
            }
            for (String unit : model.unitNames()) {
                // A multi-unit package contributes only the units actually wired into this circuit. An idle
                // amplifier with floating input pins has no defined operating point; inventing private nodes
                // for it would make the whole circuit impossible to solve.
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
                        if (pin.nature() == PinNature.DIGITAL) {
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
                drafts.add(new UnitDraft(label, model, family, parameters(object, model, element, unit), connections, internal, internalDigital));
            }
        }

        // Every analog node a model touches gets the solver's minimum conductance to ground (SPICE's gmin): a pin
        // that only a switched-off model drives must not leave the system singular.
        Set<String> leaked = new LinkedHashSet<>();
        for (UnitDraft draft : drafts) {
            draft.connections().forEach((pin, node) -> {
                if (numbers.containsKey(node) && leaked.add(node)) {
                    synthetic.add(leak(LEAK_PREFIX + leaked.size(), node));
                }
            });
        }
        List<String> branches = new ArrayList<>();
        List<Element> elements = new ArrayList<>();
        List<CircuitElement> all = new ArrayList<>(circuit.elements());
        all.addAll(synthetic);
        for (CircuitElement element : all) {
            if (element.type() == CircuitElementType.MODEL) {
                continue;
            }
            double value = element.value().startsWith("=") ? Double.parseDouble(element.value().substring(1)) : object.constant(element.value());
            requireValue(element, value);
            boolean ownCurrent = element.type() == CircuitElementType.VOLTAGE_SOURCE || element.type() == CircuitElementType.INDUCTOR;
            int branch = ownCurrent ? numbers.size() + branches.size() : -1;
            if (ownCurrent) {
                branches.add(element.id());
            }
            elements.add(new Element(element.id(), element.type(), nodeNumber(numbers, element.positive()),
                    nodeNumber(numbers, element.negative()), value, branch, null, terminalNamesOf(element)));
        }
        generators.forEach((element, signal) -> {
            ComponentModel model = models.get(element.component());
            int branch = numbers.size() + branches.size();
            branches.add(element.id());
            elements.add(new Element(element.id(), CircuitElementType.VOLTAGE_SOURCE,
                    nodeNumber(numbers, element.pins().get(SourceSignal.positivePin(model))),
                    nodeNumber(numbers, element.pins().get(SourceSignal.negativePin(model))), 1, branch, signal));
        });

        List<ModelUnit> units = new ArrayList<>();
        List<String> states = new ArrayList<>();
        for (UnitDraft draft : drafts) {
            List<Integer> voltageBranches = new ArrayList<>();
            for (ComponentModel.Equation equation : draft.model().equations()) {
                if (equation.kind() == ComponentModel.EquationKind.VOLTAGE) {
                    voltageBranches.add(numbers.size() + branches.size());
                    // Inside the model: named for a reader of a refusal, never a column of what a circuit reports.
                    branches.add(draft.label() + ": voltage " + equation.positive() + " -> " + equation.negative());
                }
            }
            units.add(new ModelUnit(draft.label(), draft.model(), draft.family(), draft.parameters(),
                    bindings(draft, numbers), draft.internal(), draft.internalDigital(), voltageBranches, new LinkedHashMap<>()));
        }
        int stateBase = numbers.size() + branches.size();
        for (ModelUnit unit : units) {
            for (ComponentModel.State state : unit.model().states()) {
                // A model's own heat is no unknown on an ideal bench: it is nought there, and its row would cost every step.
                boolean heatsOnlyWhenRealistic = ModelEquations.HEATING.equals(state.name())
                        && unit.parameters().getOrDefault(ModelEquations.REALISTIC, 0.0) == 0;
                if (!state.discrete() && !heatsOnlyWhenRealistic) {
                    unit.stateRows().put(state.name(), stateBase + states.size());
                    states.add(unit.label() + "." + state.name());
                }
            }
        }
        return new Netlist(List.copyOf(numbers.keySet()), List.copyOf(elements), List.copyOf(branches), List.copyOf(states),
                List.copyOf(units), List.copyOf(digitalNodes), circuit.time().maximum());
    }

    int size() {
        return nodes.size() + branches.size() + states.size();
    }

    /** The same circuit with one element's value replaced — what a sweep steps through. */
    Netlist withValue(String id, double value) {
        List<Element> changed = elements.stream()
                .map(element -> element.id().equals(id) ? new Element(element.id(), element.type(), element.positive(),
                        element.negative(), value, element.branch(), element.signal(), element.terminalNames()) : element)
                .toList();
        return new Netlist(nodes, changed, branches, states, units, digitalNodes, duration);
    }

    private record UnitDraft(String label, ComponentModel model, ComponentModel family, Map<String, Double> parameters,
                             Map<String, String> connections, Map<String, Integer> internal, Map<String, String> internalDigital) {
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
     * Every component's equations read the bench's temperature by this name, in kelvin: 25 °C unless the bench sets
     * another, which it does as an override of it ({@code BenchConditioning}). A drift is written in the component.
     */
    public static final String TEMPERATURE = "temperature";
    /** Whether the bench is realistic, 1, or ideal, 0 — given to every component's equations as the temperature is. */
    public static final String REALISTIC = "realistic";
    /**
     * Which corner of its spread a part stands at — -1, 0 or +1, the bench's tolerance corner —, given to every
     * component's equations as the temperature is: a parameter that spreads from part to part (a gain, a threshold, an
     * offset) is written as its value moved by the corner times its spread.
     */
    public static final String TOLERANCE_CORNER = "tolerance_corner";
    private static final double NOMINAL_KELVIN = 273.15 + ObjectDefinition.BenchConditions.NOMINAL_TEMPERATURE;

    /** The bench's figures, then the model's parameters, then what the circuit overrides — {@code Vos} or {@code A.Vos}. */
    private static Map<String, Double> parameters(CompiledObject object, ComponentModel model, CircuitElement element, String unit) {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put(TEMPERATURE, NOMINAL_KELVIN);
        values.put(REALISTIC, 0.0);
        values.put(TOLERANCE_CORNER, 0.0);
        model.parameters().forEach(parameter -> values.put(parameter.name(), parameter.value()));
        element.overrides().forEach((key, expression) -> {
            String name = key.contains(".") ? key.substring(key.indexOf('.') + 1) : key;
            boolean forThisUnit = !key.contains(".") || key.startsWith(unit + ".");
            if (!values.containsKey(name)) {
                throw new IllegalArgumentException("'" + model.name() + "' has no parameter '" + name + "' to override.");
            }
            if (forThisUnit) {
                values.put(name, object.constant(expression));
            }
        });
        return values;
    }

    /**
     * The names a primitive's terminals are reported by: the component's pins when it stands for one — the pin on its
     * positive node first —, else "+" and "−".
     */
    private static String[] terminalNamesOf(CircuitElement element) {
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

    /** What the solver's own leaks to ground are named: elements of the solution, never parts of the circuit. */
    static final String LEAK_PREFIX = "gmin_";

    /** Whether an element is one the solver added — a leak to ground — rather than a part someone placed. */
    public static boolean isSolverElement(String id) {
        return id.startsWith(LEAK_PREFIX);
    }

    private static CircuitElement leak(String id, String node) {
        return new CircuitElement(id, CircuitElementType.RESISTOR, node, Circuit.GROUND, "=" + LEAK_RESISTANCE,
                null, null, null, null, null);
    }

    private static int nodeNumber(Map<String, Integer> numbers, String node) {
        return node.equals(Circuit.GROUND) ? GROUND : numbers.get(node);
    }

    private static void requireValue(CircuitElement element, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("The value of '" + element.id() + "' is not a finite number.");
        }
        boolean passive = !element.type().isSource();
        if (passive && value <= 0) {
            throw new IllegalArgumentException("'" + element.id() + "' needs a positive value; it is " + value + ".");
        }
    }
}
