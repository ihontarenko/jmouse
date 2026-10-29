package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Equation;
import org.jmouse.cad.simulation.SimulatedFamily;
import org.jmouse.cad.component.InternalNode;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.State;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;
import org.jmouse.cad.simulation.elaboration.ElaboratedPart;
import org.jmouse.cad.simulation.elaboration.EngineElement;

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
 * <p>Unknowns are, in order: the analog nodes (the circuit's own, then the internal nodes of components), the branches
 * that carry their own current (a generator's, a component's voltage equations), and the continuous states of
 * components. Node {@value org.jmouse.cad.circuit.Circuit#GROUND} is the reference and has no number.</p>
 *
 * <p>A node that only digital pins touch is not an unknown at all: it is a digital node, resolved from its drivers by
 * the event simulation. A digital pin on an analog node gets a bridge from its logic family.</p>
 *
 * @param duration how long the circuit is run for, seconds
 */
public record Netlist(List<String> nodes, List<Element> elements, List<Branch> branches, List<String> states,
                      List<ModelUnit> units, List<String> digitalNodes, double duration) {

    /** The reference node's number: no row, no column. */
    public static final int GROUND = -1;

    public Netlist {
        nodes = List.copyOf(nodes);
        elements = List.copyOf(elements);
        branches = List.copyOf(branches);
        states = List.copyOf(states);
        units = List.copyOf(units);
        digitalNodes = List.copyOf(digitalNodes);
    }

    /**
     * One element the engine supplies itself — a component's equations are its {@link ModelUnit}s —: its terminals as
     * node numbers ({@link #GROUND} for the reference) and its branch row, or -1. A generator — a component whose drive is
     * a function of time ({@link SourceSignal}) — is here as a source carrying its {@code signal}.
     *
     * @param part the part of the circuit it is — a generator's reference —, or null for what the solver and the bench
     *             put in: a node's leak to ground, a failed part's stand-in, a logic pin's capacitance
     */
    public record Element(String id, Kind kind, String part, int positive, int negative, double value, int branch,
                          SourceSignal signal, String[] terminalNames) {

        /** What a bare element's terminals are called in what it reports: its "+" and its "−". */
        public static final String PRIMITIVE_POSITIVE = "+";
        public static final String PRIMITIVE_NEGATIVE = "−";
        static final String[] PRIMITIVE_TERMINALS = {PRIMITIVE_POSITIVE, PRIMITIVE_NEGATIVE};

        /** What the engine's own devices are ({@link Device#of(Element, int)}). */
        public enum Kind {
            RESISTOR, CAPACITOR, VOLTAGE_SOURCE, CURRENT_SOURCE;

            boolean isSource() {
                return this == VOLTAGE_SOURCE || this == CURRENT_SOURCE;
            }
        }

        /** An element that is no part: a leak, a stand-in, a pin's capacitance. */
        public Element(String id, Kind kind, int positive, int negative, double value) {
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

        /** A source's constant part: what the operating point sees. */
        double steadyValue() {
            if (signal != null) {
                return signal.valueAt(0);
            }
            return value;
        }
    }

    /**
     * An unknown current: a generator's own ({@code partCurrent}), reported under the part's name; or one a component's
     * {@code voltage} equation carries inside the part, named for a reader of a refusal.
     */
    public record Branch(String name, boolean partCurrent) {
    }

    /** Where one pin of one unit landed: {@code analog} is a node number or {@link #GROUND}; else it is digital. */
    public record PinBinding(Pin pin, int analog, String digitalNode) {

        public boolean isDigitalNode() {
            return digitalNode != null;
        }
    }

    /**
     * One block of one component in the circuit, bound: its parameters, where each pin went, the rows of its internal
     * nodes, voltage branches and continuous states.
     *
     * @param part       the part of the circuit it is a unit of: {@code A1} for {@code A1.b}
     * @param family     the logic family its digital pins follow, or null
     * @param texts      its text parameters, read by its behaviour and never by its equations
     * @param reportedAs what each pin's current is reported under, where that is not the pin's own name
     */
    public record ModelUnit(String label, String part, SimulatedComponent component, SimulatedFamily family,
                            Map<String, Double> parameters, Map<String, String> texts, Map<String, PinBinding> pins,
                            Map<String, Integer> internalNodes, Map<String, String> internalDigitalNodes,
                            List<Integer> voltageBranches, Map<String, Integer> stateRows, Map<String, String> reportedAs) {

        public ModelUnit {
            // In their written order: a unit's states are reported, and its parameters laid out, as the document has them.
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
            texts = Collections.unmodifiableMap(new LinkedHashMap<>(texts));
            pins = Collections.unmodifiableMap(new LinkedHashMap<>(pins));
            internalNodes = Collections.unmodifiableMap(new LinkedHashMap<>(internalNodes));
            internalDigitalNodes = Collections.unmodifiableMap(new LinkedHashMap<>(internalDigitalNodes));
            voltageBranches = List.copyOf(voltageBranches);
            stateRows = Collections.unmodifiableMap(new LinkedHashMap<>(stateRows));
            reportedAs = Map.copyOf(reportedAs);
        }
    }

    /** The resistance behind gmin: 1e-12 S from every analog node a component touches to ground. */
    static final double LEAK_RESISTANCE = 1e12;

    /** What the solver's own leaks to ground are named: elements of the solution, never parts of the circuit. */
    private static final String LEAK_PREFIX = "gmin_";

    /** What a voltage equation is, of the two a branch equation can be. */
    private static final String VOLTAGE = "voltage";

    /** The netlist of an elaborated circuit. */
    public static Netlist of(ElaboratedCircuit elaborated) {
        // Which nets are analog: any a non-digital pin touches, or a digital pin its equations read.
        Set<String> analog = new LinkedHashSet<>();
        Set<String> touched = new LinkedHashSet<>();
        Map<SimulatedComponent, Set<String>> reached = new IdentityHashMap<>();
        for (ElaboratedPart part : elaborated.parts()) {
            Set<String> readByEquations = reached.computeIfAbsent(part.component(), ModelEquations::namesReached);
            part.connections().forEach((key, net) -> {
                Pin pin = pinOf(part, key);
                touched.add(net);
                if (!pin.digital() || readByEquations.contains(pin.name())) {
                    analog.add(net);
                }
            });
        }
        for (EngineElement element : elaborated.elements()) {
            analog.add(element.positive());
            analog.add(element.negative());
        }
        analog.remove(Circuit.GROUND);
        Map<String, Integer> numbers = new LinkedHashMap<>();
        analog.forEach(net -> numbers.put(net, numbers.size()));
        List<String> digitalNodes = new ArrayList<>();
        for (String net : touched) {
            if (!analog.contains(net) && !net.equals(Circuit.GROUND)) {
                digitalNodes.add(net);
            }
        }
        // Bind every unit's pins; an unconnected pin gets a private node, analog or digital by its nature.
        List<UnitDraft> drafts = new ArrayList<>();
        // A generator is no unit: it is an ideal source whose drive its document writes as a function of time.
        Map<ElaboratedPart, SourceSignal> generators = new LinkedHashMap<>();
        for (ElaboratedPart part : elaborated.parts()) {
            Optional<SourceSignal> signal = SourceSignal.of(part.component(), elaborated.parameters(part, ""));
            if (signal.isPresent()) {
                generators.put(part, signal.get());
                continue;
            }
            drafts.addAll(unitsOf(elaborated, part, numbers, digitalNodes, reached.get(part.component())));
        }
        List<Branch> branches = new ArrayList<>();
        List<Element> elements = new ArrayList<>();
        // The engine's own elements: what the bench left of a failed part, an offset it put beside a source.
        for (EngineElement element : elaborated.elements()) {
            Element.Kind kind = switch (element.kind()) {
                case CONDUCTANCE -> Element.Kind.RESISTOR;
                case VOLTAGE_SOURCE -> Element.Kind.VOLTAGE_SOURCE;
                case CURRENT_SOURCE -> Element.Kind.CURRENT_SOURCE;
            };
            int branch = -1;
            if (kind == Element.Kind.VOLTAGE_SOURCE) {
                branch = numbers.size() + branches.size();
                branches.add(new Branch(element.id(), true));
            }
            elements.add(new Element(element.id(), kind, kind.isSource() ? element.id() : null, nodeNumber(numbers, element.positive()),
                    nodeNumber(numbers, element.negative()), element.value(), branch, null, Element.PRIMITIVE_TERMINALS));
        }
        // Every analog node a component touches gets the solver's minimum conductance to ground (SPICE's gmin): a pin
        // that only a switched-off component drives must not leave the system singular.
        Set<String> leaked = new LinkedHashSet<>();
        for (UnitDraft draft : drafts) {
            draft.connections().forEach((pin, net) -> {
                if (numbers.containsKey(net) && leaked.add(net)) {
                    elements.add(new Element(LEAK_PREFIX + leaked.size(), Element.Kind.RESISTOR, numbers.get(net), GROUND,
                            LEAK_RESISTANCE));
                }
            });
        }
        generators.forEach((part, signal) -> {
            Equation.Branch drive = part.component().generated().orElseThrow();
            Element.Kind kind = SourceSignal.kindOf(drive);
            int branch = -1;
            if (kind == Element.Kind.VOLTAGE_SOURCE) {
                branch = numbers.size() + branches.size();
                branches.add(new Branch(part.reference(), true));
            }
            // Its current goes into its own pins, the drive's ends, each under the name its circuit calls it.
            String[] terminals = {part.reportedAs().getOrDefault(drive.from(), drive.from()),
                                  part.reportedAs().getOrDefault(drive.to(), drive.to())};
            elements.add(new Element(part.reference(), kind, part.reference(), nodeNumber(numbers, part.connections().get(drive.from())),
                    nodeNumber(numbers, part.connections().get(drive.to())), 1, branch, signal, terminals));
        });
        List<List<Integer>> voltageBranches = new ArrayList<>();
        for (UnitDraft draft : drafts) {
            List<Integer> own = new ArrayList<>();
            for (Equation equation : draft.part().component().equations()) {
                if (equation instanceof Equation.Branch branch && branch.kind().equals(VOLTAGE)) {
                    own.add(numbers.size() + branches.size());
                    // Inside the component: named for a reader of a refusal, never a column of what a circuit reports.
                    branches.add(new Branch(draft.label() + ": voltage " + branch.from() + " -> " + branch.to(), false));
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
            for (State state : draft.part().component().states()) {
                if (state.continuous()) {
                    stateRows.put(state.name(), stateBase + states.size());
                    states.add(draft.label() + "." + state.name());
                }
            }
            units.add(new ModelUnit(draft.label(), draft.part().reference(), draft.part().component(), draft.part().family(),
                    draft.parameters(), elaborated.texts(draft.part()), bindings(draft, numbers), draft.internal(), Map.of(),
                    voltageBranches.get(index), stateRows, draft.part().reportedAs()));
        }
        return new Netlist(List.copyOf(numbers.keySet()), elements, branches, states, units, digitalNodes, elaborated.duration());
    }

    /**
     * Every unit of a part that is wired into the circuit, its pins bound. A package of several blocks contributes only
     * the blocks actually wired: an idle amplifier with floating inputs has no defined operating point, and inventing
     * private nodes for it would make the whole circuit impossible to solve.
     */
    private static List<UnitDraft> unitsOf(ElaboratedCircuit elaborated, ElaboratedPart part, Map<String, Integer> numbers,
                                           List<String> digitalNodes, Set<String> reached) {
        SimulatedComponent component = part.component();
        List<UnitDraft> drafts = new ArrayList<>();
        for (String unit : component.unitNames()) {
            if (!unit.isEmpty() && component.unitNames().size() > 1
                    && part.connections().keySet().stream().noneMatch(pin -> pin.startsWith(unit + ElaboratedCircuit.UNIT_SEPARATOR))) {
                continue;
            }
            String label = unit.isEmpty() ? part.reference() : part.reference() + ElaboratedCircuit.UNIT_SEPARATOR + unit;
            Map<String, String> connections = new LinkedHashMap<>();
            for (Pin pin : component.pins()) {
                String key = component.shared(pin) || unit.isEmpty() ? pin.name() : unit + ElaboratedCircuit.UNIT_SEPARATOR + pin.name();
                String net = part.connections().get(key);
                if (net == null) {
                    net = label + ElaboratedCircuit.UNIT_SEPARATOR + pin.name();
                    if (pin.digital() && !reached.contains(pin.name())) {
                        digitalNodes.add(net);
                    } else {
                        numbers.put(net, numbers.size());
                    }
                }
                connections.put(pin.name(), net);
            }
            Map<String, Integer> internal = new LinkedHashMap<>();
            for (InternalNode node : component.nodes()) {
                String name = label + ElaboratedCircuit.UNIT_SEPARATOR + node.name();
                numbers.put(name, numbers.size());
                internal.put(node.name(), numbers.get(name));
            }
            drafts.add(new UnitDraft(label, part, elaborated.parameters(part, unit), connections, internal));
        }
        return drafts;
    }

    private record UnitDraft(String label, ElaboratedPart part, Map<String, Double> parameters, Map<String, String> connections,
                             Map<String, Integer> internal) {
    }

    private static Map<String, PinBinding> bindings(UnitDraft draft, Map<String, Integer> numbers) {
        Map<String, PinBinding> bindings = new LinkedHashMap<>();
        for (Pin pin : draft.part().component().pins()) {
            String net = draft.connections().get(pin.name());
            boolean digital = !net.equals(Circuit.GROUND) && !numbers.containsKey(net);
            bindings.put(pin.name(), digital ? new PinBinding(pin, GROUND, net) : new PinBinding(pin, nodeNumber(numbers, net), null));
        }
        return bindings;
    }

    /** The pin a connection names: its own name, or after its block's — {@code A.output} is {@code output}. */
    private static Pin pinOf(ElaboratedPart part, String key) {
        int separator = key.indexOf(ElaboratedCircuit.UNIT_SEPARATOR);
        String name = separator < 0 ? key : key.substring(separator + 1);
        return part.component().pin(name).orElseThrow(() -> new IllegalArgumentException(
                "'" + part.reference() + "' connects '" + key + "', which is not a pin of " + part.component().title() + "."));
    }

    private static int nodeNumber(Map<String, Integer> numbers, String net) {
        return net.equals(Circuit.GROUND) ? GROUND : numbers.get(net);
    }

    /** How many unknowns the solver solves for. */
    public int size() {
        return nodes.size() + branches.size() + states.size();
    }

    /**
     * The same circuit with one element held at a value — what a sweep steps through. A generator swept is held there
     * too: its signal goes with the sweep, or it would win over the value and every point would be the same.
     */
    public Netlist withValue(String id, double value) {
        List<Element> changed = elements.stream()
                .map(element -> element.id().equals(id) ? new Element(element.id(), element.kind(), element.part(),
                        element.positive(), element.negative(), value, element.branch(), null, element.terminalNames()) : element)
                .toList();
        return new Netlist(nodes, changed, branches, states, units, digitalNodes, duration);
    }
}
