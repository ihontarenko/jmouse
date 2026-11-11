package org.jmouse.cad.spice;

import org.jmouse.cad.Parameter;
import org.jmouse.cad.Setting;
import org.jmouse.cad.Text;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Equation;
import org.jmouse.cad.component.InternalNode;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.State;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.EngineName;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.ModelLanguage;
import org.jmouse.cad.simulation.PinQuantities;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.engine.CircuitTopology;
import org.jmouse.el.node.Trivia;
import org.jmouse.formula.Formula;
import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.Simplification;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.spice.SpiceNumbers;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.expression.SpiceExpressionLanguage;
import org.jmouse.spice.expression.UntranslatableFormula;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceElement;
import org.jmouse.spice.node.SpiceParameter;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.spice.node.SpiceWaveform;
import org.jmouse.units.Quantity;
import org.jmouse.units.Temperatures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Builds the SPICE deck a {@code circuit} is, or the {@code .model} or {@code .subckt} SPICE describes a
 * {@code component} with, as jmouse-spice nodes, which write themselves in ngspice's dialect.
 *
 * <h2>What each part becomes</h2>
 * <ul>
 *     <li>A part whose component is an element card of the table ({@link SpiceDevices}) — or a member of one that
 *     changes none of its equations — that card: {@code R}, {@code C}, {@code L}, {@code E}, {@code G}. A constant
 *     source is a {@code PULSE} that rises from 0 at time 0 in a thousand-millionth of the run and stays, with its value
 *     as {@code DC}: a jMX circuit starts from rest. A sine is {@code SIN(…)}, a square {@code PULSE(…)} — its edges a
 *     millionth of the period, its width at half height.</li>
 *     <li>A component that is a device of the table → {@code D}, {@code Q}, {@code M}, {@code J} and a {@code .model} of
 *     the parameters SPICE names; its jMX equations are not carried — SPICE solves its own device model with them.</li>
 *     <li>Any other generator — one voltage between its two pins, of the time and its parameters — a {@code B} source
 *     with its equation.</li>
 *     <li>Any other component → a {@code .subckt} of its parts and of its equations as {@code B} sources, and an
 *     {@code X} instance — when every equation is one SPICE can say: an expression of pin voltages, parameters and the
 *     time. A discrete state, behaviour, or a current into a pin is not; such a part is noted and left out, never
 *     written in part.</li>
 * </ul>
 *
 * <p>The deck runs on an ideal bench: {@code realistic} is 0, and what an instrument draws on an ideal bench is what
 * it draws in SPICE. The layout, the probes and the expectations have no SPICE counterpart and are named in the
 * notes.</p>
 */
final class SpiceExporter {

    /** A step rises in this share of the run: SPICE has no ideal edge, so it gets one far shorter than anything else. */
    private static final double STEP_EDGE = 1e-9;
    /** A square's edges take this share of its period. */
    private static final double SQUARE_EDGE = 1e-6;
    /** SPICE's printing step is this share of the run; jMX has none, the engine choosing its own steps. */
    private static final double PRINTING_STEP = 1e-3;
    /** How many samples a run is printed in: one a printing step. */
    static final int PRINTED_SAMPLES = (int) Math.round(1 / PRINTING_STEP);
    /** How long a circuit that writes no analysis runs. */
    private static final double DEFAULT_SPAN = SpiceImporter.DEFAULT_SPAN;
    /** A pulse that stays: its width and period past the end of any run. */
    private static final double FOREVER = 1e30;
    /** What a state's or a charge's node is held by: one farad, so its current is the rate of what it holds. */
    private static final double HOLDING_FARAD = 1;

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

    /** What ngspice calls the time and the temperature, °C, inside a B source. */
    private static final String SPICE_TIME = "time";
    private static final String SPICE_TEMPERATURE = "temper";

    /** Every formula the deck carries is written by the SPICE dialect, walking its tree. */
    private static final SpiceExpressionLanguage EXPRESSIONS = SpiceExpressionLanguage.standard();

    /** What jMF reads a value a document writes as, and the language a part's equations are read in. */
    private static final FormulaLanguage VALUES = FormulaLanguage.standard();

    /**
     * What {@code anode.current} is read as: a name of its own. A B source cannot read the current into a pin, so the
     * name only has to survive until an ideal bench folds it away or {@link #behaviouralName} refuses it.
     */
    private static final String PIN_CURRENT = "current_into_";

    private final CatalogueDocuments catalogue;
    private final SpiceDevices devices;
    /** Parameters at other values than the document's — where the bench's sliders stand. */
    private final Map<String, Double> overrides;
    private final List<SpiceNote> notes = new ArrayList<>();
    /** Model and subcircuit cards, by name, in the order first needed: each written once. */
    private final Map<String, SpiceCard> definitions = new LinkedHashMap<>();
    /** What was already reported on, so a component used by ten parts is noted once. */
    private final Set<String> reported = new HashSet<>();
    private final Map<String, String> renamedElements = new LinkedHashMap<>();
    /** Every part written as an element, by its reference: the name SPICE knows it by. */
    private final Map<String, String> elementNames = new LinkedHashMap<>();
    /** The parts the deck could not carry: a deck without them is another circuit. */
    private final Set<String> leftOut = new LinkedHashSet<>();
    /** Subcircuits that cannot be written, each noted once; and those being written, to refuse one inside itself. */
    private final Set<String> refusedSubcircuits = new HashSet<>();
    private final Set<String> writingSubcircuits = new HashSet<>();
    private final Map<String, Optional<SimulatedComponent>> components = new HashMap<>();
    private double span = DEFAULT_SPAN;

    SpiceExporter(CatalogueDocuments catalogue, SpiceDevices devices, Map<String, Double> overrides) {
        this.catalogue = catalogue;
        this.devices = devices;
        this.overrides = Map.copyOf(overrides);
    }

    /**
     * The deck built, and what was not carried across.
     *
     * @param elementNames the name each part written as an element has in SPICE, by its reference ({@code 1} → {@code R1})
     * @param leftOut      the parts the deck does not carry: SPICE's answer is then another circuit's
     */
    record Written(SpiceDeck deck, List<SpiceNote> notes, Map<String, String> elementNames, Set<String> leftOut) {

        Written {
            notes = List.copyOf(new LinkedHashSet<>(notes));
            elementNames = Map.copyOf(elementNames);
            leftOut = Set.copyOf(leftOut);
        }

        String text() {
            return deck.toSource();
        }
    }

    Written write(DocumentNode document) {
        SpiceDeck deck;
        if (document.kind().equals(SimulationModule.CIRCUIT)) {
            deck = circuit(SimulationReading.circuit(document), document);
        } else if (document.kind().equals(SimulationModule.COMPONENT)) {
            deck = component(document);
        } else {
            throw new SpiceParseException(SpicePosition.ofLine(document.line()), "SPICE describes circuits and components; "
                    + "this is a " + document.kind().word());
        }
        return new Written(deck, notes, elementNames, leftOut);
    }

    // ─── a circuit ──────────────────────────────────────────────────────────────────────────────────

    private SpiceDeck circuit(Circuit circuit, DocumentNode document) {
        circuit.run().ifPresent(analysis -> span = analysis.duration());
        List<SpiceStatement> statements = new ArrayList<>();
        Set<String> parameterNames = new LinkedHashSet<>();
        Map<String, Double> parameterValues = new LinkedHashMap<>();
        boolean ranged = false;
        for (Parameter parameter : circuit.parameters()) {
            if (!(parameter.value() instanceof org.jmouse.cad.Formula written)) {
                notes.add(new SpiceNote(0, "parameter " + parameter.name() + " is a text, which SPICE has no place for; "
                        + "left out"));
                continue;
            }
            Formula value = formula(written);
            parameterNames.add(parameter.name());
            double number = overrides.containsKey(parameter.name()) ? overrides.get(parameter.name())
                    : SpiceAnalyzer.evaluate(value, parameterValues);
            parameterValues.put(parameter.name(), number);
            SpiceValue spiceValue = overrides.containsKey(parameter.name()) ? SpiceValue.number(number)
                    : value(value, name -> name);
            SpiceCard.Parameters card = new SpiceCard.Parameters(List.of(SpiceParameter.of(parameter.name(), spiceValue)),
                    SpicePosition.NONE);
            if (!parameter.description().isBlank()) {
                card.addLeadingTrivia(List.of(Trivia.comment(parameter.name() + ": " + parameter.description())));
            }
            statements.add(card);
            ranged |= parameter.offered().isPresent();
        }
        if (ranged) {
            notes.add(new SpiceNote(0, "the parameters' ranges are left out: SPICE keeps values, not sliders"));
        }
        Scope scope = new Scope(parameterNames, parameterValues);
        for (Part part : circuit.parts()) {
            statements.addAll(part(part, scope));
        }
        statements.addAll(definitions.values());
        statements.add(new SpiceCard.Transient(SpiceValue.number(span * PRINTING_STEP), SpiceValue.number(span), null, null,
                false, SpicePosition.NONE));
        statements.add(new SpiceCard.End(SpicePosition.NONE));
        noteLeftOutBlocks(circuit);
        if (!renamedElements.isEmpty()) {
            notes.add(new SpiceNote(0, "a SPICE element's name begins with its letter: " + String.join(", ",
                    renamedElements.entrySet().stream().map(entry -> entry.getKey() + " is " + entry.getValue()).toList())));
        }
        return SpiceDeck.of(document.title(), statements);
    }

    private void noteLeftOutBlocks(Circuit circuit) {
        if (!circuit.layout().isEmpty()) {
            notes.add(new SpiceNote(0, "the layout has no SPICE counterpart: a netlist is not drawn; left out"));
        }
        if (!circuit.probes().isEmpty()) {
            notes.add(new SpiceNote(0, "the probes have no SPICE counterpart; left out — print the nodes they read"));
        }
        if (!circuit.expectations().isEmpty()) {
            notes.add(new SpiceNote(0, circuit.expectations().size() + " expectation(s) have no SPICE counterpart; left out"));
        }
    }

    // ─── names ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * The parameters a value may name where a part is written — a circuit's, or a subcircuit's own — and what each is
     * worth, for what SPICE needs as a number: the edges of a square are a share of its period.
     */
    private record Scope(Set<String> parameters, Map<String, Double> values) {

        /** A formula's value at these parameters; NaN when it reads something else. */
        double numberOf(Formula formula) {
            try {
                return SpiceAnalyzer.evaluate(formula, values);
            } catch (RuntimeException unknown) {
                return Double.NaN;
            }
        }
    }

    /** A SPICE element name: the part's reference, its letter put in front when it does not begin with it. */
    private String elementName(char letter, String reference) {
        if (Character.toUpperCase(reference.charAt(0)) == letter) {
            elementNames.put(reference, reference);
            return reference;
        }
        String name = letter + reference;
        renamedElements.put(reference, name);
        elementNames.put(reference, name);
        return name;
    }

    /** A component's slug as a SPICE name: ngspice reads a {@code -} in a name as a minus. */
    static String spiceName(String slug) {
        return slug.replace('-', '_').replace("@", "_revision_");
    }

    /** A pin or a unit's parameter as SPICE can name it: a dot is no part of a SPICE name, so {@code A.output} is {@code A_output}. */
    static String spiceNode(String name) {
        return name.replace('.', '_');
    }

    /**
     * A node of a subcircuit as ngspice reads it as that node: {@code gnd}, in any case, is ground to ngspice, and a
     * component's pin of that name (an op-amp's negative supply) is no ground — it is renamed, as {@code 0} would be.
     */
    static String subcircuitNode(String name) {
        String node = spiceNode(name);
        return node.equalsIgnoreCase("gnd") || node.equals(Circuit.GROUND) ? node + "_pin" : node;
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    /** The statements one part becomes; none, with a note, when it is left out. */
    private List<SpiceStatement> part(Part part, Scope scope) {
        List<SpiceStatement> written;
        try {
            written = translatedPart(part, scope);
        } catch (UntranslatableFormula untranslatable) {
            notes.add(new SpiceNote(0, part.reference() + ": " + untranslatable.getMessage() + "; left out"));
            written = List.of();
        }
        if (written.isEmpty()) {
            leftOut.add(part.reference());
        }
        return written;
    }

    private List<SpiceStatement> translatedPart(Part part, Scope scope) {
        Optional<SimulatedComponent> found = component(part.component());
        if (found.isEmpty()) {
            notes.add(new SpiceNote(0, part.reference() + ": component '" + part.component() + "' is not in the catalogue, "
                    + "or does not read; left out"));
            return List.of();
        }
        SimulatedComponent component = found.get();
        Map<String, Formula> settings = new LinkedHashMap<>();
        for (Setting setting : part.settings()) {
            // A text — a display's first line, a chip's start-up bytes — is read by the part's behaviour, never by its
            // equations; SPICE has no place for it.
            if (setting.value() instanceof Text) {
                notes.add(new SpiceNote(0, part.reference() + "'s " + setting.name() + " is a text, which SPICE has no "
                        + "place for; left out"));
                continue;
            }
            if (setting.value() instanceof org.jmouse.cad.Formula written) {
                settings.put(spiceNode(setting.name()), formula(written));
            }
        }
        List<String> packagePins = packagePins(component);
        Map<String, String> pins = new LinkedHashMap<>();
        List<Connection> connections = part.connections();
        for (int index = 0; index < connections.size(); index++) {
            Connection connection = connections.get(index);
            if (connection.named().isEmpty()) {
                if (index < packagePins.size()) {
                    pins.put(packagePins.get(index), connection.net());
                }
                continue;
            }
            pins.put(connection.pin(), connection.net());
        }
        return parts(part, component, packagePins, pins, settings, scope);
    }

    /** A value a document writes as a jMF tree: a quantity in SI, or the expression. */
    private static Formula formula(org.jmouse.cad.Formula written) {
        Optional<Quantity> quantity = Quantity.parse(written.written());
        if (quantity.isPresent()) {
            return new Formula.Constant(quantity.get().value());
        }
        try {
            return VALUES.parse(written.written());
        } catch (RuntimeException unreadable) {
            throw new SpiceParseException(SpicePosition.NONE, "'" + written.written() + "' does not read as jMF: "
                    + unreadable.getMessage());
        }
    }

    private List<SpiceStatement> parts(Part part, SimulatedComponent component, List<String> packagePins,
                                       Map<String, String> pins, Map<String, Formula> settings, Scope scope) {
        List<SpiceStatement> leaks = openPins(part, packagePins, pins);
        List<SpiceStatement> written = new ArrayList<>(statements(part, component, packagePins, pins, settings, scope));
        if (!written.isEmpty()) {
            written.addAll(0, leaks);
        }
        return written;
    }

    /**
     * What the engine gives a pin a part connects to nothing — a private node, and its leak to ground (gmin) —, written
     * as SPICE needs it: every node of an element named, and each with a way to ground. {@code pins} gains the private
     * nodes.
     */
    private List<SpiceStatement> openPins(Part part, List<String> packagePins, Map<String, String> pins) {
        List<String> open = packagePins.stream().filter(pin -> !pins.containsKey(pin)).toList();
        List<SpiceStatement> leaks = new ArrayList<>();
        for (String pin : open) {
            String node = spiceNode(part.reference() + "." + pin);
            pins.put(pin, node);
            leaks.add(new SpiceElement.Passive("R" + node + "_leak", node, Circuit.GROUND,
                    SpiceValue.number(CircuitTopology.NODE_LEAK_RESISTANCE), List.of(), SpicePosition.NONE));
        }
        if (!open.isEmpty()) {
            notes.add(new SpiceNote(0, part.reference() + ": " + String.join(", ", open) + " connect to nothing — each "
                    + "is a node of its own with the engine's leak to ground, "
                    + SpiceNumbers.write(CircuitTopology.NODE_LEAK_RESISTANCE) + " Ω, as the engine solves it"));
        }
        return leaks;
    }

    private List<SpiceStatement> statements(Part part, SimulatedComponent component, List<String> packagePins,
                                            Map<String, String> pins, Map<String, Formula> settings, Scope scope) {
        Map<String, Formula> values = parameterValues(component, settings);
        Optional<ElementCard> card = elementCard(component);
        if (card.isPresent()) {
            return element(part, card.get(), pins, settings, values, scope);
        }
        Optional<SpiceDevice> device = devices.deviceOf(component);
        if (device.isPresent() && pins.keySet().containsAll(device.get().pins())) {
            return List.of(device(part, device.get(), component, pins, settings, values, scope));
        }
        Optional<List<SpiceStatement>> generator = generator(part.reference(), component, pins, values, scope);
        if (generator.isPresent()) {
            return generator.get();
        }
        Optional<String> subcircuit = subcircuit(component);
        if (subcircuit.isEmpty()) {
            return List.of();
        }
        List<SpiceParameter> instanceParameters = new ArrayList<>();
        settings.forEach((name, formula) -> instanceParameters.add(SpiceParameter.of(name, value(formula, scope))));
        return List.of(new SpiceElement.Instance(elementName('X', part.reference()), packagePins.stream().map(pins::get).toList(),
                subcircuit.get(), instanceParameters, SpicePosition.NONE));
    }

    /**
     * The element card a component is: the table's card of a component in its lineage, when nothing after it writes
     * equations of its own — a datasheet's resistor is a resistor still; a thermistor, whose physics is its own, is not.
     */
    private Optional<ElementCard> elementCard(SimulatedComponent component) {
        List<Component> lineage = component.lineage();
        for (int index = lineage.size() - 1; index >= 0; index--) {
            String slug = lineage.get(index).head().slug();
            Optional<ElementCard> card = devices.elements().stream().filter(element -> element.component().equals(slug))
                    .findFirst();
            if (card.isPresent()) {
                boolean ownPhysics = lineage.subList(index + 1, lineage.size()).stream().anyMatch(SpiceExporter::behaves);
                return ownPhysics ? Optional.empty() : card;
            }
        }
        return Optional.empty();
    }

    /** Whether a component says what it does itself, rather than leaving it to its base. */
    private static boolean behaves(Component component) {
        return !component.equations().isEmpty() || component.script().isPresent() || !component.parts().isEmpty();
    }

    /** Each of the component's parameters at this part: the part's setting, else the component's own value. */
    private static Map<String, Formula> parameterValues(SimulatedComponent component, Map<String, Formula> settings) {
        Map<String, Formula> values = new LinkedHashMap<>();
        component.parameters().forEach(parameter -> values.put(parameter.name(),
                settings.getOrDefault(parameter.name(), new Formula.Constant(parameter.value()))));
        return values;
    }

    /**
     * The pins of the package as a part connects them: a pin of every unit by its unit ({@code A.output},
     * {@code B.output}), a shared one — the supplies — once, in the order the component writes its pins.
     */
    static List<String> packagePins(SimulatedComponent component) {
        if (component.units().isEmpty()) {
            return component.pins().stream().map(Pin::name).toList();
        }
        List<String> pins = new ArrayList<>();
        for (String unit : component.units()) {
            component.pins().stream().filter(pin -> !component.shared(pin)).forEach(pin -> pins.add(unit + "." + pin.name()));
        }
        component.pins().stream().filter(component::shared).forEach(pin -> pins.add(pin.name()));
        return pins;
    }

    // ─── element cards ──────────────────────────────────────────────────────────────────────────────

    /** A part that is an element card: its nodes in the card's order, its value or its signal's arguments. */
    private List<SpiceStatement> element(Part part, ElementCard card, Map<String, String> pins, Map<String, Formula> settings,
                                         Map<String, Formula> values, Scope scope) {
        List<String> nodes = card.pins().stream().map(pins::get).toList();
        if (nodes.contains(null)) {
            notes.add(new SpiceNote(0, part.reference() + ": " + card.component() + " connects " + card.pins()
                    + "; left out"));
            return List.of();
        }
        char letter = Character.toUpperCase(card.card().charAt(0));
        String name = elementName(letter, part.reference());
        Set<String> carried = new HashSet<>();
        SpiceStatement statement;
        if (card.signal().isPresent()) {
            statement = signal(part, card, name, nodes, values, scope, carried);
        } else {
            String setting = card.valueSetting();
            carried.add(setting);
            SpiceValue value = value(values.get(setting), scope);
            statement = switch (letter) {
                case 'V', 'I' -> step(name, nodes.get(0), nodes.get(1), value);
                case 'E', 'G' -> new SpiceElement.Controlled(name, nodes.get(0), nodes.get(1),
                        List.of(word(nodes.get(2)), word(nodes.get(3)), argument(value)), SpicePosition.NONE);
                default -> new SpiceElement.Passive(name, nodes.get(0), nodes.get(1), value, List.of(), SpicePosition.NONE);
            };
        }
        if (statement == null) {
            return List.of();
        }
        List<String> rest = settings.keySet().stream().filter(setting -> !carried.contains(setting)).toList();
        if (!rest.isEmpty()) {
            notes.add(new SpiceNote(0, part.reference() + ": " + String.join(", ", rest) + " — a rating, a tolerance or a "
                    + "drift — has no SPICE counterpart on this element; left out"));
        }
        return List.of(statement);
    }

    /** A sine or a square, its arguments the component's parameters the table names. */
    private SpiceStatement signal(Part part, ElementCard card, String name, List<String> nodes, Map<String, Formula> values,
                                  Scope scope, Set<String> carried) {
        Function<String, Formula> argument = role -> {
            String setting = card.setting(role);
            carried.add(setting);
            return values.getOrDefault(setting, new Formula.Constant(0));
        };
        String signal = card.signal().orElseThrow();
        if (signal.equals(SINE)) {
            Formula degrees = new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Binary(Formula.Operator.MULTIPLY,
                    argument.apply(PHASE), new Formula.Constant(180)), new Formula.NamedConstant(MathematicalConstant.PI));
            SpiceWaveform sine = new SpiceWaveform(SpiceWaveform.Kind.SIN, List.of(value(argument.apply(OFFSET), scope),
                    value(argument.apply(AMPLITUDE), scope), value(argument.apply(FREQUENCY), scope), SpiceValue.number(0),
                    SpiceValue.number(0), value(Simplification.simplify(degrees), scope)), SpicePosition.NONE);
            return new SpiceElement.Source(name, nodes.get(0), nodes.get(1), null, false, sine, List.of(), SpicePosition.NONE);
        }
        if (!signal.equals(PULSE)) {
            notes.add(new SpiceNote(0, part.reference() + ": the table's signal '" + signal + "' is not one this writer "
                    + "knows; left out"));
            return null;
        }
        Formula period = new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Constant(1), argument.apply(FREQUENCY));
        Formula folded = Simplification.simplify(period);
        double periodValue = scope.numberOf(folded);
        if (Double.isNaN(periodValue)) {
            notes.add(new SpiceNote(0, part.reference() + ": the square's period cannot be worked out from the parameters, "
                    + "and its edges are a share of it; left out"));
            return null;
        }
        double edge = periodValue * SQUARE_EDGE;
        Formula width = new Formula.Binary(Formula.Operator.SUBTRACT, new Formula.Binary(Formula.Operator.MULTIPLY,
                argument.apply(DUTY), period), new Formula.Constant(edge));
        SpiceWaveform pulse = new SpiceWaveform(SpiceWaveform.Kind.PULSE, List.of(value(argument.apply(INITIAL), scope),
                value(argument.apply(PULSED), scope), SpiceValue.number(0), SpiceValue.number(edge), SpiceValue.number(edge),
                value(Simplification.simplify(width), scope), value(folded, scope)), SpicePosition.NONE);
        return new SpiceElement.Source(name, nodes.get(0), nodes.get(1), null, false, pulse, List.of(), SpicePosition.NONE);
    }

    /**
     * A source as it is in a jMX circuit, which starts from rest: a pulse that rises from 0 at time 0 and stays past the
     * end of any run — SPICE's transient starts from the pulse's value at 0 —, and its value as {@code DC}, which the
     * operating point, the DC sweep and the small-signal analyses stand on (checked against ngspice 47: {@code DC 5
     * PULSE(0 5 …)} is 5 V at the operating point and 0 V at the start of the transient).
     */
    private SpiceElement.Source step(String name, String positive, String negative, SpiceValue value) {
        SpiceValue edge = SpiceValue.number(span * STEP_EDGE);
        SpiceValue forever = SpiceValue.number(FOREVER);
        SpiceWaveform pulse = new SpiceWaveform(SpiceWaveform.Kind.PULSE,
                List.of(SpiceValue.number(0), value, SpiceValue.number(0), edge, edge, forever, forever), SpicePosition.NONE);
        return new SpiceElement.Source(name, positive, negative, value, true, pulse, List.of(), SpicePosition.NONE);
    }

    // ─── devices ────────────────────────────────────────────────────────────────────────────────────

    /** {@code D1 anode cathode 1n4148} and its {@code .model}, one per set of parameter values. */
    private SpiceStatement device(Part part, SpiceDevice device, SimulatedComponent component, Map<String, String> pins,
                                  Map<String, Formula> settings, Map<String, Formula> values, Scope scope) {
        List<SpiceParameter> parameters = modelParameters(device, values, scope);
        boolean overridden = settings.keySet().stream().anyMatch(name -> device.byJmxName(name).isPresent());
        String modelName = spiceName(component.slug()) + (overridden ? "_" + part.reference() : "");
        definitions.putIfAbsent(modelName, new SpiceCard.Model(modelName, device.modelType().name(), parameters,
                SpicePosition.NONE));
        if (reported.add("device:" + component.slug())) {
            notes.add(new SpiceNote(0, component.slug() + " is written as SPICE's own " + device.modelType()
                    + " model with the parameters SPICE names; its jMX equations are not carried"
                    + unmappedNote(device, component)));
        }
        settings.keySet().stream().filter(name -> device.byJmxName(name).isEmpty())
                .forEach(name -> notes.add(new SpiceNote(0, part.reference() + "'s " + name + " has no SPICE model "
                        + "parameter; left out")));
        List<String> terminals = device.pins().stream().map(pins::get).toList();
        String name = elementName(Character.toUpperCase(device.element().charAt(0)), part.reference());
        return switch (Character.toUpperCase(device.element().charAt(0))) {
            case 'D' -> new SpiceElement.Diode(name, terminals.get(0), terminals.get(1), modelName, List.of(), SpicePosition.NONE);
            case 'Q' -> new SpiceElement.Bipolar(name, terminals.get(0), terminals.get(1), terminals.get(2), null, modelName,
                    List.of(), SpicePosition.NONE);
            // W = L = 1: the jMX model's KP is the whole channel and its CGSO, CGDO are farads.
            case 'M' -> new SpiceElement.Mosfet(name, terminals.get(0), terminals.get(1), terminals.get(2), terminals.get(2),
                    modelName, List.of(unitGeometry("W"), unitGeometry("L")), SpicePosition.NONE);
            // SPICE's J line has no element class of its own here: drain, gate, source, model as words.
            default -> new SpiceElement.Generic(name, List.of(word(terminals.get(0)), word(terminals.get(1)),
                    word(terminals.get(2)), word(modelName)), SpicePosition.NONE);
        };
    }

    /** The model card's parameters: each the table names, in SPICE's units, but a 0 jMX means as SPICE's none. */
    private static List<SpiceParameter> modelParameters(SpiceDevice device, Map<String, Formula> values, Scope scope) {
        List<SpiceParameter> parameters = new ArrayList<>();
        for (SpiceDevice.ParameterName parameter : device.parameters()) {
            Formula value = values.get(parameter.jmx());
            if (value == null) {
                continue;
            }
            Formula folded = Simplification.simplify(value);
            // A 0 where SPICE reads 0 as a value but jMX means "none" (RBM = RB): written, SPICE would take the 0.
            if (parameter.spiceDefault().isEmpty() && isZero(folded)) {
                continue;
            }
            if (parameter.temperature() && folded.variables().isEmpty()) {
                parameters.add(SpiceParameter.of(parameter.spice(), SpiceValue.number(parameter.toSpice(
                        SpiceAnalyzer.evaluate(folded, Map.of())))));
            } else if (parameter.temperature()) {
                parameters.add(SpiceParameter.of(parameter.spice(), value(new Formula.Binary(Formula.Operator.SUBTRACT,
                        value, new Formula.Constant(Temperatures.ZERO_CELSIUS)), scope)));
            } else {
                parameters.add(SpiceParameter.of(parameter.spice(), value(value, scope)));
            }
        }
        return parameters;
    }

    private static String unmappedNote(SpiceDevice device, SimulatedComponent component) {
        List<String> unmapped = component.parameters().stream().map(EvaluatedParameter::name)
                .filter(name -> device.byJmxName(name).isEmpty() && !name.equals(SpiceDevices.POLARITY)).toList();
        return unmapped.isEmpty() ? "" : ", and " + String.join(", ", unmapped) + " have no model parameter";
    }

    private static SpiceArgument word(String text) {
        return new SpiceArgument.Word(text, SpicePosition.NONE);
    }

    /** A value as an argument of a line: a number as a word, an expression in braces. */
    private static SpiceArgument argument(SpiceValue value) {
        return value instanceof SpiceValue.Expression expression ? new SpiceArgument.Expression(expression.expression())
                : word(value.written());
    }

    /** A MOSFET's width or length of one metre, written on its device line. */
    private static SpiceArgument unitGeometry(String name) {
        return new SpiceArgument.Assignment(name, word("1"), SpicePosition.NONE);
    }

    // ─── generators ─────────────────────────────────────────────────────────────────────────────────

    /** A component that is one voltage between two pins, an equation of the time and its parameters: a {@code B} source. */
    private Optional<List<SpiceStatement>> generator(String reference, SimulatedComponent component, Map<String, String> pins,
                                                     Map<String, Formula> values, Scope scope) {
        Optional<Equation.Branch> branch = component.generated();
        if (branch.isEmpty() || !branch.get().kind().equals(PinQuantities.VOLTAGE)) {
            return Optional.empty();
        }
        Formula shape;
        try {
            shape = ModelLanguage.read(component.title(), branch.get().value().written(),
                    ModelLanguage.definitions(component.title(), component.formulas()));
        } catch (RuntimeException unreadable) {
            return Optional.empty();
        }
        Set<String> pinNames = new HashSet<>(pins.keySet());
        if (shape.variables().stream().anyMatch(pinNames::contains)) {
            return Optional.empty();
        }
        Formula substituted = Substitution.substitute(shape, values);
        try {
            String written = EXPRESSIONS.write(substituted, name -> behaviouralName(name, Map.of(), scope, Function.identity()));
            return Optional.of(List.of(new SpiceElement.Behavioural(elementName('B', reference), pins.get(branch.get().from()),
                    pins.get(branch.get().to()), true, written, SpicePosition.NONE)));
        } catch (UntranslatableFormula untranslatable) {
            return Optional.empty();
        }
    }

    // ─── subcircuits ────────────────────────────────────────────────────────────────────────────────

    /**
     * The {@code .subckt} a component is, written once among the definitions: its parts, and its equations as
     * {@code B} sources. Empty, with a note, when an equation is one SPICE cannot say.
     */
    private Optional<String> subcircuit(SimulatedComponent component) {
        String name = spiceName(component.slug());
        if (definitions.containsKey(name)) {
            return Optional.of(name);
        }
        if (refusedSubcircuits.contains(name)) {
            return Optional.empty();
        }
        if (!writingSubcircuits.add(name)) {
            throw new SpiceParseException(SpicePosition.NONE, component.slug() + " contains itself");
        }
        Optional<String> refusal = unsupported(component);
        List<SpiceStatement> body = new ArrayList<>();
        for (String unit : component.unitNames()) {
            if (refusal.isPresent()) {
                break;
            }
            refusal = body(component, unit, body);
        }
        writingSubcircuits.remove(name);
        if (refusal.isPresent()) {
            refusedSubcircuits.add(name);
            notes.add(new SpiceNote(0, component.slug() + " cannot be a SPICE subcircuit: " + refusal.get()
                    + "; the parts that are one are left out"));
            return Optional.empty();
        }
        List<SpiceParameter> parameters = new ArrayList<>(component.parameters().stream()
                .map(parameter -> SpiceParameter.of(parameter.name(), SpiceValue.number(parameter.value()))).toList());
        // Each unit reads its parameters by its own names (A_gain), the package's value unless a part sets the unit's.
        for (String unit : component.units()) {
            component.parameters().forEach(parameter -> parameters.add(SpiceParameter.of(unit + "_" + parameter.name(),
                    SpiceValue.expression(parameter.name()))));
        }
        definitions.put(name, SpiceCard.Subcircuit.of(name, packagePins(component).stream().map(SpiceExporter::subcircuitNode)
                .toList(), parameters, body));
        return Optional.of(name);
    }

    /** What a subcircuit cannot say. */
    private static Optional<String> unsupported(SimulatedComponent component) {
        if (component.states().stream().anyMatch(state -> !state.continuous())) {
            return Optional.of("it keeps a discrete state, which only its behaviour changes");
        }
        if (!component.behaviours().isEmpty()) {
            return Optional.of("it has behaviour");
        }
        if (!component.units().isEmpty() && component.subcircuit()) {
            return Optional.of("it is a package of several units built of parts");
        }
        return Optional.empty();
    }

    /**
     * One unit's statements into {@code body} — the component's, for a package of one —; the reason it cannot be
     * written, when it cannot. A unit's pins are the package's pins of that unit ({@code A_output}), its inner nodes and
     * its elements are its own ({@code A_stage}, {@code BA1}), and it reads its parameters by its own names.
     *
     * @param unit the unit, {@code A}; empty for a package of one
     */
    private Optional<String> body(SimulatedComponent component, String unit, List<SpiceStatement> body) {
        String prefix = unit.isEmpty() ? "" : unit + "_";
        Set<String> parameters = new LinkedHashSet<>();
        Map<String, Double> values = new LinkedHashMap<>();
        component.parameters().forEach(parameter -> {
            parameters.add(parameter.name());
            values.put(parameter.name(), parameter.value());
        });
        Scope scope = new Scope(parameters, values);
        for (Part part : component.parts()) {
            body.addAll(part(part, scope));
        }
        Map<String, String> nodes = nodes(component, prefix);
        Function<String, String> names = name -> behaviouralName(name, nodes, scope, parameter -> prefix + parameter);
        try {
            List<Formula.Definition> library = ModelLanguage.definitions(component.title(), component.formulas());
            Equations equations = new Equations(component.title(), library);
            component.locals().forEach(local -> equations.local(local.name(), local.value().written()));
            Elements elements = new Elements(unit, body);
            for (State state : component.states()) {
                elements.state(node(nodes, state.name()));
            }
            for (Equation equation : component.equations()) {
                switch (equation) {
                    case Equation.Derivative derivative -> elements.rate(node(nodes, derivative.state()),
                            EXPRESSIONS.write(equations.read(derivative.rate().written()), names));
                    case Equation.Branch branch when branch.kind().equals(PinQuantities.VOLTAGE) -> elements.voltage(
                            node(nodes, branch.from()), node(nodes, branch.to()),
                            EXPRESSIONS.write(requireNoRate(equations.read(branch.value().written())), names));
                    case Equation.Branch branch -> elements.current(node(nodes, branch.from()), node(nodes, branch.to()),
                            Rates.of(equations.read(branch.value().written())), names);
                    default -> {
                    }
                }
            }
            if (equations.ideal && reported.add("ideal:" + component.slug())) {
                notes.add(new SpiceNote(0, component.slug() + " is written as on an ideal bench: 'realistic' is 0, so what it "
                        + "draws there is what it draws in SPICE"));
            }
            if (equations.noisy && reported.add("noise:" + component.slug())) {
                notes.add(new SpiceNote(0, component.slug() + "'s noise sources (noise(…)) have no SPICE counterpart: they "
                        + "add nothing to the other analyses, and SPICE's noise analysis leaves them out"));
            }
        } catch (RuntimeException untranslatable) {
            return Optional.of(untranslatable.getMessage());
        }
        return Optional.empty();
    }

    /**
     * A component's expressions as one unit reads them: its {@code let}s put in place, in order; a pin's members read as
     * its voltage or {@link #PIN_CURRENT}; the ideal bench's terms folded away; a noise source, which adds nothing but to
     * a noise analysis, as 0.
     */
    private static final class Equations {

        private final String where;
        private final List<Formula.Definition> library;
        private final Map<String, Formula> locals = new LinkedHashMap<>();
        private boolean ideal;
        private boolean noisy;

        Equations(String where, List<Formula.Definition> library) {
            this.where = where;
            this.library = library;
        }

        void local(String name, String formula) {
            locals.put(name, read(formula));
        }

        Formula read(String formula) {
            Formula expression = Substitution.replaceHostCalls(ModelLanguage.read(where, formula, library), call -> {
                if (call.name().equals(ModelLanguage.NOISE)) {
                    noisy = true;
                    return new Formula.Constant(0);
                }
                if (call.arguments().size() == 1 && call.arguments().getFirst() instanceof Formula.Variable pin) {
                    return call.name().equals(PinQuantities.VOLTAGE) ? pin : call.name().equals(PinQuantities.CURRENT)
                            ? new Formula.Variable(PIN_CURRENT + pin.name()) : call;
                }
                return call;
            });
            expression = Substitution.substitute(expression, locals);
            String realistic = EngineName.REALISTIC.written();
            if (expression.variables().contains(realistic)) {
                expression = IdealBench.fold(Substitution.substitute(expression, realistic, new Formula.Constant(0)));
                ideal = true;
            }
            return expression;
        }
    }

    /**
     * A current equation's right side split as a capacitor's is: what flows now ({@code rest}), and the charges whose
     * rate of change flows besides — {@code d(q)/dt}, each with its sign.
     */
    private record Rates(Formula rest, List<Formula> charges) {

        static Rates of(Formula expression) {
            List<Formula> now = new ArrayList<>();
            List<Formula> charges = new ArrayList<>();
            gather(expression, false, now, charges);
            Formula rest = now.stream().reduce((left, right) -> new Formula.Binary(Formula.Operator.ADD, left, right))
                    .orElse(new Formula.Constant(0));
            return new Rates(rest, charges);
        }

        /** Every term of a sum, each with its sign: a rate's charge into {@code charges}, any other term into {@code now}. */
        private static void gather(Formula formula, boolean negated, List<Formula> now, List<Formula> charges) {
            if (formula instanceof Formula.HostCall call && call.name().equals(ModelLanguage.RATE) && call.arguments().size() == 1) {
                charges.add(signed(call.arguments().getFirst(), negated));
                return;
            }
            if (formula instanceof Formula.Binary binary && binary.operator() == Formula.Operator.ADD) {
                gather(binary.left(), negated, now, charges);
                gather(binary.right(), negated, now, charges);
                return;
            }
            if (formula instanceof Formula.Binary binary && binary.operator() == Formula.Operator.SUBTRACT) {
                gather(binary.left(), negated, now, charges);
                gather(binary.right(), !negated, now, charges);
                return;
            }
            if (formula instanceof Formula.Negation negation) {
                gather(negation.operand(), !negated, now, charges);
                return;
            }
            now.add(signed(requireNoRate(formula), negated));
        }

        private static Formula signed(Formula formula, boolean negated) {
            return negated ? new Formula.Negation(formula) : formula;
        }
    }

    /** A formula with no rate of change in it: a rate is said only as a term of a current, where a capacitor carries it. */
    private static Formula requireNoRate(Formula formula) {
        boolean[] found = {false};
        Substitution.replaceHostCalls(formula, call -> {
            found[0] |= call.name().equals(ModelLanguage.RATE);
            return call;
        });
        if (found[0]) {
            throw new UntranslatableFormula("a rate of change d(…)/dt that is not a term of a current, which SPICE can "
                    + "carry only through a capacitor");
        }
        return formula;
    }

    /**
     * The SPICE elements one unit's equations become, named after the unit ({@code BA1}, {@code CA_gain_stage}):
     * <ul>
     *     <li>a voltage or a current that is an expression of the pins — a {@code B} source;</li>
     *     <li>a charge's rate {@code d(q)/dt} in a current — a {@code B} source holding {@code q} on a node of its own,
     *     a 1 F capacitor whose current is then {@code dq/dt}, sensed by a zero-volt source and driven between the pins
     *     by a current-controlled source. A {@code ddt()} in a B source would say it too, but ngspice leaves it out of
     *     the small-signal analyses (checked against ngspice 47: an RC whose C is {@code ddt} has a flat response);</li>
     *     <li>a state {@code d(x)/dt = f} — a node {@code x} with a 1 F capacitor to ground and a {@code B} current
     *     source pushing {@code f} into it, so {@code dx/dt = f} exactly.</li>
     * </ul>
     */
    private static final class Elements {

        private final String unit;
        private final List<SpiceStatement> body;
        private int index = 1;

        Elements(String unit, List<SpiceStatement> body) {
            this.unit = unit;
            this.body = body;
        }

        private String name(char letter) {
            String name = letter + unit + index;
            index += 1;
            return name;
        }

        void state(String node) {
            body.add(new SpiceElement.Passive(name('C'), node, Circuit.GROUND, SpiceValue.number(HOLDING_FARAD), List.of(),
                    SpicePosition.NONE));
        }

        void rate(String node, String written) {
            body.add(new SpiceElement.Behavioural(name('B'), Circuit.GROUND, node, false, written, SpicePosition.NONE));
        }

        void voltage(String positive, String negative, String written) {
            body.add(new SpiceElement.Behavioural(name('B'), positive, negative, true, written, SpicePosition.NONE));
        }

        void current(String positive, String negative, Rates rates, Function<String, String> names) {
            if (!isZero(Simplification.simplify(rates.rest()))) {
                body.add(new SpiceElement.Behavioural(name('B'), positive, negative, false,
                        EXPRESSIONS.write(rates.rest(), names), SpicePosition.NONE));
            }
            for (Formula charge : rates.charges()) {
                if (isZero(Simplification.simplify(charge))) {
                    continue;
                }
                String held = "q" + unit + index;
                String sensed = held + "_sensed";
                String sense = name('V');
                body.add(new SpiceElement.Behavioural(name('B'), held, Circuit.GROUND, true, EXPRESSIONS.write(charge, names),
                        SpicePosition.NONE));
                body.add(new SpiceElement.Source(sense, held, sensed, SpiceValue.number(0), true, null, List.of(),
                        SpicePosition.NONE));
                body.add(new SpiceElement.Passive(name('C'), sensed, Circuit.GROUND, SpiceValue.number(HOLDING_FARAD),
                        List.of(), SpicePosition.NONE));
                body.add(new SpiceElement.Generic(name('F'), List.of(word(positive), word(negative), word(sense), word("1")),
                        SpicePosition.NONE));
            }
        }
    }

    /**
     * The names an equation reads as node voltages, and the node each is in the subcircuit: the component's pins and
     * its inner nodes — a unit's under its prefix ({@code A_output}), a shared pin as it is.
     */
    private static Map<String, String> nodes(SimulatedComponent component, String prefix) {
        Map<String, String> nodes = new LinkedHashMap<>();
        component.pins().forEach(pin -> nodes.put(pin.name(),
                subcircuitNode(component.shared(pin) ? pin.name() : prefix + pin.name())));
        for (InternalNode node : component.nodes()) {
            nodes.put(node.name(), subcircuitNode(prefix + node.name()));
        }
        component.states().forEach(state -> nodes.put(state.name(), subcircuitNode(prefix + state.name())));
        return nodes;
    }

    /** The subcircuit node a name of the component is: a pin, an inner node or a state; ground is 0. */
    private static String node(Map<String, String> nodes, String name) {
        return nodes.getOrDefault(name, name);
    }

    /**
     * How a name in an equation is written in a B source: a node's voltage {@code V(node)}, a parameter in braces, the
     * time and the temperature as ngspice names them.
     *
     * @param parameterName how a parameter is named where the expression stands: a unit reads its own ({@code A_gain})
     */
    private static String behaviouralName(String name, Map<String, String> nodes, Scope scope,
                                          Function<String, String> parameterName) {
        if (nodes.containsKey(name)) {
            return "V(" + nodes.get(name) + ")";
        }
        if (scope.parameters().contains(name)) {
            return "{" + parameterName.apply(name) + "}";
        }
        String time = EngineName.TIME.written();
        String temperature = EngineName.TEMPERATURE.written();
        // The bench's other figures as the nominal bench a deck runs on sets them: ideal, every part at its value,
        // standard detail. The temperature is ngspice's own, below.
        Double figure = BenchConditions.nominal().figures().get(name);
        if (figure != null && !name.equals(temperature)) {
            return SpiceNumbers.write(figure);
        }
        if (name.startsWith(PIN_CURRENT)) {
            throw new UntranslatableFormula("the current into pin '" + name.substring(PIN_CURRENT.length())
                    + "' is not something a B source can read");
        }
        if (name.equals(time)) {
            return SPICE_TIME;
        }
        if (name.equals(temperature)) {
            return "(" + SPICE_TEMPERATURE + " + " + SpiceNumbers.write(Temperatures.ZERO_CELSIUS) + ")";
        }
        throw new UntranslatableFormula("'" + name + "' is neither a node, a parameter nor the time");
    }

    // ─── a component on its own ─────────────────────────────────────────────────────────────────────

    private SpiceDeck component(DocumentNode document) {
        CatalogueDocuments withIt = slug -> slug.equals(document.slug()) ? Optional.of(document) : catalogue.find(slug);
        SimulatedComponent component = ComponentCompilation.compile(SimulationReading.component(document), withIt);
        components.put(component.slug(), Optional.of(component));
        Optional<SpiceDevice> device = devices.deviceOf(component);
        Trivia origin = Trivia.comment(component.title() + " — written from jMX component " + component.slug());
        if (device.isPresent() && device.get().pins().stream().allMatch(pin -> component.pin(pin).isPresent())) {
            Map<String, Formula> values = parameterValues(component, Map.of());
            SpiceCard.Model card = new SpiceCard.Model(spiceName(component.slug()), device.get().modelType().name(),
                    modelParameters(device.get(), values, new Scope(Set.of(), Map.of())), SpicePosition.NONE);
            card.addLeadingTrivia(List.of(origin, Trivia.comment("connected as " + device.get().modelType() + " "
                    + String.join(" ", device.get().pins()))));
            notes.add(new SpiceNote(0, component.slug() + " is written as SPICE's own " + device.get().modelType()
                    + " model; its jMX equations are not carried" + unmappedNote(device.get(), component)));
            return SpiceDeck.of(null, List.of(card));
        }
        Optional<String> subcircuit = subcircuit(component);
        if (subcircuit.isEmpty()) {
            throw new SpiceParseException(SpicePosition.ofLine(document.line()), notes.isEmpty()
                    ? component.slug() + " cannot be written as SPICE" : notes.getLast().text());
        }
        List<SpiceStatement> statements = new ArrayList<>(definitions.values());
        statements.getFirst().addLeadingTrivia(List.of(origin));
        return SpiceDeck.of(null, statements);
    }

    // ─── the catalogue ──────────────────────────────────────────────────────────────────────────────

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

    // ─── values ─────────────────────────────────────────────────────────────────────────────────────

    /** A value as SPICE writes it: a number, or the expression in braces over the parameters in scope. */
    private static SpiceValue value(Formula formula, Scope scope) {
        return value(formula, name -> {
            if (!scope.parameters().contains(name)) {
                throw new UntranslatableFormula("'" + name + "' is not a parameter here");
            }
            return name;
        });
    }

    private static SpiceValue value(Formula formula, Function<String, String> names) {
        Formula folded = Simplification.simplify(formula);
        if (folded.variables().isEmpty()) {
            try {
                return SpiceValue.number(SpiceAnalyzer.evaluate(folded, Map.of()));
            } catch (RuntimeException notANumber) {
                // falls through to the expression
            }
        }
        return SpiceValue.expression(EXPRESSIONS.write(formula, names));
    }

    private static boolean isZero(Formula formula) {
        return formula instanceof Formula.Constant constant && constant.value() == 0;
    }

    /** What is left of an expression on an ideal bench, where every term multiplied by {@code realistic} is gone. */
    private static final class IdealBench {

        private IdealBench() {
        }

        static Formula fold(Formula formula) {
            return switch (formula) {
                case Formula.Binary binary -> {
                    Formula left = fold(binary.left());
                    Formula right = fold(binary.right());
                    yield switch (binary.operator()) {
                        case MULTIPLY -> isZero(left) || isZero(right) ? new Formula.Constant(0)
                                : new Formula.Binary(binary.operator(), left, right);
                        case DIVIDE -> isZero(left) ? new Formula.Constant(0) : new Formula.Binary(binary.operator(), left, right);
                        case ADD -> isZero(left) ? right : isZero(right) ? left : new Formula.Binary(binary.operator(), left, right);
                        case SUBTRACT -> isZero(right) ? left : isZero(left) ? new Formula.Negation(right)
                                : new Formula.Binary(binary.operator(), left, right);
                        case POWER -> new Formula.Binary(binary.operator(), left, right);
                    };
                }
                case Formula.Negation negation -> {
                    Formula operand = fold(negation.operand());
                    yield isZero(operand) ? new Formula.Constant(0) : new Formula.Negation(operand);
                }
                default -> formula;
            };
        }
    }
}
