package org.jmouse.cad.spice;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.DocumentParser;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationLines;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.spice.SpiceNetlist.Element;
import org.jmouse.cad.spice.SpiceNetlist.Model;
import org.jmouse.cad.spice.SpiceNetlist.Parameter;
import org.jmouse.cad.spice.SpiceNetlist.Subcircuit;
import org.jmouse.el.grammar.AssignedNode;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ConnectionsNode;
import org.jmouse.el.grammar.FormulaNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.RangeNode;
import org.jmouse.el.grammar.SettingsNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.Simplification;
import org.jmouse.formula.Substitution;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the document a {@link SpiceNetlist} is — a {@code circuit} when the netlist has parts of its own, a
 * {@code component} when it is a {@code .subckt} alone — as its tree, which writes itself in either spelling.
 *
 * <h2>What each SPICE card becomes</h2>
 * <ul>
 *     <li>An element card — {@code R}, {@code C}, {@code L}, a constant {@code V} or {@code I}, a linear {@code E} or
 *     {@code G} — the component the table ({@link SpiceDevices}) gives its letter, its nodes on that component's pins in
 *     the table's order, its value on the setting the table names.</li>
 *     <li>{@code SIN(…)} the table's sine element, the phase in radians. {@code PULSE(…)} the constant element when it
 *     rises once from 0 and stays — a jMX circuit starts from rest, so every source is switched on at the start —, else
 *     the table's pulse element with the duty taken at half height.</li>
 *     <li>{@code D}, {@code Q}, {@code M}, {@code J} and their {@code .model} — the component the model's name names when
 *     the catalogue holds one of that device, else the device's carrier — the model's parameters set on it by the names
 *     the table gives them, wherever they differ from its own.</li>
 *     <li>{@code X} — the catalogue's component of the subcircuit's name when its pins are the subcircuit's pins, else
 *     the subcircuit written out as its parts ({@code X1_R1}).</li>
 *     <li>{@code .param} → {@code parameters}; {@code .tran} → {@code analysis transient}.</li>
 * </ul>
 *
 * <p>A value is a quantity in the unit the component writes that parameter in, when it is a number; else the jMF
 * expression. Nothing is guessed: whatever is carried across differently, or not at all, is a {@link SpiceNote}.</p>
 */
final class SpiceImporter {

    /** How long a circuit runs when the netlist has no {@code .tran}: jMX needs a span, SPICE's text gave none. */
    static final double DEFAULT_SPAN = 0.01;

    /** Edges at most this share of a period are ideal already; longer ones are made ideal, and that is said. */
    private static final double NEGLIGIBLE_EDGE = 1e-4;

    /** How far a circuit's parameter slides either way of its value: a tenth to ten times it. */
    private static final double SLIDE = 10;

    /** The duty a square may have, as its component offers it. */
    private static final double LOWEST_DUTY = 0.01;
    private static final double HIGHEST_DUTY = 0.99;

    /** What a renamed parameter, a node and a part that begin with a digit are given. */
    private static final String RENAMED_PARAMETER = "_parameter";
    private static final String NODE_PREFIX = "n";
    private static final String PART_PREFIX = "U";

    /** What joins a subcircuit written out to its parts: {@code X1_R1}. */
    private static final String EXPANDED = "_";

    /** The arguments of a SPICE signal, as the table names what each sets. */
    private static final String OFFSET = "offset";
    private static final String AMPLITUDE = "amplitude";
    private static final String FREQUENCY = "frequency";
    private static final String PHASE = "phase";
    private static final String PULSED = "pulsed";
    private static final String INITIAL = "initial";
    private static final String DUTY = "duty";

    /** The signals a source card writes, as the table names them. */
    private static final String SINE = "SIN";
    private static final String PULSE = "PULSE";

    /** The units of time a span is written in. */
    private static final String SECONDS = "s";

    /**
     * The document built, and what was not carried across.
     *
     * @param notes each said once, in the order met: a model ten parts use is noted at its own line, once
     */
    record Imported(DocumentNode document, List<SpiceNote> notes) {

        Imported {
            notes = List.copyOf(new LinkedHashSet<>(notes));
        }
    }

    private final CatalogueDocuments catalogue;
    private final SpiceDevices devices;
    private final List<SpiceNote> notes = new ArrayList<>();
    private final SpiceNames nodes = new SpiceNames(NODE_PREFIX);
    private final SpiceNames references = new SpiceNames(PART_PREFIX);
    private final Map<String, String> parameterNames = new LinkedHashMap<>();
    private final Map<String, Double> globalValues = new LinkedHashMap<>();
    private final Map<String, Optional<SimulatedComponent>> components = new HashMap<>();
    private SpiceNetlist netlist;
    private double span = DEFAULT_SPAN;
    /** The subcircuit being written as a component, while it is. */
    private Subcircuit converted;

    private SpiceImporter(CatalogueDocuments catalogue, SpiceDevices devices) {
        this.catalogue = catalogue;
        this.devices = devices;
    }

    /**
     * @param name the document's title; when the netlist holds subcircuits alone, also which of them to convert
     * @throws SpiceParseException when the netlist holds nothing to convert, or names no one subcircuit to
     */
    static Imported convert(SpiceNetlist netlist, String name, CatalogueDocuments catalogue, SpiceDevices devices) {
        return new SpiceImporter(catalogue, devices).write(netlist, name);
    }

    private Imported write(SpiceNetlist netlist, String name) {
        this.netlist = netlist;
        notes.addAll(netlist.notes());
        netlist.analysis().ifPresent(analysis -> span = analysis.stop());
        globalValues.putAll(SpiceAnalyzer.values(netlist.parameters()));
        if (!netlist.elements().isEmpty()) {
            return new Imported(circuit(name), notes);
        }
        if (!netlist.subcircuits().isEmpty()) {
            return new Imported(component(chosenSubcircuit(name), name), notes);
        }
        throw new SpiceParseException(SpicePosition.ofLine(1), "the text holds no parts and no '.subckt': there is "
                + "nothing to convert");
    }

    // ─── a circuit ──────────────────────────────────────────────────────────────────────────────────

    private DocumentNode circuit(String name) {
        String title = titleOf(name, netlist.title().orElse(""), "Imported circuit");
        DocumentNode document = new DocumentNode(SimulationModule.CIRCUIT, title, DocumentParser.slugOf(title), false, null, 1);
        document.addExpression(meta("Imported from a SPICE netlist"
                + netlist.title().filter(written -> !written.isBlank()).map(written -> ": " + written).orElse("")));
        addBlock(document, "parameters", parameterLines(netlist.parameters(), netlist.elements(), true));
        List<ClauseLineNode> parts = new ArrayList<>();
        for (Element element : netlist.elements()) {
            parts.addAll(parts(element, Placement.TOP, new LinkedHashSet<>()));
        }
        document.addExpression(block("parts", parts));
        document.addExpression(analysis());
        noteRenamings();
        return document;
    }

    private ClauseBlockNode analysis() {
        double stop = DEFAULT_SPAN;
        if (netlist.analysis().isEmpty()) {
            notes.add(new SpiceNote(0, "the netlist has no '.tran'; a jMX circuit runs its transient for a span, so it "
                    + "runs for " + Quantity.prefixed(DEFAULT_SPAN, SECONDS).written() + " — change it to what the circuit "
                    + "needs"));
        } else {
            SpiceNetlist.Transient written = netlist.analysis().get();
            stop = written.stop();
            if (written.start() != 0) {
                notes.add(new SpiceNote(written.position().line(), "'.tran' starts saving at "
                        + Quantity.prefixed(written.start(), SECONDS).written() + "; a jMX transient shows the whole run "
                        + "from 0"));
            }
        }
        return new ClauseBlockNode("analysis", LineBuilder.line(SimulationLines.ANALYSIS)
                .slot("kind", Nodes.word(SimulationVocabulary.TRANSIENT))
                .clause("for", new QuantityLiteralNode(Quantity.prefixed(stop, SECONDS))).build());
    }

    // ─── a component ────────────────────────────────────────────────────────────────────────────────

    /** The subcircuit to convert: the one {@code name} names, else the one no other uses. */
    private Subcircuit chosenSubcircuit(String name) {
        if (name != null && netlist.subcircuits().containsKey(name.strip().toLowerCase(Locale.ROOT))) {
            return netlist.subcircuits().get(name.strip().toLowerCase(Locale.ROOT));
        }
        Set<String> used = new HashSet<>();
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

    private DocumentNode component(Subcircuit subcircuit, String name) {
        converted = subcircuit;
        String title = titleOf(name, subcircuit.name(), subcircuit.name());
        DocumentNode document = new DocumentNode(SimulationModule.COMPONENT, title, DocumentParser.slugOf(title), false, null, 1);
        document.addExpression(meta("Imported from SPICE subcircuit " + subcircuit.name()));
        List<Parameter> declared = new ArrayList<>(netlist.parameters());
        declared.addAll(subcircuit.parameters());
        if (!netlist.parameters().isEmpty()) {
            notes.add(new SpiceNote(netlist.parameters().getFirst().position().line(), "the netlist's own parameters are "
                    + "the component's parameters too, since a component sees nothing outside itself"));
        }
        addBlock(document, "parameters", parameterLines(declared, subcircuit.elements(), false));
        List<String> pins = subcircuit.pins().stream().map(nodes::of).toList();
        List<ClauseLineNode> pinLines = new ArrayList<>();
        for (int index = 0; index < pins.size(); index++) {
            pinLines.add(LineBuilder.line(SimulationLines.PIN).slot("name", Nodes.name(pins.get(index)))
                    .slot("nature", Nodes.word(SimulationVocabulary.ANALOG))
                    .slot("numbers", Nodes.groups(List.of(List.of(String.valueOf(index + 1)))))
                    .ending(Nodes.text("pin " + (index + 1) + " of subcircuit " + subcircuit.name())).build());
        }
        addBlock(document, "pins", pinLines);
        List<ClauseLineNode> parts = new ArrayList<>();
        for (Element element : subcircuit.elements()) {
            parts.addAll(parts(element, Placement.TOP, new LinkedHashSet<>()));
        }
        document.addExpression(block("parts", parts));
        document.addExpression(symbolBox(pins));
        netlist.analysis().ifPresent(analysis -> notes.add(new SpiceNote(analysis.position().line(),
                "'.tran' belongs to a circuit; a component has no transient of its own; left out")));
        noteRenamings();
        return document;
    }

    /** {@code symbol box { left a, b  right c, d }}: the pins shared between the two sides, the first half on the left. */
    private static ClauseBlockNode symbolBox(List<String> pins) {
        ClauseBlockNode symbol = new ClauseBlockNode("symbol", LineBuilder.line(SimulationLines.SYMBOL).flag("box").build());
        symbol.open();
        int half = (pins.size() + 1) / 2;
        symbol.addExpression(side(SimulationVocabulary.LEFT, pins.subList(0, half)));
        if (half < pins.size()) {
            symbol.addExpression(side(SimulationVocabulary.RIGHT, pins.subList(half, pins.size())));
        }
        return symbol;
    }

    private static ClauseLineNode side(String side, List<String> pins) {
        LineGrammar grammar = SimulationLines.BOX_SIDES.stream().filter(line -> line.word().equals(side)).findFirst()
                .orElseThrow();
        return LineBuilder.line(grammar).slot("pins", Nodes.groups(pins.stream().map(List::of).toList())).build();
    }

    // ─── parameters ─────────────────────────────────────────────────────────────────────────────────

    /**
     * {@code R = 1 kOhm "from .param"}, the unit taken from the first part whose value the parameter is alone; a
     * circuit's with the range its slider spans.
     */
    private List<ClauseLineNode> parameterLines(List<Parameter> parameters, List<Element> elements, boolean sliders) {
        for (Parameter parameter : parameters) {
            String name = parameter.name();
            if (reserved(name)) {
                String renamed = name + RENAMED_PARAMETER;
                notes.add(new SpiceNote(parameter.position().line(), "parameter '" + name + "' is a word jMX gives a "
                        + "meaning of its own; it is called '" + renamed + "'"));
                parameterNames.put(name, renamed);
            }
        }
        List<ClauseLineNode> lines = new ArrayList<>();
        Set<String> written = new HashSet<>();
        boolean ranged = false;
        for (Parameter parameter : parameters) {
            String name = parameterNames.getOrDefault(parameter.name(), parameter.name());
            if (!written.add(name.toLowerCase(Locale.ROOT))) {
                notes.add(new SpiceNote(parameter.position().line(), "parameter '" + parameter.name() + "' is declared "
                        + "again; the first value is kept"));
                continue;
            }
            String unit = unitOf(parameter.name(), elements);
            LineBuilder line = LineBuilder.line(SimulationLines.PARAMETER).slot("name", Nodes.name(name))
                    .slot("value", new AssignedNode(value(parameter.value(), unit)));
            Optional<RangeNode> range = sliders ? range(parameter.value(), unit) : Optional.empty();
            if (range.isPresent()) {
                line.clause("in", range.get());
                ranged = true;
            }
            lines.add(line.ending(Nodes.text(originOf(parameter))).build());
        }
        if (ranged) {
            notes.add(new SpiceNote(parameters.getFirst().position().line(), "a circuit's parameter is a slider with a "
                    + "range, and SPICE gives none: each spans a tenth to ten times its value"));
        }
        return lines;
    }

    /** Whether jMX gives a name a meaning of its own — the engine's names, and jMF's constants —, which SPICE does not. */
    private static boolean reserved(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return ParameterEvaluation.reserved(lower) || MathematicalConstant.bySymbol(lower).isPresent();
    }

    /**
     * {@code in 100 Ohm .. 10 kOhm}: the range a circuit's slider needs, a tenth to ten times the value — none for a
     * parameter worked out from others, which is no slider.
     */
    private Optional<RangeNode> range(Formula value, String unit) {
        Formula folded = Simplification.simplify(renamed(value));
        if (!folded.variables().isEmpty()) {
            return Optional.empty();
        }
        double number = SpiceAnalyzer.evaluate(folded, Map.of());
        double low = number == 0 ? -1 : number > 0 ? number / SLIDE : number * SLIDE;
        double high = number == 0 ? 1 : number > 0 ? number * SLIDE : number / SLIDE;
        return Optional.of(RangeNode.between(quantity(low, unit), quantity(high, unit)));
    }

    /** Where a parameter was declared: in the subcircuit being converted, or on a {@code .param} card of the netlist. */
    private String originOf(Parameter parameter) {
        if (converted != null && converted.parameters().contains(parameter)) {
            return "a parameter of subcircuit " + converted.name() + ", line " + parameter.position().line();
        }
        return "from .param, line " + parameter.position().line();
    }

    /** The unit of the first element's value that is this parameter alone, as its component writes it; none when none is. */
    private String unitOf(String parameter, List<Element> elements) {
        Formula alone = new Formula.Variable(parameter);
        for (Element element : elements) {
            Formula value = switch (element) {
                case SpiceNetlist.Passive passive -> passive.value();
                case SpiceNetlist.Source source when source.signal() instanceof SpiceNetlist.Constant constant -> constant.value();
                default -> null;
            };
            if (alone.equals(value)) {
                Optional<ElementCard> mapped = devices.element(card(element), Optional.empty());
                if (mapped.isPresent() && mapped.get().value().isPresent()) {
                    return unit(mapped.get().component(), mapped.get().valueSetting());
                }
            }
        }
        return "";
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * The part lines one element becomes: one, none when it is left out, several for a subcircuit written out.
     *
     * @param expanding the subcircuits being written out around this one, to refuse a recursion
     */
    private List<ClauseLineNode> parts(Element element, Placement placement, Set<String> expanding) {
        return switch (element) {
            case SpiceNetlist.Passive passive -> constant(passive, card(passive),
                    List.of(passive.positive(), passive.negative()), passive.value(), placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Source source -> source(source, placement).map(List::of).orElse(List.of());
            case SpiceNetlist.LinearControlled controlled -> constant(controlled, card(controlled),
                    List.of(controlled.positive(), controlled.negative(), controlled.controlPositive(),
                            controlled.controlNegative()), controlled.gain(), placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Diode diode -> device(diode, diode.model(), List.of(diode.anode(), diode.cathode()),
                    Map.of(), placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Bipolar bipolar -> device(bipolar, bipolar.model(),
                    List.of(bipolar.collector(), bipolar.base(), bipolar.emitter()), Map.of(), placement)
                    .map(List::of).orElse(List.of());
            case SpiceNetlist.Mosfet mosfet -> mosfet(mosfet, placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Jfet jfet -> device(jfet, jfet.model(), List.of(jfet.drain(), jfet.gate(), jfet.source()),
                    Map.of(), placement).map(List::of).orElse(List.of());
            case SpiceNetlist.Instance instance -> instance(instance, placement, expanding);
        };
    }

    /** The card an element is: the first letter of its name, as SPICE reads it. */
    private static String card(Element element) {
        return element.name().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    /**
     * How an element inside a subcircuit written out is placed: its nodes, its reference and its parameters.
     *
     * @param pins   its subcircuit's pins → the nodes outside they are on
     * @param prefix what its reference and its inner nodes are prefixed with
     * @param bound  its subcircuit's parameters → their values at this instance
     */
    private record Placement(Map<String, String> pins, String prefix, Map<String, Formula> bound) {

        static final Placement TOP = new Placement(Map.of(), "", Map.of());

        String node(String local) {
            if (local.equals(Circuit.GROUND) || prefix.isEmpty()) {
                return local;
            }
            String outside = pins.get(local);
            return outside != null ? outside : prefix + local;
        }

        String reference(String written) {
            return prefix.isEmpty() ? written : prefix + written;
        }

        Formula value(Formula formula) {
            return bound.isEmpty() ? formula : Substitution.substitute(formula, bound);
        }
    }

    /**
     * An element whose one value sets one setting of its component — a resistor's resistance, a source's voltage, a
     * controlled source's gain —; left out with a note where the table or the catalogue has no such component.
     */
    private Optional<ClauseLineNode> constant(Element element, String card, List<String> spiceNodes, Formula value,
                                              Placement placement) {
        Optional<ElementCard> mapped = mapped(element, card, Optional.empty());
        if (mapped.isEmpty()) {
            return Optional.empty();
        }
        String setting = mapped.get().valueSetting();
        Map<String, Expression> settings = new LinkedHashMap<>();
        settings.put(setting, value(placement.value(value), unit(mapped.get().component(), setting)));
        return Optional.of(part(element, mapped.get(), spiceNodes, settings, placement));
    }

    /** The table's element of that card and signal, whose component the catalogue holds; else empty, and noted. */
    private Optional<ElementCard> mapped(Element element, String card, Optional<String> signal) {
        Optional<ElementCard> mapped = devices.element(card, signal);
        int line = element.position().line();
        if (mapped.isEmpty()) {
            notes.add(new SpiceNote(line, element.name() + ": a " + card + signal.map(written -> " " + written).orElse("")
                    + " card has no jMX component; left out"));
            return Optional.empty();
        }
        if (component(mapped.get().component()).isEmpty()) {
            notes.add(new SpiceNote(line, element.name() + " needs the component " + mapped.get().component()
                    + ", which the catalogue has not; left out"));
            return Optional.empty();
        }
        return mapped;
    }

    /** A part line: the element's reference, its component, its nodes on the component's pins, and its settings. */
    private ClauseLineNode part(Element element, ElementCard mapped, List<String> spiceNodes, Map<String, Expression> settings,
                                Placement placement) {
        Map<String, String> connections = new LinkedHashMap<>();
        for (int index = 0; index < mapped.pins().size(); index++) {
            connections.put(mapped.pins().get(index), nodes.of(placement.node(spiceNodes.get(index))));
        }
        return part(references.of(placement.reference(element.name())), mapped.component(), connections, settings);
    }

    private static ClauseLineNode part(String reference, String component, Map<String, String> connections,
                                       Map<String, Expression> settings) {
        List<ConnectionsNode.Connection> wired = new ArrayList<>();
        connections.forEach((pin, net) -> wired.add(new ConnectionsNode.Connection(pin, net)));
        LineBuilder line = LineBuilder.line(SimulationLines.PART).slot("reference", Nodes.name(reference))
                .slot("component", Nodes.word(component)).slot("connections", new ConnectionsNode(wired));
        if (!settings.isEmpty()) {
            List<SettingsNode.Setting> given = new ArrayList<>();
            settings.forEach((name, value) -> given.add(new SettingsNode.Setting(name, value)));
            line.slot("settings", new SettingsNode(given));
        }
        return line.build();
    }

    // ─── sources ────────────────────────────────────────────────────────────────────────────────────

    private Optional<ClauseLineNode> source(SpiceNetlist.Source source, Placement placement) {
        List<String> spiceNodes = List.of(source.positive(), source.negative());
        return switch (source.signal()) {
            case SpiceNetlist.Constant constant -> constant(source, card(source), spiceNodes, constant.value(), placement);
            case SpiceNetlist.Sine sine -> sine(source, sine, spiceNodes, placement);
            case SpiceNetlist.Pulse pulse -> pulse(source, pulse, spiceNodes, placement);
        };
    }

    private Optional<ClauseLineNode> sine(SpiceNetlist.Source source, SpiceNetlist.Sine sine, List<String> spiceNodes,
                                          Placement placement) {
        int line = source.position().line();
        Optional<ElementCard> mapped = mapped(source, card(source), Optional.of(SINE));
        if (mapped.isEmpty()) {
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
                    + "transient, " + Quantity.prefixed(1 / span, "Hz").written()));
        }
        if (!isZero(placement.value(sine.delay()))) {
            notes.add(new SpiceNote(line, source.name() + "'s delay has no counterpart in a " + mapped.get().component()
                    + ", which starts at time 0; left out"));
        }
        if (!isZero(placement.value(sine.damping()))) {
            notes.add(new SpiceNote(line, source.name() + "'s damping has no counterpart in a " + mapped.get().component()
                    + ", whose amplitude stays; left out"));
        }
        Map<String, Formula> arguments = new LinkedHashMap<>();
        arguments.put(AMPLITUDE, sine.amplitude());
        arguments.put(FREQUENCY, frequency);
        if (!isZero(placement.value(sine.offset()))) {
            arguments.put(OFFSET, sine.offset());
        }
        if (!isZero(placement.value(sine.phase()))) {
            // SPICE writes the phase in degrees.
            arguments.put(PHASE, new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Binary(Formula.Operator.MULTIPLY,
                    sine.phase(), new Formula.NamedConstant(MathematicalConstant.PI)), new Formula.Constant(180)));
        }
        return Optional.of(part(source, mapped.get(), spiceNodes, signalSettings(mapped.get(), arguments, placement), placement));
    }

    /** What each argument of a signal sets on the element's component, in its unit. */
    private Map<String, Expression> signalSettings(ElementCard mapped, Map<String, Formula> arguments, Placement placement) {
        Map<String, Expression> settings = new LinkedHashMap<>();
        arguments.forEach((argument, formula) -> {
            String setting = mapped.setting(argument);
            settings.put(setting, value(placement.value(formula), unit(mapped.component(), setting)));
        });
        return settings;
    }

    /**
     * A step when the pulse rises once and stays for the whole run; else a square, its duty measured at half height —
     * {@code (width + (rise + fall) / 2) / period}, which keeps the square's average the pulse's.
     */
    private Optional<ClauseLineNode> pulse(SpiceNetlist.Source source, SpiceNetlist.Pulse pulse, List<String> spiceNodes,
                                           Placement placement) {
        int line = source.position().line();
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
                notes.add(new SpiceNote(line, source.name() + "'s rise of " + Quantity.prefixed(rise, SECONDS).written()
                        + " is made an ideal step"));
            }
            return constant(source, card(source), spiceNodes, pulse.pulsed(), placement);
        }
        Optional<ElementCard> mapped = mapped(source, card(source), Optional.of(PULSE));
        if (mapped.isEmpty()) {
            return Optional.empty();
        }
        if (delay != 0) {
            notes.add(new SpiceNote(line, source.name() + "'s delay of " + Quantity.prefixed(delay, SECONDS).written()
                    + " has no counterpart in a " + mapped.get().component() + ", which starts high at time 0; left out"));
        }
        if (rise > period * NEGLIGIBLE_EDGE || fall > period * NEGLIGIBLE_EDGE) {
            notes.add(new SpiceNote(line, source.name() + "'s rise and fall are made ideal edges; the duty is taken at "
                    + "half height, so the average stays"));
        }
        Formula halfHeightWidth = new Formula.Binary(Formula.Operator.ADD, pulse.width(),
                new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Binary(Formula.Operator.ADD, pulse.rise(),
                        pulse.fall()), new Formula.Constant(2)));
        double duty = (width + (rise + fall) / 2) / period;
        if (duty < LOWEST_DUTY || duty > HIGHEST_DUTY) {
            notes.add(new SpiceNote(line, source.name() + "'s duty " + Quantity.prefixed(duty, "").written() + " is "
                    + "outside a " + mapped.get().component() + "'s " + LOWEST_DUTY + " … " + HIGHEST_DUTY));
        }
        Map<String, Formula> arguments = new LinkedHashMap<>();
        arguments.put(PULSED, pulse.pulsed());
        if (initial != 0) {
            arguments.put(INITIAL, pulse.initial());
        }
        arguments.put(FREQUENCY, new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Constant(1), pulse.period()));
        arguments.put(DUTY, new Formula.Binary(Formula.Operator.DIVIDE, halfHeightWidth, pulse.period()));
        return Optional.of(part(source, mapped.get(), spiceNodes, signalSettings(mapped.get(), arguments, placement), placement));
    }

    // ─── devices and their models ───────────────────────────────────────────────────────────────────

    /**
     * A MOSFET: a jMX MOSFET's body is its source, and the line's width and length are folded into its model as the
     * table's geometry says.
     */
    private Optional<ClauseLineNode> mosfet(SpiceNetlist.Mosfet mosfet, Placement placement) {
        if (!mosfet.bulk().equals(mosfet.source())) {
            notes.add(new SpiceNote(mosfet.position().line(), mosfet.name() + "'s bulk is on " + mosfet.bulk()
                    + ", not its source; a jMX MOSFET's body is its source, so that connection is left out"));
        }
        Map<String, Formula> scale = new LinkedHashMap<>();
        Model model = netlist.models().get(mosfet.model().toLowerCase(Locale.ROOT));
        if (model != null && devices.takes(card(mosfet), model.type())) {
            devices.device(model.type()).geometry().ifPresent(geometry -> {
                Formula fallback = new Formula.Constant(geometry.fallback());
                Formula width = mosfet.width() == null ? fallback : mosfet.width();
                Formula length = mosfet.length() == null ? fallback : mosfet.length();
                Formula ratio = new Formula.Binary(Formula.Operator.DIVIDE, width, length);
                geometry.byRatio().forEach(parameter -> scale.put(parameter, ratio));
                geometry.byWidth().forEach(parameter -> scale.put(parameter, width));
            });
        }
        return device(mosfet, mosfet.model(), List.of(mosfet.drain(), mosfet.gate(), mosfet.source()), scale, placement);
    }

    /**
     * A diode or a transistor: the component its model names, or the device's carrier — with the model's parameters on
     * it where they differ from the component's own.
     *
     * @param spiceNodes its nodes, in the order SPICE writes them — the table's order of the device's pins
     * @param scale      some of the model's parameters scaled for this one element: a jMX parameter and what it is
     *                   multiplied by
     */
    private Optional<ClauseLineNode> device(Element element, String modelName, List<String> spiceNodes,
                                            Map<String, Formula> scale, Placement placement) {
        int line = element.position().line();
        Model model = netlist.models().get(modelName.toLowerCase(Locale.ROOT));
        if (model == null) {
            notes.add(new SpiceNote(line, element.name() + "'s model '" + modelName + "' is not in the netlist; left out"));
            return Optional.empty();
        }
        if (!devices.takes(card(element), model.type())) {
            notes.add(new SpiceNote(line, element.name() + "'s model '" + modelName + "' is a " + model.type()
                    + " model, not one for this element; left out"));
            return Optional.empty();
        }
        SpiceDevice device = devices.device(model.type());
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
        Optional<Carried> carried = componentFor(model, device);
        if (carried.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> connections = new LinkedHashMap<>();
        for (int index = 0; index < device.pins().size(); index++) {
            connections.put(device.pins().get(index), nodes.of(placement.node(spiceNodes.get(index))));
        }
        Map<String, Expression> overrides = overrides(model, device, given, carried.get(), scale, placement);
        return Optional.of(part(references.of(placement.reference(element.name())), carried.get().slug(), connections,
                overrides));
    }

    /** A model parameter in jMX's units: a temperature in kelvin where SPICE writes °C, the rest as written. */
    private static Formula inJmxUnits(SpiceDevice.ParameterName parameter, Formula formula) {
        if (!parameter.temperature()) {
            return formula;
        }
        if (formula instanceof Formula.Constant constant) {
            return new Formula.Constant(parameter.toJmx(constant.value()));
        }
        return new Formula.Binary(Formula.Operator.ADD, formula, new Formula.Constant(parameter.toJmx(0)));
    }

    /** A component a model becomes, compiled. */
    private record Carried(String slug, SimulatedComponent component) {
    }

    /** The catalogue's component the model's name names, when it is that device; else the device's carrier. */
    private Optional<Carried> componentFor(Model model, SpiceDevice device) {
        for (String candidate : candidates(model.name())) {
            Optional<Carried> named = carrier(candidate, device);
            if (named.isPresent()) {
                return named;
            }
        }
        Optional<Carried> carried = carrier(device.carrier(), device);
        if (carried.isEmpty()) {
            notes.add(new SpiceNote(model.position().line(), "the " + device.modelType() + " carrier '" + device.carrier()
                    + "' is not a " + device.componentType() + " the catalogue holds; model " + model.name()
                    + " is left out"));
            return Optional.empty();
        }
        notes.add(new SpiceNote(model.position().line(), "model " + model.name() + " names no component of the catalogue; "
                + "its parts are " + device.carrier() + " parts with the model's parameters"));
        return carried;
    }

    /** The slugs a SPICE name may stand for: as written in lower case, and with {@code -} for {@code _}. */
    private static List<String> candidates(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("_") ? List.of(lower, lower.replace('_', '-')) : List.of(lower);
    }

    /** The component of that slug, when it is the device and has its pins. */
    private Optional<Carried> carrier(String slug, SpiceDevice device) {
        return component(slug).filter(component -> devices.deviceOf(component).filter(device::equals).isPresent())
                .filter(component -> device.pins().stream().allMatch(pin -> component.pin(pin).isPresent()))
                .map(component -> new Carried(slug, component));
    }

    /**
     * The model's parameters as the part's settings: each one written, SPICE's default for each one not written that
     * jMX can say, and only those that differ from the component's own values.
     */
    private Map<String, Expression> overrides(Model model, SpiceDevice device, Map<String, Formula> given, Carried carried,
                                              Map<String, Formula> scale, Placement placement) {
        Map<String, Expression> overrides = new LinkedHashMap<>();
        List<String> kept = new ArrayList<>();
        for (SpiceDevice.ParameterName parameter : device.parameters()) {
            Optional<EvaluatedParameter> own = carried.component().parameter(parameter.jmx());
            if (own.isEmpty()) {
                if (given.containsKey(parameter.jmx())) {
                    notes.add(new SpiceNote(model.position().line(), "model " + model.name() + "'s " + parameter.spice()
                            + " has no counterpart in " + carried.slug() + "; left out"));
                }
                continue;
            }
            Formula value = given.get(parameter.jmx());
            // SPICE writes "none" as 0 for exactly the parameters jMX cannot say it for: VAF=0 and IKR=0 are infinite,
            // RS=0 and RB=0 drop a node. Such a 0 keeps the component's own value, as an unwritten one does.
            if (parameter.spiceDefault().isEmpty() && isZero(value)) {
                value = null;
            }
            if (value == null) {
                if (parameter.spiceDefault().isEmpty()) {
                    kept.add(parameter.spice());
                    continue;
                }
                value = new Formula.Constant(parameter.toJmx(parameter.spiceDefault().get()));
            }
            if (scale.containsKey(parameter.jmx())) {
                value = new Formula.Binary(Formula.Operator.MULTIPLY, value, scale.get(parameter.jmx()));
            }
            Formula folded = Simplification.simplify(placement.value(value));
            if (folded.variables().isEmpty()) {
                double number = SpiceAnalyzer.evaluate(folded, Map.of());
                double ownValue = own.get().value();
                if (Math.abs(number - ownValue) <= 1e-9 * Math.max(Math.abs(ownValue), Math.abs(number))) {
                    continue;
                }
            }
            overrides.put(parameter.jmx(), value(folded, own.get().unit()));
        }
        if (!kept.isEmpty()) {
            notes.add(new SpiceNote(model.position().line(), "model " + model.name() + " does not write " + String.join(", ", kept)
                    + ", and SPICE's default for " + (kept.size() == 1 ? "it" : "them") + " — no resistance, no "
                    + "breakdown, no limit — is not a value jMX can write; " + carried.slug() + "'s own "
                    + (kept.size() == 1 ? "value is" : "values are") + " kept"));
        }
        return overrides;
    }

    // ─── subcircuit instances ───────────────────────────────────────────────────────────────────────

    private List<ClauseLineNode> instance(SpiceNetlist.Instance instance, Placement placement, Set<String> expanding) {
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
        Optional<ClauseLineNode> asComponent = catalogueInstance(instance, subcircuit, placement);
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
        String prefix = placement.reference(instance.name()) + EXPANDED;
        Placement inside = new Placement(pins, prefix, boundParameters(instance, subcircuit, placement));
        List<ClauseLineNode> lines = new ArrayList<>();
        for (Element inner : subcircuit.elements()) {
            lines.addAll(parts(inner, inside, expanding));
        }
        expanding.remove(key);
        notes.add(new SpiceNote(line, instance.name() + ": subcircuit " + subcircuit.name() + " is written out as its "
                + "parts (" + prefix + "…); convert the .subckt alone to keep it as a component"));
        return lines;
    }

    /** The component of the subcircuit's name, when the catalogue holds one with exactly the subcircuit's pins. */
    private Optional<ClauseLineNode> catalogueInstance(SpiceNetlist.Instance instance, Subcircuit subcircuit,
                                                       Placement placement) {
        List<String> spicePins = subcircuit.pins().stream().map(pin -> pin.toLowerCase(Locale.ROOT)).toList();
        for (String candidate : candidates(subcircuit.name())) {
            Optional<SimulatedComponent> found = component(candidate);
            if (found.isEmpty()) {
                continue;
            }
            SimulatedComponent component = found.get();
            List<String> pins = component.pins().stream().map(pin -> pin.name()).toList();
            if (!pins.stream().map(pin -> pin.toLowerCase(Locale.ROOT)).toList().equals(spicePins)) {
                continue;
            }
            Map<String, String> connections = new LinkedHashMap<>();
            for (int index = 0; index < pins.size(); index++) {
                connections.put(pins.get(index), nodes.of(placement.node(instance.nodes().get(index))));
            }
            Map<String, Expression> overrides = new LinkedHashMap<>();
            instance.parameters().forEach((name, formula) -> {
                Optional<EvaluatedParameter> declared = component.parameters().stream()
                        .filter(parameter -> parameter.name().equalsIgnoreCase(name)).findFirst();
                if (declared.isEmpty()) {
                    notes.add(new SpiceNote(instance.position().line(), instance.name() + "'s " + name + " is not a "
                            + "parameter of " + candidate + "; left out"));
                } else {
                    overrides.put(declared.get().name(), value(placement.value(formula), declared.get().unit()));
                }
            });
            return Optional.of(part(references.of(placement.reference(instance.name())), candidate, connections, overrides));
        }
        return Optional.empty();
    }

    /** The subcircuit's parameters at this instance: the instance's values, else the defaults, each in order. */
    private Map<String, Formula> boundParameters(SpiceNetlist.Instance instance, Subcircuit subcircuit, Placement placement) {
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

    // ─── the catalogue ──────────────────────────────────────────────────────────────────────────────

    /** The catalogue's component of that slug, compiled; empty when it holds none, or none it can read. */
    private Optional<SimulatedComponent> component(String slug) {
        return components.computeIfAbsent(slug, key -> catalogue.find(key)
                .filter(document -> document.kind().equals(SimulationModule.COMPONENT))
                .flatMap(document -> {
                    try {
                        return Optional.of(ComponentCompilation.compile(SimulationReading.component(document), catalogue));
                    } catch (RuntimeException unreadable) {
                        return Optional.empty();
                    }
                }));
    }

    /** The unit a component writes a parameter in; none for a parameter it has not, or writes as a bare number. */
    private String unit(String slug, String parameter) {
        return component(slug).flatMap(component -> component.parameter(parameter)).map(EvaluatedParameter::unit).orElse("");
    }

    // ─── values ─────────────────────────────────────────────────────────────────────────────────────

    /** A value as jMX writes it: a quantity with its unit when it is a number, else the jMF expression. */
    private Expression value(Formula formula, String unit) {
        Formula folded = Simplification.simplify(renamed(formula));
        if (folded.variables().isEmpty()) {
            return quantity(SpiceAnalyzer.evaluate(folded, Map.of()), unit);
        }
        return new FormulaNode(FormulaPrinter.text(folded));
    }

    private static FormulaNode quantity(double value, String unit) {
        return Nodes.formula(new QuantityLiteralNode(Quantity.prefixed(value, unit)));
    }

    private Formula renamed(Formula formula) {
        if (parameterNames.isEmpty()) {
            return formula;
        }
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

    // ─── the document ───────────────────────────────────────────────────────────────────────────────

    private static MetaNode meta(String summary) {
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(summary)));
        return meta;
    }

    /** A block of lines in braces, written even when empty. */
    private static ClauseBlockNode block(String word, List<ClauseLineNode> lines) {
        ClauseBlockNode block = new ClauseBlockNode(word, null);
        block.open();
        lines.forEach(block::addExpression);
        return block;
    }

    /** A block of lines, left out when it has none. */
    private static void addBlock(DocumentNode document, String word, List<ClauseLineNode> lines) {
        if (!lines.isEmpty()) {
            document.addExpression(block(word, lines));
        }
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
        if (!references.renamed().isEmpty()) {
            notes.add(new SpiceNote(0, "parts renamed to be jMX names: " + renamings(references.renamed())));
        }
    }

    private static String renamings(Map<String, String> renamed) {
        return String.join(", ", renamed.entrySet().stream().map(entry -> entry.getKey() + " → " + entry.getValue()).toList());
    }
}
