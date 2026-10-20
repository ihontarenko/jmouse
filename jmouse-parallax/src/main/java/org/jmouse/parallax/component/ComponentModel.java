package org.jmouse.parallax.component;

import org.jmouse.el.node.Expression;
import org.jmouse.el.type.ValueType;
import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;
import org.jmouse.parallax.script.Block;
import org.jmouse.script.el.node.FunctionDeclarationNode;
import org.jmouse.script.el.node.ScriptNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

/**
 * A component as the solver reads it, built from its jMX document by {@link ComponentDocuments}: what the part is
 * called, its pins and parameters, its analog equations (the source of each right side, for jMF to differentiate),
 * its behaviour (the jMS script node, parsed once) and the datasheet points it must meet.
 *
 * <p>{@code formulas} holds the jMF definitions the equations may call, already taken from the function libraries the
 * document includes — the solver never looks a library up by name.</p>
 *
 * <p>A family ({@code family "cmos-hc"}) has the same shape with only parameters: the thresholds, edges and strength
 * every part of a logic family shares.</p>
 */
public record ComponentModel(String name, boolean family, String kind, Map<String, List<String>> header,
                             List<String> units, List<Parameter> parameters, Map<String, String> texts, List<Pin> pins,
                             Map<String, List<String>> buses,
                             List<String> nodes, List<String> digitalNodes,
                             List<String> symbol, List<SubcircuitLine> subcircuit, List<String> formulas, List<State> states,
                             Map<String, String> locals, Map<String, ValueType> localTypes, List<Equation> equations,
                             ScriptNode behaviour,
                             List<Expectation> expectations, Map<String, String> displays, Map<String, Measure> measures,
                             Map<String, LeadLook> leads, Map<String, String> displayUnits,
                             Map<String, String> displayCaptions, Heat heat) {

    public ComponentModel {
        header = Map.copyOf(header);
        units = List.copyOf(units);
        parameters = List.copyOf(parameters);
        // Its text parameters, by name, in the order written: what a behaviour reads as words — a message, a label.
        texts = orderedCopy(texts);
        pins = List.copyOf(pins);
        // Each bus of pins, its bits in order, the first the least significant: what a behaviour reads as one number.
        buses = orderedCopy(buses);
        nodes = List.copyOf(nodes);
        digitalNodes = List.copyOf(digitalNodes);
        symbol = List.copyOf(symbol);
        subcircuit = List.copyOf(subcircuit);
        formulas = List.copyOf(formulas);
        states = List.copyOf(states);
        // What a typed let says its value is: let I_through : current = …
        localTypes = orderedCopy(localTypes);
        equations = List.copyOf(equations);
        expectations = List.copyOf(expectations);
        displays = orderedCopy(displays);
        measures = orderedCopy(measures);
        leads = orderedCopy(leads);
        // A display figure with a unit is written beside the part as well as drawn in its symbol.
        displayUnits = orderedCopy(displayUnits);
        // What such a figure is of, written after it: "of the rated voltage".
        displayCaptions = orderedCopy(displayCaptions);
    }

    /** An unmodifiable copy that keeps the order the document wrote; empty for none. */
    private static <V> Map<String, V> orderedCopy(Map<String, V> written) {
        return written == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(written));
    }

    /**
     * How a part warms ({@code heat { … }}), as written: each thermal state by name with the right side of its rate, the
     * lets in order, the right side of {@code temperature = …}, and the jMF definitions of the document the law is
     * written in. Its own when the component writes or uses one, else its base's, else the library's shared law
     * ({@link ComponentDocuments#SHARED_HEAT}); null for a part that does not warm.
     *
     * @param source the document the law is written in, for the messages
     */
    public record Heat(String source, Map<String, String> rates, Map<String, String> locals, String temperature,
                       List<String> formulas) {
        public Heat {
            rates = Collections.unmodifiableMap(new LinkedHashMap<>(rates));
            locals = Collections.unmodifiableMap(new LinkedHashMap<>(locals));
            formulas = List.copyOf(formulas);
        }
    }

    /** This model with another heat law: the one a part warms by when it writes none of its own. */
    ComponentModel withHeat(Heat law) {
        return new ComponentModel(name, family, kind, header, units, parameters, texts, pins, buses, nodes, digitalNodes, symbol, subcircuit,
                formulas, states, locals, localTypes, equations, behaviour, expectations, displays, measures, leads,
                displayUnits, displayCaptions, law);
    }

    /** A primitive or a nested model inside a component: one line of its internal netlist. */
    public record SubcircuitLine(String id, CircuitElementType type, String reference, String positive, String negative,
                                 String value, Map<String, String> bindings) {
        public SubcircuitLine { bindings = Map.copyOf(bindings); }
    }

    /**
     * This part as a member of the component it names as its family: the family's physics — its formulas, nodes,
     * states, equations, behaviour and parts — unless the part writes behaviour of its own; the family's parameters
     * with the part's values over them by name, and so its displays; the part's own pins, drawing and leads where it writes them,
     * else the family's. What the part states of itself (its header and expectations) is never inherited.
     *
     * <p>A base's {@code behaviour} is the one exception to "the part's own physics": it is inherited even by a part
     * that writes its own ({@link #behaviourOver}), with the discrete states it keeps — so a protocol is written once, in
     * the base, and each part writes only what it does with what the protocol hands it.</p>
     *
     * @param behaves whether the part's document writes behaviour of its own
     */
    ComponentModel memberOf(ComponentModel base, boolean behaves) {
        Map<String, Parameter> merged = new LinkedHashMap<>();
        base.parameters.forEach(parameter -> merged.put(parameter.name(), parameter));
        parameters.forEach(parameter -> merged.put(parameter.name(), parameter));
        return new ComponentModel(name, false, kind.isEmpty() ? base.kind : kind, header,
                units.isEmpty() ? base.units : units, List.copyOf(merged.values()), byName(base.texts, texts),
                pins.isEmpty() ? base.pins : pins,
                pins.isEmpty() ? base.buses : buses,
                behaves ? nodes : base.nodes, behaves ? digitalNodes : base.digitalNodes,
                symbol.isEmpty() ? base.symbol : symbol, behaves ? subcircuit : base.subcircuit,
                behaves ? formulas : base.formulas, behaves ? statesOver(base) : base.states, behaves ? locals : base.locals,
                behaves ? localTypes : base.localTypes,
                behaves ? equations : base.equations, behaves ? behaviourOver(base.behaviour) : base.behaviour, expectations,
                byName(base.displays, displays), measures.isEmpty() ? base.measures : measures,
                leads.isEmpty() ? base.leads : leads, byName(base.displayUnits, displayUnits),
                byName(base.displayCaptions, displayCaptions), heat == null ? base.heat : heat);
    }

    /**
     * The behaviour a part runs over its base's: the base's handlers, then its own, every one of them heard; a function
     * it writes again replaces the base's of that name, so the base's handlers call the part's — the way a protocol
     * written once in a base hands each byte to what the part does with it.
     */
    private ScriptNode behaviourOver(ScriptNode inherited) {
        if (inherited == null) {
            return behaviour;
        }
        if (behaviour == null) {
            return inherited;
        }
        Set<String> rewritten = behaviour.getFunctions().stream().map(FunctionDeclarationNode::getName).collect(Collectors.toSet());
        ScriptNode merged = new ScriptNode();
        merged.setName(behaviour.getName());
        for (Expression expression : inherited.getExpressions()) {
            boolean replaced = expression instanceof FunctionDeclarationNode function && rewritten.contains(function.getName());
            if (!replaced) {
                merged.addExpression(expression);
            }
        }
        behaviour.getExpressions().forEach(merged::addExpression);
        return merged;
    }

    /** The part's states over the discrete ones a behaving base keeps its protocol in: the base's by name, the part's over them. */
    private List<State> statesOver(ComponentModel base) {
        if (base.behaviour == null) {
            return states;
        }
        Map<String, State> merged = new LinkedHashMap<>();
        base.states.stream().filter(State::discrete).forEach(state -> merged.put(state.name(), state));
        states.forEach(state -> merged.put(state.name(), state));
        return List.copyOf(merged.values());
    }

    /** The family's entries with the part's over them by name: what a part displays adds to what its family does. */
    private static <V> Map<String, V> byName(Map<String, V> family, Map<String, V> own) {
        Map<String, V> merged = new LinkedHashMap<>(family);
        merged.putAll(own);
        return merged;
    }

    /** One header value, such as the summary or the family. */
    public Optional<String> headerValue(String key) {
        return Optional.ofNullable(header.get(key)).filter(values -> !values.isEmpty()).map(List::getFirst);
    }

    /**
     * How it fails when it overheats on a realistic bench, as its header writes it ({@code failure short drain source});
     * open when it says nothing.
     */
    public BenchConditions.Failure failure() {
        List<String> written = header.getOrDefault("failure", List.of());
        return written.isEmpty() ? BenchConditions.Failure.OPEN : BenchConditions.Failure.of(written);
    }

    /**
     * Whether it is shaped as a generator — an ideal source whose one equation, {@code voltage a -> b = …} or
     * {@code current a -> b = …}, runs between its only two pins, with no state, no behaviour and no node of its own.
     * What the equation reads on its right side is checked when it is compiled. An ideal ammeter is a voltage of 0
     * between two pins too, but it is a short to be read, not a source to hold a node.
     */
    public boolean isGenerator() {
        if (isInstrument() || equations.size() != 1 || pins.size() != 2 || !states.isEmpty() || behaviour != null
                || !nodes.isEmpty() || !digitalNodes.isEmpty()) {
            return false;
        }
        Equation equation = equations.getFirst();
        List<String> names = pins.stream().map(Pin::name).toList();
        boolean drives = equation.kind() == EquationKind.VOLTAGE || equation.kind() == EquationKind.CURRENT;
        return drives && names.contains(equation.positive()) && names.contains(equation.negative())
                && !equation.positive().equals(equation.negative());
    }

    /** Whether a generator holds a voltage, rather than drives a current — what holds a node, and so cuts a circuit. */
    public boolean generatesVoltage() {
        return equations.getFirst().kind() == EquationKind.VOLTAGE;
    }

    /** The pin a generator's equation holds above the other, and that other. */
    public String generatorPositive() {
        return equations.getFirst().positive();
    }

    public String generatorNegative() {
        return equations.getFirst().negative();
    }

    /** The logic family its digital pins belong to, when it names one. */
    public Optional<String> familyName() {
        return headerValue("family");
    }

    /** The blocks of the package; one unnamed block when it declares none. */
    public List<String> unitNames() {
        return units.isEmpty() ? List.of("") : units;
    }

    public Optional<Pin> pin(String name) {
        return pins.stream().filter(pin -> pin.name().equals(name)).findFirst();
    }

    public Optional<Parameter> parameter(String name) {
        return parameters.stream().filter(parameter -> parameter.name().equals(name)).findFirst();
    }

    /**
     * One thing an instrument reads ({@code measure { reading = plus - minus unit V }}): its expression as written — over
     * its pins' voltages, {@code current(pin)}, its states, its parameters, {@code power} and {@code time} — and its unit.
     */
    public record Measure(String expression, String unit, boolean signal) {
    }

    /** How the lead from one pin looks on a sheet ({@code leads { plus red stripes black }}): its colour, its stripes or null. */
    public record LeadLook(String colour, String stripes) {
    }

    /** Whether the component is an instrument: one that says what it reads. */
    public boolean isInstrument() {
        return !measures.isEmpty();
    }

    public boolean hasDigitalPins() {
        return pins.stream().anyMatch(pin -> pin.nature() == PinNature.DIGITAL);
    }

    /**
     * The names its equations carry a current between or read. A digital pin among them — an I/O pin with a pull-up
     * written as a current — puts its node in the analog solver, whatever else sits on that net: a level alone has no
     * voltage to read and takes no current. Worked out once per model by whoever asks often.
     */
    public Set<String> namesReachedByEquations() {
        Set<String> names = new HashSet<>();
        for (Equation equation : equations) {
            if (equation.positive() != null) {
                names.add(equation.positive());
            }
            if (equation.negative() != null) {
                names.add(equation.negative());
            }
            if (equation.formula() != null) {
                IDENTIFIER.matcher(equation.formula()).results().forEach(match -> names.add(match.group()));
            }
        }
        return names;
    }

    /**
     * The states its equations read, by name: every single state, and of a row only the slots the equations name —
     * {@code registers_12}, a register's bits feeding a pull-up — as {@code row_index}, the way a display reads them.
     */
    public List<String> equationStateNames() {
        Set<String> reached = namesReachedByEquations();
        List<String> names = new ArrayList<>();
        for (State state : states) {
            if (state.initial() instanceof Object[] row) {
                for (int index = 0; index < row.length; index++) {
                    if (reached.contains(state.name() + "_" + index)) {
                        names.add(state.name() + "_" + index);
                    }
                }
            } else {
                names.add(state.name());
            }
        }
        return names;
    }

    /** A name in a formula: what a pin, a state or a parameter is written as. */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

    /**
     * {@code name = 2.52 nA [per unit] ["what it is"]}.
     *
     * @param unit the unit its value was written in — {@code nA}, {@code K/W} —; empty when it was written without one,
     *             which says nothing about what it measures
     */
    public record Parameter(String name, double value, boolean perUnit, String description, String unit) {
    }

    /**
     * A pin: its package number in every block ({@code ""} for a single block or a shared pin), its nature, its
     * direction, and the words on it ({@code active-low}, {@code open-drain}).
     */
    public record Pin(String name, Map<String, Integer> numbers, boolean shared, PinNature nature, PinDirection direction,
                      List<String> attributes, String description) {

        /** The word that makes a driving pin pull only low: a 1 driven on it lets the line go. */
        public static final String OPEN_DRAIN = "open-drain";

        public Pin {
            numbers = Map.copyOf(numbers);
            attributes = List.copyOf(attributes);
        }

        /** Whether it drives its line: an output, or a bidirectional pin. */
        public boolean output() {
            return direction != PinDirection.INPUT;
        }

        /** Whether it reads its line: an input, or a bidirectional pin. */
        public boolean reads() {
            return direction != PinDirection.OUTPUT;
        }

        /** Whether it only pulls low, as I²C's lines and a wired-OR interrupt do. */
        public boolean openDrain() {
            return attributes.contains(OPEN_DRAIN);
        }
    }

    public enum PinNature { ANALOG, DIGITAL, POWER, GROUND }

    /** Which way a pin works: it reads its line, drives it, or both — a bus line a part both talks and listens on. */
    public enum PinDirection { INPUT, OUTPUT, BIDIRECTIONAL }

    /**
     * A state variable. A continuous one is an unknown of the solver with a {@code d(x)/dt} equation; a discrete one
     * is written only by the behaviour and read by the equations as a constant between events. Its initial value is
     * a number, {@code X}, or a list of them.
     */
    public record State(String name, Object initial, boolean discrete, String description) {
    }

    public enum EquationKind { CURRENT, VOLTAGE, STATE }

    /** {@code current a -> b = f}, {@code voltage a -> b = f} or {@code d(x)/dt = f}; {@code formula} is the right side as written. */
    public record Equation(EquationKind kind, String positive, String negative, String state, String formula, int line) {
    }

    /**
     * {@code expect "what" from "where in the datasheet"}: a bench, a measurement and what it must come to. The bench
     * is the document's parts, in the notation a circuit's are written in.
     *
     * @param temperature the bench's temperature in °C ({@code bench for 1 ms at 85 °C}); null for 25 °C
     */
    public record Expectation(String title, String reference, List<Block.Part> bench,
                              double duration, Double temperature, boolean realistic, List<String> measure, Criterion criterion,
                              int line) {
        public Expectation {
            bench = List.copyOf(bench);
            measure = List.copyOf(measure);
        }
    }

    /** What a measured value must be: near a typical value, between limits, or exactly a value. */
    public record Criterion(Kind kind, double minimum, double maximum, String exact) {

        public enum Kind { NEAR, WITHIN, IS }

        public boolean admits(double value) {
            return switch (kind) {
                case NEAR, WITHIN -> value >= minimum && value <= maximum;
                case IS -> Math.abs(value - Double.parseDouble(exact)) < 1e-9;
            };
        }

        public boolean admits(String value) {
            return kind == Kind.IS && exact.replace(" ", "").equals(value.replace(" ", ""));
        }

        public String describe() {
            return switch (kind) {
                case NEAR, WITHIN -> "[" + format(minimum) + ", " + format(maximum) + "]";
                case IS -> exact;
            };
        }

        private static String format(double value) {
            return Double.isInfinite(value) ? (value > 0 ? "∞" : "−∞") : String.format(Locale.ROOT, "%.4g", value);
        }
    }
}
