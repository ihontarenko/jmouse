package org.jmouse.parallax.spice;

import org.jmouse.parallax.component.TemperatureScale;

import org.jmouse.formula.Formula;
import org.jmouse.formula.Simplification;
import org.jmouse.formula.Substitution;
import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.spice.SpiceNetlist.Element;
import org.jmouse.parallax.spice.SpiceNetlist.Model;
import org.jmouse.parallax.spice.SpiceNetlist.Parameter;
import org.jmouse.parallax.spice.SpiceNetlist.Subcircuit;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Writes a {@link SpiceNetlist} as one jMX document: a {@code circuit} when the netlist has parts of its own, a
 * {@code component} when it is a {@code .subckt} alone.
 *
 * <h2>What each SPICE card becomes</h2>
 * <ul>
 *     <li>{@code R}, {@code C}, {@code L} → {@code resistor}, {@code capacitor}, {@code inductor}, by pin order.</li>
 *     <li>{@code V … DC} → {@code voltage-source}. {@code I … DC} → {@code current-source} with its nodes swapped:
 *     SPICE's current flows from the first node through the source to the second, jMX's source pushes its value out of
 *     its first pin.</li>
 *     <li>{@code SIN(…)} → {@code sine-generator}, the phase in radians. {@code PULSE(…)} → a {@code voltage-source}
 *     when it rises once from 0 and stays — a jMX circuit starts from rest, so every source is switched on at the
 *     start —, else {@code square-generator} with the duty taken at half height.</li>
 *     <li>{@code D}, {@code Q}, {@code M} and their {@code .model} → the component the model's name names when the
 *     library holds one of that device, else the engine's own {@code diode} for a diode model that writes only
 *     {@code IS}, else the device's carrier ({@link SpiceModelCarriers}) — the model's parameters set on it by the
 *     names {@link SpiceDevice} gives them.</li>
 *     <li>{@code X} → the library's component of the subcircuit's name when its pins are the subcircuit's pins, else
 *     the subcircuit written out as its parts ({@code X1_R1}).</li>
 *     <li>{@code .param} → {@code parameters}; {@code .tran} → {@code analysis transient}.</li>
 * </ul>
 *
 * <p>Nothing is guessed: whatever is carried across differently, or not at all, is a {@link SpiceNote}.</p>
 */
final class SpiceImporter {

    /** What jMX calls the parameter of each SPICE device parameter, and its unit, for writing it as a quantity. */
    /** SPICE's width and length of a MOSFET whose line writes none (ngspice's DEFW, DEFL), metres. */
    private static final double DEFAULT_GEOMETRY = 100e-6;

    private static final Map<String, String> UNITS = Map.ofEntries(
            Map.entry("I_saturation", "A"), Map.entry("R_series", "Ohm"), Map.entry("C_junction_zero_bias", "F"),
            Map.entry("V_junction", "V"), Map.entry("transit_time", "s"), Map.entry("V_breakdown", "V"),
            Map.entry("I_breakdown", "A"), Map.entry("E_gap", "V"), Map.entry("T_nominal", "K"),
            Map.entry("V_early", "V"), Map.entry("I_knee", "A"), Map.entry("R_base", "Ohm"),
            Map.entry("R_collector", "Ohm"), Map.entry("R_emitter", "Ohm"), Map.entry("C_base_emitter", "F"),
            Map.entry("C_base_collector", "F"), Map.entry("V_threshold", "V"),
            Map.entry("R_drain", "Ohm"), Map.entry("R_source", "Ohm"), Map.entry("C_bulk_drain", "F"),
            Map.entry("C_gate_source", "F"), Map.entry("C_gate_drain", "F"),
            Map.entry("resistance", "Ohm"), Map.entry("capacitance", "F"), Map.entry("inductance", "H"),
            Map.entry("U_output", "V"), Map.entry("I_output", "A"), Map.entry("amplitude", "V"), Map.entry("offset", "V"),
            Map.entry("high", "V"), Map.entry("low", "V"), Map.entry("frequency", "Hz"), Map.entry("phase", "rad"));

    /** Names jMX gives a meaning of its own, which a SPICE parameter may not take. */
    private static final Set<String> RESERVED = Set.of("temperature", "realistic", "tolerance_corner", "time", "pi", "e", "i", "tau",
            "phi", "inf");

    /** How long a circuit runs when the netlist has no {@code .tran}: jMX needs a span, SPICE's text gave none. */
    static final double DEFAULT_SPAN = 0.01;

    /** Edges at most this share of a period are ideal already; longer ones are made ideal, and that is said. */
    private static final double NEGLIGIBLE_EDGE = 1e-4;

    private final ComponentLibrary   library;
    private final SpiceModelCarriers carriers;
    private final List<SpiceNote>    notes = new ArrayList<>();
    private final JmxText.Names      nodes = new JmxText.Names("n");
    private final JmxText.Names      ids   = new JmxText.Names("U");
    private final Map<String, String> parameterNames = new LinkedHashMap<>();
    private final Map<String, Double> globalValues   = new LinkedHashMap<>();
    private SpiceNetlist netlist;
    private double       span = DEFAULT_SPAN;
    /** The subcircuit being written as a component, while it is. */
    private Subcircuit   converted;

    SpiceImporter(ComponentLibrary library, SpiceModelCarriers carriers) {
        this.library = library;
        this.carriers = carriers;
    }

    /**
     * The document written, what kind it is, and what was not carried across.
     *
     * @param notes each said once, in the order met: a model ten parts use is noted at its own line, once
     */
    record Written(String source, Kind kind, List<SpiceNote> notes) {
        Written {
            notes = List.copyOf(new java.util.LinkedHashSet<>(notes));
        }
    }

    /**
     * @param name the document's title; when the netlist holds subcircuits alone, also which of them to convert
     */
    Written write(SpiceNetlist netlist, String name) {
        this.netlist = netlist;
        notes.addAll(netlist.notes());
        netlist.analysis().ifPresent(analysis -> span = analysis.stop());
        globalValues.putAll(SpiceAnalyzer.values(netlist.parameters()));
        if (!netlist.elements().isEmpty()) {
            return new Written(circuit(netlist, name), Kind.CIRCUIT, notes);
        }
        if (!netlist.subcircuits().isEmpty()) {
            return new Written(component(chosenSubcircuit(netlist, name), name), Kind.COMPONENT, notes);
        }
        throw new SpiceParseException(SpicePosition.ofLine(1), "the text holds no parts and no '.subckt': there is "
                + "nothing to convert");
    }

    // ─── a circuit ──────────────────────────────────────────────────────────────────────────────────

    private String circuit(SpiceNetlist netlist, String name) {
        String title = titleOf(name, netlist.title().orElse(""), "Imported circuit");
        StringBuilder text = new StringBuilder();
        text.append("circuit ").append(JmxText.quoted(title)).append(" as ").append(ExperienceDocument.slugOf(title))
                .append(" {\n");
        text.append("    meta {\n        summary ").append(JmxText.quoted("Imported from a SPICE netlist"
                + netlist.title().filter(written -> !written.isBlank()).map(written -> ": " + written).orElse("")))
                .append("\n    }\n");
        List<String> parameters = parameterLines(netlist.parameters(), netlist.elements(), true);
        block(text, "parameters", parameters);
        List<String> parts = new ArrayList<>();
        for (Element element : netlist.elements()) {
            parts.addAll(parts(element, Context.CIRCUIT, Map.of(), "", Map.of(), new LinkedHashSet<>()));
        }
        block(text, "parts", parts);
        text.append("    ").append(analysis()).append('\n');
        text.append("}\n");
        noteRenamings();
        return text.toString();
    }

    private String analysis() {
        Optional<SpiceNetlist.Transient> analysis = netlist.analysis();
        if (analysis.isEmpty()) {
            notes.add(new SpiceNote(0, "the netlist has no '.tran'; a jMX circuit runs its transient for a span, so it "
                    + "runs for " + JmxText.quantity(DEFAULT_SPAN, "s") + " — change it to what the circuit needs"));
            return "analysis transient for " + JmxText.quantity(DEFAULT_SPAN, "s");
        }
        SpiceNetlist.Transient written = analysis.get();
        return written.start() == 0
                ? "analysis transient for " + JmxText.quantity(written.stop(), "s")
                : "analysis transient from " + JmxText.quantity(written.start(), "s") + " to "
                  + JmxText.quantity(written.stop(), "s");
    }

    // ─── a component ────────────────────────────────────────────────────────────────────────────────

    /** The subcircuit to convert: the one {@code name} names, else the one no other uses. */
    private static Subcircuit chosenSubcircuit(SpiceNetlist netlist, String name) {
        if (name != null && netlist.subcircuits().containsKey(name.strip().toLowerCase(Locale.ROOT))) {
            return netlist.subcircuits().get(name.strip().toLowerCase(Locale.ROOT));
        }
        Set<String> used = new java.util.HashSet<>();
        netlist.subcircuits().values().forEach(subcircuit -> subcircuit.elements().stream()
                .filter(SpiceNetlist.Instance.class::isInstance)
                .forEach(instance -> used.add(((SpiceNetlist.Instance) instance).subcircuit().toLowerCase(Locale.ROOT))));
        List<Subcircuit> top = netlist.subcircuits().entrySet().stream().filter(entry -> !used.contains(entry.getKey()))
                .map(Map.Entry::getValue).toList();
        if (top.size() == 1) {
            return top.getFirst();
        }
        throw new SpiceParseException(SpicePosition.ofLine(1), "the text defines the subcircuits "
                + String.join(", ", netlist.subcircuits().values().stream().map(Subcircuit::name).toList())
                + " and no parts; name the one to convert");
    }

    private String component(Subcircuit subcircuit, String name) {
        converted = subcircuit;
        String title = titleOf(name, subcircuit.name(), subcircuit.name());
        List<String> pins = subcircuit.pins().stream().map(nodes::of).toList();
        StringBuilder text = new StringBuilder();
        text.append("component ").append(JmxText.quoted(title)).append(" as ").append(ExperienceDocument.slugOf(title))
                .append(" {\n");
        text.append("    meta {\n        summary ").append(JmxText.quoted("Imported from SPICE subcircuit "
                + subcircuit.name())).append("\n    }\n");
        List<Parameter> declared = new ArrayList<>(netlist.parameters());
        declared.addAll(subcircuit.parameters());
        if (!netlist.parameters().isEmpty()) {
            notes.add(new SpiceNote(netlist.parameters().getFirst().position().line(), "the netlist's own parameters "
                    + "are the component's parameters too, since a component sees nothing outside itself"));
        }
        block(text, "parameters", parameterLines(declared, subcircuit.elements(), false));
        List<String> pinLines = new ArrayList<>();
        for (int index = 0; index < pins.size(); index++) {
            pinLines.add(pins.get(index) + " analog " + (index + 1) + " "
                    + JmxText.quoted("pin " + (index + 1) + " of subcircuit " + subcircuit.name()));
        }
        block(text, "pins", pinLines);
        List<String> parts = new ArrayList<>();
        for (Element element : subcircuit.elements()) {
            parts.addAll(parts(element, Context.COMPONENT, Map.of(), "", Map.of(), new LinkedHashSet<>()));
        }
        block(text, "parts", parts);
        int half = (pins.size() + 1) / 2;
        List<String> symbol = new ArrayList<>();
        symbol.add("left " + String.join(", ", pins.subList(0, half)));
        if (half < pins.size()) {
            symbol.add("right " + String.join(", ", pins.subList(half, pins.size())));
        }
        text.append("    symbol box {\n");
        symbol.forEach(line -> text.append("        ").append(line).append('\n'));
        text.append("    }\n");
        text.append("}\n");
        netlist.analysis().ifPresent(analysis -> notes.add(new SpiceNote(analysis.position().line(),
                "'.tran' belongs to a circuit; a component has no transient of its own; left out")));
        noteRenamings();
        return text.toString();
    }

    // ─── parameters ─────────────────────────────────────────────────────────────────────────────────

    /** {@code R = 1 kOhm "from .param"}, the unit taken from the first part whose value the parameter is. */
    private List<String> parameterLines(List<Parameter> parameters, List<Element> elements, boolean sliders) {
        for (Parameter parameter : parameters) {
            String name = parameter.name();
            if (RESERVED.contains(name.toLowerCase(Locale.ROOT))) {
                String renamed = name + "_parameter";
                notes.add(new SpiceNote(parameter.position().line(), "parameter '" + name + "' is a word jMX gives a "
                        + "meaning of its own; it is called '" + renamed + "'"));
                parameterNames.put(name, renamed);
            }
        }
        List<String> lines = new ArrayList<>();
        Set<String> written = new java.util.HashSet<>();
        for (Parameter parameter : parameters) {
            String name = parameterNames.getOrDefault(parameter.name(), parameter.name());
            if (!written.add(name.toLowerCase(Locale.ROOT))) {
                notes.add(new SpiceNote(parameter.position().line(), "parameter '" + parameter.name() + "' is declared "
                        + "again; the first value is kept"));
                continue;
            }
            String unit = unitOf(parameter.name(), elements);
            lines.add(name + " = " + value(parameter.value(), unit) + (sliders ? range(parameter.value(), unit) : "") + " "
                    + JmxText.quoted(originOf(parameter)));
        }
        if (sliders && lines.stream().anyMatch(line -> line.contains(" in "))) {
            notes.add(new SpiceNote(parameters.getFirst().position().line(), "a circuit's parameter is a slider with a "
                    + "range, and SPICE gives none: each spans a tenth to ten times its value"));
        }
        return lines;
    }

    /**
     * {@code in 100 Ohm .. 10 kOhm}: the range a circuit's slider needs, a tenth to ten times the value — none for a
     * parameter worked out from others, which is no slider.
     */
    private String range(Formula value, String unit) {
        Formula folded = Simplification.simplify(parameterNames.isEmpty() ? value : renamed(value));
        if (!folded.variables().isEmpty()) {
            return "";
        }
        double number = SpiceAnalyzer.evaluate(folded, Map.of());
        double low = number == 0 ? -1 : number > 0 ? number / 10 : number * 10;
        double high = number == 0 ? 1 : number > 0 ? number * 10 : number / 10;
        return " in " + JmxText.quantity(low, unit) + " .. " + JmxText.quantity(high, unit);
    }

    /** Where a parameter was declared: in the subcircuit being converted, or on a {@code .param} card of the netlist. */
    private String originOf(Parameter parameter) {
        if (converted != null && converted.parameters().contains(parameter)) {
            return "a parameter of subcircuit " + converted.name() + ", line " + parameter.position().line();
        }
        return "from .param, line " + parameter.position().line();
    }

    /** The unit of the first primitive value that is this parameter alone; none when no value is. */
    private static String unitOf(String parameter, List<Element> elements) {
        Formula alone = new Formula.Variable(parameter);
        for (Element element : elements) {
            if (element instanceof SpiceNetlist.Passive passive && passive.value().equals(alone)) {
                return switch (passive.kind()) {
                    case RESISTOR -> "Ohm";
                    case CAPACITOR -> "F";
                    case INDUCTOR -> "H";
                };
            }
            if (element instanceof SpiceNetlist.Source source && source.signal() instanceof SpiceNetlist.Constant constant
                    && constant.value().equals(alone)) {
                return source.voltage() ? "V" : "A";
            }
        }
        return "";
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    /** Where a part line is written, which decides where a model part's parameters go. */
    private enum Context {
        /** {@code U1 lm358 (in = a, out = b) gain = 2}: a model's parameters after its connections. */
        CIRCUIT,
        /** {@code U1 lm358 (in = a, out = b, gain = 2)}: inside a component every binding is in the parentheses. */
        COMPONENT
    }

    /**
     * The part lines one element becomes: one, none when it is left out, several for a subcircuit written out.
     *
     * @param pins       inside a subcircuit written out: its pins → the nodes outside they are on
     * @param prefix     inside a subcircuit written out: what its parts' ids and inner nodes are prefixed with
     * @param bound      inside a subcircuit written out: its parameters → their values at this instance
     * @param expanding  the subcircuits being written out around this one, to refuse a recursion
     */
    private List<String> parts(Element element, Context context, Map<String, String> pins, String prefix,
                               Map<String, Formula> bound, Set<String> expanding) {
        Placement placement = new Placement(pins, prefix, bound);
        return switch (element) {
            case SpiceNetlist.Passive passive -> List.of(passive(passive, placement));
            case SpiceNetlist.Source source -> source(source, placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Diode diode -> device(diode, diode.model(), SpiceNetlist.ModelType.D,
                    Map.of("anode", diode.anode(), "cathode", diode.cathode()), context, placement)
                    .map(List::of).orElse(List.of());
            case SpiceNetlist.Bipolar bipolar -> bipolarDevice(bipolar, context, placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Mosfet mosfet -> mosfetDevice(mosfet, context, placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Jfet jfet -> jfetDevice(jfet, context, placement).map(List::of).orElse(List.of());
            case SpiceNetlist.LinearControlled controlled -> controlledSource(controlled, placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Instance instance -> instance(instance, context, placement, expanding);
        };
    }

    /** How an element inside a subcircuit written out is placed: its nodes, its id and its parameters. */
    private record Placement(Map<String, String> pins, String prefix, Map<String, Formula> bound) {

        String node(String local) {
            if (local.equals("0")) {
                return "0";
            }
            if (prefix.isEmpty()) {
                return local;
            }
            String outside = pins.get(local);
            return outside != null ? outside : prefix + local;
        }

        String id(String written) {
            return prefix.isEmpty() ? written : prefix + written;
        }

        Formula value(Formula formula) {
            return bound.isEmpty() ? formula : Substitution.substitute(formula, bound);
        }
    }

    private String passive(SpiceNetlist.Passive passive, Placement placement) {
        String reference;
        String setting;
        switch (passive.kind()) {
            case RESISTOR -> {
                reference = "resistor";
                setting = "resistance";
            }
            case CAPACITOR -> {
                reference = "capacitor";
                setting = "capacitance";
            }
            default -> {
                reference = "inductor";
                setting = "inductance";
            }
        }
        return ids.of(placement.id(passive.name())) + " " + reference + " (" + nodes.of(placement.node(passive.positive()))
                + ", " + nodes.of(placement.node(passive.negative())) + ") " + setting + " = "
                + value(placement.value(passive.value()), UNITS.get(setting));
    }

    // ─── sources ────────────────────────────────────────────────────────────────────────────────────

    private Optional<String> source(SpiceNetlist.Source source, Placement placement) {
        String id = ids.of(placement.id(source.name()));
        String positive = nodes.of(placement.node(source.positive()));
        String negative = nodes.of(placement.node(source.negative()));
        int line = source.position().line();
        return switch (source.signal()) {
            case SpiceNetlist.Constant constant -> Optional.of(source.voltage()
                    ? id + " voltage-source (" + positive + ", " + negative + ") U_output = "
                      + value(placement.value(constant.value()), "V")
                    : id + " current-source (" + negative + ", " + positive + ") I_output = "
                      + value(placement.value(constant.value()), "A"));
            case SpiceNetlist.Sine sine -> sine(source, sine, id, positive, negative, placement);
            case SpiceNetlist.Pulse pulse -> pulse(source, pulse, id, positive, negative, placement, line);
        };
    }

    private Optional<String> sine(SpiceNetlist.Source source, SpiceNetlist.Sine sine, String id, String positive,
                                  String negative, Placement placement) {
        int line = source.position().line();
        if (!source.voltage()) {
            notes.add(new SpiceNote(line, source.name() + " is an alternating current source, which jMX has no "
                    + "component for; left out"));
            return Optional.empty();
        }
        Formula frequency = sine.frequency();
        if (frequency == null) {
            if (netlist.analysis().isEmpty()) {
                notes.add(new SpiceNote(line, source.name() + "'s SIN writes no frequency, and without '.tran' there is "
                        + "no span to take one period over; left out"));
                return Optional.empty();
            }
            frequency = new Formula.Constant(1 / span);
            notes.add(new SpiceNote(line, source.name() + "'s SIN writes no frequency; SPICE's is one period over the "
                    + "transient, " + JmxText.quantity(1 / span, "Hz")));
        }
        if (!isZero(placement.value(sine.delay()))) {
            notes.add(new SpiceNote(line, source.name() + "'s delay has no counterpart in a sine-generator, which starts "
                    + "at time 0; left out"));
        }
        if (!isZero(placement.value(sine.damping()))) {
            notes.add(new SpiceNote(line, source.name() + "'s damping has no counterpart in a sine-generator, whose "
                    + "amplitude stays; left out"));
        }
        List<String> settings = new ArrayList<>();
        settings.add("amplitude = " + value(placement.value(sine.amplitude()), "V"));
        settings.add("frequency = " + value(placement.value(frequency), "Hz"));
        if (!isZero(placement.value(sine.offset()))) {
            settings.add("offset = " + value(placement.value(sine.offset()), "V"));
        }
        if (!isZero(placement.value(sine.phase()))) {
            Formula radians = new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Binary(Formula.Operator.MULTIPLY,
                    placement.value(sine.phase()), new Formula.NamedConstant(org.jmouse.formula.MathematicalConstant.PI)),
                    new Formula.Constant(180));
            settings.add("phase = " + value(radians, "rad"));
        }
        return Optional.of(id + " sine-generator (positive = " + positive + ", negative = " + negative + ") "
                + String.join(", ", settings));
    }

    /**
     * A step when the pulse rises once and stays for the whole run; else a square, its duty measured at half height —
     * {@code (width + (rise + fall) / 2) / period}, which keeps the square's average the pulse's.
     */
    private Optional<String> pulse(SpiceNetlist.Source source, SpiceNetlist.Pulse pulse, String id, String positive,
                                   String negative, Placement placement, int line) {
        double delay = number(placement.value(pulse.delay()));
        double rise = number(placement.value(pulse.rise()));
        double fall = number(placement.value(pulse.fall()));
        double width = pulse.width() == null ? Double.POSITIVE_INFINITY : number(placement.value(pulse.width()));
        double period = pulse.period() == null ? Double.POSITIVE_INFINITY : number(placement.value(pulse.period()));
        double initial = number(placement.value(pulse.initial()));
        if (Double.isNaN(delay + rise + fall + width + period + initial)) {
            notes.add(new SpiceNote(line, source.name() + "'s PULSE times cannot be worked out from the parameters, "
                    + "which deciding between a step and a square needs; left out"));
            return Optional.empty();
        }
        boolean once = delay + rise + width >= span || Double.isInfinite(period);
        if (once) {
            if (initial != 0 || delay != 0) {
                notes.add(new SpiceNote(line, source.name() + "'s PULSE steps from " + initial + " after " + delay
                        + " s; a jMX source steps from 0 at time 0; left out"));
                return Optional.empty();
            }
            if (rise > span * NEGLIGIBLE_EDGE) {
                notes.add(new SpiceNote(line, source.name() + "'s rise of " + JmxText.quantity(rise, "s")
                        + " is made an ideal step"));
            }
            return Optional.of(source.voltage()
                    ? id + " voltage-source (" + positive + ", " + negative + ") U_output = "
                      + value(placement.value(pulse.pulsed()), "V")
                    : id + " current-source (" + negative + ", " + positive + ") I_output = "
                      + value(placement.value(pulse.pulsed()), "A"));
        }
        if (!source.voltage()) {
            notes.add(new SpiceNote(line, source.name() + " is a pulsed current source, which jMX has no component for; "
                    + "left out"));
            return Optional.empty();
        }
        if (delay != 0) {
            notes.add(new SpiceNote(line, source.name() + "'s delay of " + JmxText.quantity(delay, "s") + " has no "
                    + "counterpart in a square-generator, which starts high at time 0; left out"));
        }
        if (rise > period * NEGLIGIBLE_EDGE || fall > period * NEGLIGIBLE_EDGE) {
            notes.add(new SpiceNote(line, source.name() + "'s rise and fall are made ideal edges; the duty is taken at "
                    + "half height, so the average stays"));
        }
        Formula periodFormula = placement.value(pulse.period());
        Formula halfHeightWidth = new Formula.Binary(Formula.Operator.ADD, placement.value(pulse.width()),
                new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Binary(Formula.Operator.ADD,
                        placement.value(pulse.rise()), placement.value(pulse.fall())), new Formula.Constant(2)));
        Formula duty = new Formula.Binary(Formula.Operator.DIVIDE, halfHeightWidth, periodFormula);
        double dutyValue = (width + (rise + fall) / 2) / period;
        if (dutyValue < 0.01 || dutyValue > 0.99) {
            notes.add(new SpiceNote(line, source.name() + "'s duty " + JmxText.number(dutyValue) + " is outside a "
                    + "square-generator's 0.01 … 0.99"));
        }
        List<String> settings = new ArrayList<>();
        settings.add("high = " + value(placement.value(pulse.pulsed()), "V"));
        if (initial != 0) {
            settings.add("low = " + value(placement.value(pulse.initial()), "V"));
        }
        settings.add("frequency = " + value(new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Constant(1),
                periodFormula), "Hz"));
        settings.add("duty = " + value(duty, ""));
        return Optional.of(id + " square-generator (positive = " + positive + ", negative = " + negative + ") "
                + String.join(", ", settings));
    }

    // ─── devices and their models ───────────────────────────────────────────────────────────────────

    private Optional<String> bipolarDevice(SpiceNetlist.Bipolar bipolar, Context context, Placement placement) {
        Model model = netlist.models().get(bipolar.model().toLowerCase(Locale.ROOT));
        SpiceNetlist.ModelType type = model != null && model.type() == SpiceNetlist.ModelType.PNP
                ? SpiceNetlist.ModelType.PNP : SpiceNetlist.ModelType.NPN;
        return device(bipolar, bipolar.model(), type, Map.of("collector", bipolar.collector(), "base", bipolar.base(),
                "emitter", bipolar.emitter()), context, placement);
    }

    private Optional<String> mosfetDevice(SpiceNetlist.Mosfet mosfet, Context context, Placement placement) {
        Model model = netlist.models().get(mosfet.model().toLowerCase(Locale.ROOT));
        SpiceNetlist.ModelType type = model != null && model.type() == SpiceNetlist.ModelType.PMOS
                ? SpiceNetlist.ModelType.PMOS : SpiceNetlist.ModelType.NMOS;
        if (!mosfet.bulk().equals(mosfet.source())) {
            notes.add(new SpiceNote(mosfet.position().line(), mosfet.name() + "'s bulk is on " + mosfet.bulk()
                    + ", not its source; a jMX MOSFET's body is its source, so that connection is left out"));
        }
        return device(mosfet, mosfet.model(), type, Map.of("drain", mosfet.drain(), "gate", mosfet.gate(), "source",
                mosfet.source()), context, placement, geometry(mosfet));
    }

    /**
     * What a MOSFET's width and length do to its model: a jMX MOSFET is the whole channel (W = L = 1), so KP is
     * multiplied by W / L and the overlap capacitances, written per metre of width, by W. A line that writes neither
     * takes SPICE's default of 100 µm for both: KP unchanged, the overlaps 10⁻⁴ of what the model writes.
     */
    private static Map<String, Formula> geometry(SpiceNetlist.Mosfet mosfet) {
        Formula width = mosfet.width() == null ? new Formula.Constant(DEFAULT_GEOMETRY) : mosfet.width();
        Formula length = mosfet.length() == null ? new Formula.Constant(DEFAULT_GEOMETRY) : mosfet.length();
        Formula ratio = new Formula.Binary(Formula.Operator.DIVIDE, width, length);
        return Map.of("transconductance", ratio, "C_gate_source", width, "C_gate_drain", width);
    }

    private Optional<String> jfetDevice(SpiceNetlist.Jfet jfet, Context context, Placement placement) {
        Model model = netlist.models().get(jfet.model().toLowerCase(Locale.ROOT));
        SpiceNetlist.ModelType type = model != null && model.type() == SpiceNetlist.ModelType.PJF
                ? SpiceNetlist.ModelType.PJF : SpiceNetlist.ModelType.NJF;
        return device(jfet, jfet.model(), type, Map.of("drain", jfet.drain(), "gate", jfet.gate(), "source", jfet.source()),
                context, placement);
    }

    /**
     * A linear E or G as the library's controlled source of its kind, its gain on the part line; left out with a note
     * where the library has no such component.
     */
    private Optional<String> controlledSource(SpiceNetlist.LinearControlled controlled, Placement placement) {
        String reference = controlled.voltage() ? "voltage-controlled-voltage-source" : "voltage-controlled-current-source";
        if (library.source(reference).isEmpty()) {
            notes.add(new SpiceNote(controlled.position().line(), controlled.name() + " (a voltage-controlled "
                    + (controlled.voltage() ? "voltage" : "current") + " source) needs the component " + reference
                    + ", which the library has not; left out"));
            return Optional.empty();
        }
        String setting = controlled.voltage() ? "gain" : "transconductance";
        return Optional.of(ids.of(placement.id(controlled.name())) + " " + reference
                + " (positive = " + nodes.of(placement.node(controlled.positive()))
                + ", negative = " + nodes.of(placement.node(controlled.negative()))
                + ", control_positive = " + nodes.of(placement.node(controlled.controlPositive()))
                + ", control_negative = " + nodes.of(placement.node(controlled.controlNegative()))
                + ") " + setting + " = " + value(placement.value(controlled.gain()), ""));
    }

    /**
     * A diode or a transistor: the component its model names, the engine's own diode, or the device's carrier — with
     * the model's parameters on it where they differ from the component's own.
     *
     * @param terminals each of the device's pins, by its jMX name → the SPICE node it is on
     */
    private Optional<String> device(Element element, String modelName, SpiceNetlist.ModelType expected,
                                    Map<String, String> terminals, Context context, Placement placement) {
        return device(element, modelName, expected, terminals, context, placement, Map.of());
    }

    /**
     * The same, with some of the model's parameters scaled for this one element: {@code scale} names a jMX parameter and
     * what it is multiplied by — a MOSFET's width and length, which SPICE writes on the device line.
     */
    private Optional<String> device(Element element, String modelName, SpiceNetlist.ModelType expected,
                                    Map<String, String> terminals, Context context, Placement placement,
                                    Map<String, Formula> scale) {
        int line = element.position().line();
        Model model = netlist.models().get(modelName.toLowerCase(Locale.ROOT));
        if (model == null) {
            notes.add(new SpiceNote(line, element.name() + "'s model '" + modelName + "' is not in the netlist; left out"));
            return Optional.empty();
        }
        boolean matches = switch (expected) {
            case D -> model.type() == SpiceNetlist.ModelType.D;
            case NPN, PNP -> model.type() == SpiceNetlist.ModelType.NPN || model.type() == SpiceNetlist.ModelType.PNP;
            case NMOS, PMOS -> model.type() == SpiceNetlist.ModelType.NMOS || model.type() == SpiceNetlist.ModelType.PMOS;
            case NJF, PJF -> model.type() == SpiceNetlist.ModelType.NJF || model.type() == SpiceNetlist.ModelType.PJF;
        };
        if (!matches) {
            notes.add(new SpiceNote(line, element.name() + "'s model '" + modelName + "' is a " + model.type()
                    + " model, not one for this element; left out"));
            return Optional.empty();
        }
        SpiceDevice device = SpiceDevice.of(model.type());
        String id = ids.of(placement.id(element.name()));
        Map<String, Formula> given = new LinkedHashMap<>();
        model.parameters().forEach((spiceName, formula) -> {
            Optional<SpiceDevice.ParameterName> named = device.bySpiceName(spiceName);
            if (named.isEmpty()) {
                notes.add(new SpiceNote(model.position().line(), "model " + model.name() + "'s " + spiceName
                        + " has no jMX counterpart; left out"));
            } else {
                given.put(named.get().jmx(), inJmxUnits(named.get(), formula));
            }
        });
        if (device == SpiceDevice.DIODE && isPrimitiveDiode(given)) {
            Formula saturation = given.getOrDefault("I_saturation", new Formula.Constant(1e-14));
            return Optional.of(id + " diode (" + nodes.of(placement.node(terminals.get("anode"))) + ", "
                    + nodes.of(placement.node(terminals.get("cathode"))) + ") I_saturation = "
                    + value(placement.value(saturation), "A"));
        }
        Optional<Carried> carried = componentFor(model, device, line);
        if (carried.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> bindings = new LinkedHashMap<>();
        for (String pin : device.pins()) {
            bindings.put(pin, nodes.of(placement.node(terminals.get(pin))));
        }
        Map<String, String> overrides = overrides(model, device, given, carried.get(), line, scale);
        return Optional.of(modelPart(id, carried.get().reference(), bindings, overrides, context));
    }

    /** A model parameter in jMX's units: a temperature in kelvin where SPICE writes °C, the rest as written. */
    private static Formula inJmxUnits(SpiceDevice.ParameterName parameter, Formula formula) {
        if (!parameter.temperature()) {
            return formula;
        }
        if (formula instanceof Formula.Constant constant) {
            return new Formula.Constant(parameter.toJmx(constant.value()));
        }
        return new Formula.Binary(Formula.Operator.ADD, formula, new Formula.Constant(TemperatureScale.ZERO_CELSIUS));
    }

    /** The engine's own diode is SPICE's with nothing but {@code IS}: emission 1, no resistance, no charge. */
    private static boolean isPrimitiveDiode(Map<String, Formula> given) {
        for (Map.Entry<String, Formula> entry : given.entrySet()) {
            switch (entry.getKey()) {
                case "I_saturation", "T_nominal" -> {
                }
                case "emission_coefficient" -> {
                    if (!(entry.getValue() instanceof Formula.Constant constant) || constant.value() != 1) {
                        return false;
                    }
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    /** A component a model becomes, and its parameters' own values. */
    private record Carried(String reference, Map<String, Double> parameters, boolean namedByModel) {
    }

    /** The library's component the model's name names, when it is that device; else the device's carrier. */
    private Optional<Carried> componentFor(Model model, SpiceDevice device, int line) {
        for (String candidate : candidates(model.name())) {
            Optional<Carried> named = carrier(candidate, device, true);
            if (named.isPresent()) {
                return named;
            }
        }
        Optional<String> carrier = carriers.carrierOf(device);
        if (carrier.isEmpty()) {
            notes.add(new SpiceNote(model.position().line(), "model " + model.name() + " (" + device.modelType() + ") names no component "
                    + "of the library, and nothing carries a " + device.modelType() + " device; left out"));
            return Optional.empty();
        }
        Optional<Carried> carried = carrier(carrier.get(), device, false);
        if (carried.isEmpty()) {
            notes.add(new SpiceNote(model.position().line(), "the " + device.modelType() + " carrier '" + carrier.get() + "' is not a "
                    + device.componentType() + " the library holds; model " + model.name() + " is left out"));
            return Optional.empty();
        }
        notes.add(new SpiceNote(model.position().line(), "model " + model.name() + " names no component of the library; its parts are "
                + carrier.get() + " parts with the model's parameters"));
        return carried;
    }

    /** The references a SPICE name may stand for: as written in lower case, and with {@code _} for {@code -}. */
    private static List<String> candidates(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("_") ? List.of(lower, lower.replace('_', '-')) : List.of(lower);
    }

    private Optional<Carried> carrier(String reference, SpiceDevice device, boolean namedByModel) {
        Optional<String> source = library.source(reference);
        if (source.isEmpty()) {
            return Optional.empty();
        }
        ExperienceDocument document;
        ComponentModel model;
        try {
            document = ExperienceReader.readOne(source.get(), reference);
            model = ComponentDocuments.model(document, library);
        } catch (RuntimeException unreadable) {
            return Optional.empty();
        }
        List<String> symbols = document.block(Block.Symbol.class).map(Block.Symbol::uses).orElse(List.of());
        Optional<SpiceDevice> its = document.type().flatMap(type -> SpiceDevice.ofComponent(type, symbols));
        if (its.isEmpty() || its.get() != device) {
            return Optional.empty();
        }
        Set<String> pins = new java.util.HashSet<>();
        model.pins().forEach(pin -> pins.add(pin.name()));
        if (!pins.containsAll(device.pins())) {
            return Optional.empty();
        }
        Map<String, Double> parameters = new LinkedHashMap<>();
        model.parameters().forEach(parameter -> parameters.put(parameter.name(), parameter.value()));
        return Optional.of(new Carried(reference, parameters, namedByModel));
    }

    /**
     * The model's parameters as the part's settings: each one written, SPICE's default for each one not written that
     * jMX can say, and only those that differ from the component's own values.
     */
    private Map<String, String> overrides(Model model, SpiceDevice device, Map<String, Formula> given, Carried carried,
                                          int line, Map<String, Formula> scale) {
        Map<String, String> overrides = new LinkedHashMap<>();
        List<String> kept = new ArrayList<>();
        for (SpiceDevice.ParameterName parameter : device.parameters()) {
            if (!carried.parameters().containsKey(parameter.jmx())) {
                if (given.containsKey(parameter.jmx())) {
                    notes.add(new SpiceNote(model.position().line(), "model " + model.name() + "'s " + parameter.spice()
                            + " has no counterpart in " + carried.reference() + "; left out"));
                }
                continue;
            }
            Formula value = given.get(parameter.jmx());
            // SPICE writes "none" as 0 for exactly the parameters jMX cannot say it for: VAF=0 and IKR=0 are infinite,
            // RS=0 and RB=0 drop a node. Such a 0 keeps the component's own value, as an unwritten one does.
            if (parameter.spiceDefault() == null && value instanceof Formula.Constant constant && constant.value() == 0) {
                value = null;
            }
            if (value == null) {
                if (parameter.spiceDefault() == null) {
                    kept.add(parameter.spice());
                    continue;
                }
                value = new Formula.Constant(parameter.toJmx(parameter.spiceDefault()));
            }
            if (scale.containsKey(parameter.jmx())) {
                value = new Formula.Binary(Formula.Operator.MULTIPLY, value, scale.get(parameter.jmx()));
            }
            Formula folded = Simplification.simplify(value);
            double own = carried.parameters().get(parameter.jmx());
            if (folded.variables().isEmpty()) {
                double number = SpiceAnalyzer.evaluate(folded, Map.of());
                if (Math.abs(number - own) <= 1e-9 * Math.max(Math.abs(own), Math.abs(number))) {
                    continue;
                }
            }
            overrides.put(parameter.jmx(), value(folded, UNITS.getOrDefault(parameter.jmx(), "")));
        }
        if (!kept.isEmpty()) {
            notes.add(new SpiceNote(model.position().line(), "model " + model.name() + " does not write " + String.join(", ", kept)
                    + ", and SPICE's default for " + (kept.size() == 1 ? "it" : "them") + " — no resistance, no "
                    + "breakdown, no limit — is not a value jMX can write; " + carried.reference() + "'s own "
                    + (kept.size() == 1 ? "value is" : "values are") + " kept"));
        }
        return overrides;
    }

    private static String modelPart(String id, String reference, Map<String, String> bindings,
                                    Map<String, String> overrides, Context context) {
        List<String> connections = new ArrayList<>();
        bindings.forEach((pin, net) -> connections.add(pin + " = " + net));
        List<String> settings = new ArrayList<>();
        overrides.forEach((name, written) -> settings.add(name + " = " + written));
        if (context == Context.COMPONENT) {
            connections.addAll(settings);
            return id + " " + reference + " (" + String.join(", ", connections) + ")";
        }
        return (id + " " + reference + " (" + String.join(", ", connections) + ") " + String.join(", ", settings)).strip();
    }

    // ─── subcircuit instances ───────────────────────────────────────────────────────────────────────

    private List<String> instance(SpiceNetlist.Instance instance, Context context, Placement placement,
                                  Set<String> expanding) {
        int line = instance.position().line();
        String key = instance.subcircuit().toLowerCase(Locale.ROOT);
        Subcircuit subcircuit = netlist.subcircuits().get(key);
        if (subcircuit == null) {
            notes.add(new SpiceNote(line, instance.name() + "'s subcircuit '" + instance.subcircuit() + "' is not in the "
                    + "netlist, so the order of its pins is unknown; left out"));
            return List.of();
        }
        if (subcircuit.pins().size() != instance.nodes().size()) {
            throw new SpiceParseException(instance.position(), instance.name() + " connects " + instance.nodes().size()
                    + " nodes, and subcircuit " + subcircuit.name() + " has " + subcircuit.pins().size() + " pins");
        }
        Optional<String> asComponent = libraryInstance(instance, subcircuit, context, placement);
        if (asComponent.isPresent()) {
            return List.of(asComponent.get());
        }
        if (!expanding.add(key)) {
            throw new SpiceParseException(instance.position(), "subcircuit " + subcircuit.name() + " contains itself");
        }
        Map<String, String> pins = new LinkedHashMap<>();
        for (int index = 0; index < subcircuit.pins().size(); index++) {
            pins.put(subcircuit.pins().get(index), placement.node(instance.nodes().get(index)));
        }
        Map<String, Formula> bound = boundParameters(instance, subcircuit, placement);
        String prefix = placement.id(instance.name()) + "_";
        List<String> lines = new ArrayList<>();
        for (Element inner : subcircuit.elements()) {
            lines.addAll(parts(inner, context, pins, prefix, bound, expanding));
        }
        expanding.remove(key);
        notes.add(new SpiceNote(line, instance.name() + ": subcircuit " + subcircuit.name() + " is written out as its "
                + "parts (" + prefix + "…); convert the .subckt alone to keep it as a component"));
        return lines;
    }

    /** The component of the subcircuit's name, when the library holds one with exactly the subcircuit's pins. */
    private Optional<String> libraryInstance(SpiceNetlist.Instance instance, Subcircuit subcircuit, Context context,
                                             Placement placement) {
        for (String candidate : candidates(subcircuit.name())) {
            Optional<String> source = library.source(candidate);
            if (source.isEmpty()) {
                continue;
            }
            ComponentModel model;
            try {
                model = ComponentDocuments.read(source.get(), candidate, library);
            } catch (RuntimeException unreadable) {
                continue;
            }
            List<String> pins = model.pins().stream().map(ComponentModel.Pin::name).toList();
            List<String> spicePins = subcircuit.pins().stream().map(pin -> pin.toLowerCase(Locale.ROOT)).toList();
            if (!pins.stream().map(pin -> pin.toLowerCase(Locale.ROOT)).toList().equals(spicePins)) {
                continue;
            }
            Map<String, String> bindings = new LinkedHashMap<>();
            for (int index = 0; index < pins.size(); index++) {
                bindings.put(pins.get(index), nodes.of(placement.node(instance.nodes().get(index))));
            }
            Map<String, String> overrides = new LinkedHashMap<>();
            Set<String> known = new java.util.HashSet<>();
            model.parameters().forEach(parameter -> known.add(parameter.name()));
            instance.parameters().forEach((name, formula) -> {
                String declared = known.stream().filter(parameter -> parameter.equalsIgnoreCase(name)).findFirst().orElse(null);
                if (declared == null) {
                    notes.add(new SpiceNote(instance.position().line(), instance.name() + "'s " + name + " is not a "
                            + "parameter of " + candidate + "; left out"));
                } else {
                    overrides.put(declared, value(placement.value(formula), UNITS.getOrDefault(declared, "")));
                }
            });
            return Optional.of(modelPart(ids.of(placement.id(instance.name())), candidate, bindings, overrides, context));
        }
        return Optional.empty();
    }

    /** The subcircuit's parameters at this instance: the instance's values, else the defaults, each in order. */
    private Map<String, Formula> boundParameters(SpiceNetlist.Instance instance, Subcircuit subcircuit,
                                                 Placement placement) {
        Map<String, Formula> bound = new LinkedHashMap<>();
        for (Parameter parameter : subcircuit.parameters()) {
            Formula given = instance.parameters().entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(parameter.name())).map(Map.Entry::getValue)
                    .findFirst().map(placement::value).orElse(null);
            bound.put(parameter.name(), given != null ? given : Substitution.substitute(parameter.value(), bound));
        }
        instance.parameters().keySet().stream()
                .filter(name -> subcircuit.parameters().stream().noneMatch(parameter -> parameter.name().equalsIgnoreCase(name)))
                .forEach(name -> notes.add(new SpiceNote(instance.position().line(), instance.name() + "'s " + name
                        + " is not a parameter of subcircuit " + subcircuit.name() + "; left out")));
        return bound;
    }

    // ─── values ─────────────────────────────────────────────────────────────────────────────────────

    /** A value as jMX writes it: a quantity with its unit when it is a number, else the jMF expression. */
    private String value(Formula formula, String unit) {
        Formula renamed = parameterNames.isEmpty() ? formula : renamed(formula);
        Formula folded = Simplification.simplify(renamed);
        if (folded.variables().isEmpty()) {
            return JmxText.quantity(SpiceAnalyzer.evaluate(folded, Map.of()), unit == null ? "" : unit);
        }
        String expression = SpiceExpressions.jmf(folded);
        // ⚠️ A part's setting that begins with a digit is read as a quantity — '1 / f' as 1 with a unit it ignores —, so
        // an expression that would begin with one is written in parentheses.
        char first = expression.charAt(0);
        return Character.isDigit(first) || first == '.' ? "(" + expression + ")" : expression;
    }

    private Formula renamed(Formula formula) {
        Map<String, Formula> replacements = new LinkedHashMap<>();
        parameterNames.forEach((from, to) -> replacements.put(from, new Formula.Variable(to)));
        return Substitution.substitute(formula, replacements);
    }

    /** A value as a number, worked out from the netlist's parameters; NaN when it cannot be. */
    private double number(Formula formula) {
        try {
            return SpiceAnalyzer.evaluate(formula, globalValues);
        } catch (RuntimeException unknown) {
            return Double.NaN;
        }
    }

    private static boolean isZero(Formula formula) {
        return formula instanceof Formula.Constant constant && constant.value() == 0;
    }

    // ─── writing ────────────────────────────────────────────────────────────────────────────────────

    private static void block(StringBuilder text, String name, List<String> lines) {
        if (lines.isEmpty() && !name.equals("parts")) {
            return;
        }
        text.append("    ").append(name).append(" {\n");
        lines.forEach(line -> text.append("        ").append(line).append('\n'));
        text.append("    }\n");
    }

    private static String titleOf(String name, String fallback, String last) {
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.strip();
        }
        return last;
    }

    private void noteRenamings() {
        if (!nodes.renamed().isEmpty()) {
            notes.add(new SpiceNote(0, "nodes renamed to be jMX names: " + renamings(nodes.renamed())));
        }
        if (!ids.renamed().isEmpty()) {
            notes.add(new SpiceNote(0, "parts renamed to be jMX names: " + renamings(ids.renamed())));
        }
    }

    private static String renamings(Map<String, String> renamed) {
        return String.join(", ", renamed.entrySet().stream().map(entry -> entry.getKey() + " → " + entry.getValue())
                .toList());
    }
}
