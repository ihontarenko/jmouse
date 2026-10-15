package org.jmouse.parallax.circuit;

import org.jmouse.parallax.script.Block;
import org.jmouse.script.el.node.ScriptNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
                             List<String> units, List<Parameter> parameters, List<Pin> pins, List<String> nodes, List<String> digitalNodes,
                             List<String> symbol, List<SubcircuitLine> subcircuit, List<String> formulas, List<State> states,
                             Map<String, String> locals, List<Equation> equations, ScriptNode behaviour,
                             List<Expectation> expectations, Map<String, String> displays, Map<String, Measure> measures,
                             Map<String, LeadLook> leads, Map<String, String> displayUnits,
                             Map<String, String> displayCaptions) {

    public ComponentModel {
        header = Map.copyOf(header);
        units = List.copyOf(units);
        parameters = List.copyOf(parameters);
        pins = List.copyOf(pins);
        nodes = List.copyOf(nodes);
        digitalNodes = List.copyOf(digitalNodes);
        symbol = List.copyOf(symbol);
        subcircuit = List.copyOf(subcircuit);
        formulas = List.copyOf(formulas);
        states = List.copyOf(states);
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

    /** A primitive or a nested model inside a component: one line of its internal netlist. */
    public record SubcircuitLine(String id, char kind, String reference, String positive, String negative,
                                 String waveform, String value, String frequency, Map<String, String> bindings) {
        public SubcircuitLine { bindings = Map.copyOf(bindings); }
    }

    /**
     * This part as a member of the component it names as its family: the family's physics — its formulas, nodes,
     * states, equations, behaviour and parts — unless the part writes behaviour of its own; the family's parameters
     * with the part's values over them by name; the part's own pins, drawing, displays and leads where it writes them,
     * else the family's. What the part states of itself (its header and expectations) is never inherited.
     *
     * @param behaves whether the part's document writes behaviour of its own
     */
    ComponentModel memberOf(ComponentModel base, boolean behaves) {
        Map<String, Parameter> merged = new LinkedHashMap<>();
        base.parameters.forEach(parameter -> merged.put(parameter.name(), parameter));
        parameters.forEach(parameter -> merged.put(parameter.name(), parameter));
        return new ComponentModel(name, false, kind.isEmpty() ? base.kind : kind, header,
                units.isEmpty() ? base.units : units, List.copyOf(merged.values()), pins.isEmpty() ? base.pins : pins,
                behaves ? nodes : base.nodes, behaves ? digitalNodes : base.digitalNodes,
                symbol.isEmpty() ? base.symbol : symbol, behaves ? subcircuit : base.subcircuit,
                behaves ? formulas : base.formulas, behaves ? states : base.states, behaves ? locals : base.locals,
                behaves ? equations : base.equations, behaves ? behaviour : base.behaviour, expectations,
                displays.isEmpty() ? base.displays : displays, measures.isEmpty() ? base.measures : measures,
                leads.isEmpty() ? base.leads : leads, displayUnits.isEmpty() ? base.displayUnits : displayUnits,
                displayCaptions.isEmpty() ? base.displayCaptions : displayCaptions);
    }

    /** One header value, such as the summary or the family. */
    public Optional<String> headerValue(String key) {
        return Optional.ofNullable(header.get(key)).filter(values -> !values.isEmpty()).map(List::getFirst);
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

    /** {@code name = 2.52 nA [per unit] ["what it is"]}. */
    public record Parameter(String name, double value, boolean perUnit, String description) {
    }

    /**
     * A pin: its package number in every block ({@code ""} for a single block or a shared pin), its nature, whether it
     * is an output, and the words on it ({@code active-low}).
     */
    public record Pin(String name, Map<String, Integer> numbers, boolean shared, PinNature nature, boolean output,
                      List<String> attributes, String description) {
        public Pin {
            numbers = Map.copyOf(numbers);
            attributes = List.copyOf(attributes);
        }
    }

    public enum PinNature { ANALOG, DIGITAL, POWER, GROUND }

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
