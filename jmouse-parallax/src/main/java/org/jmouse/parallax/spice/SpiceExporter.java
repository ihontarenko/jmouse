package org.jmouse.parallax.spice;

import org.jmouse.parallax.component.TemperatureScale;

import org.jmouse.el.node.Trivia;
import org.jmouse.formula.Formula;
import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.Simplification;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ComponentVocabulary;
import org.jmouse.parallax.component.ModelLanguage;
import org.jmouse.parallax.component.PinQuantities;
import org.jmouse.parallax.circuit.CircuitTopology;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.spice.SpiceNumbers;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceElement;
import org.jmouse.spice.node.SpiceParameter;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.spice.expression.SpiceExpressionLanguage;
import org.jmouse.spice.expression.UntranslatableFormula;
import org.jmouse.spice.node.SpiceWaveform;
import org.jmouse.spice.printer.SpicePrinter;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Writes a jMX {@code circuit} as a SPICE deck, or a jMX {@code component} as the {@code .model} or {@code .subckt}
 * SPICE describes it with: the deck is built as jmouse-spice nodes and written by its one {@link SpicePrinter}, in
 * ngspice's dialect.
 *
 * <h2>What each jMX part becomes</h2>
 * <ul>
 *     <li>{@code resistor}, {@code capacitor}, {@code inductor} → {@code R}, {@code C}, {@code L}.</li>
 *     <li>{@code voltage-source} → a {@code PULSE} that rises from 0 at time 0 in a thousand-millionth of the run and
 *     stays: a jMX circuit starts from rest and every source is switched on at the start, where a SPICE {@code DC}
 *     source would start the run at its operating point. {@code current-source} → {@code I} with its nodes swapped (see
 *     {@link SpiceImporter}).</li>
 *     <li>A generator — one {@code voltage positive -> negative} equation of the time and its parameters — →
 *     {@code SIN(…)} when its equation is the sine's, {@code PULSE(…)} when it is the square's (its edges a millionth
 *     of the period, its width at half height), else a {@code B} source with the equation itself.</li>
 *     <li>The engine's {@code diode} → {@code D} with a {@code .model} of its {@code IS}. A component that is a diode,
 *     a bipolar transistor or a MOSFET ({@link SpiceDevice}) → {@code D}, {@code Q}, {@code M} and a {@code .model} of
 *     the parameters SPICE names; its jMX equations are not carried — SPICE solves its own device model with them.</li>
 *     <li>Any other component → a {@code .subckt} of its parts and of its equations as {@code B} sources, and an
 *     {@code X} instance — when every equation is one SPICE can say: an expression of pin voltages, parameters and
 *     the time. A derivative, a state, behaviour, or a function of the document's own is not; such a part is noted and
 *     left out, never written in part.</li>
 * </ul>
 *
 * <p>The deck runs on an ideal bench: {@code realistic} is 0, and what an instrument draws on an ideal bench is what
 * it draws in SPICE — an ammeter is a zero-volt source, a voltmeter draws nothing. The layout, the probes and the
 * checks have no SPICE counterpart and are named in the notes.</p>
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

    private static final String SINE   = "offset+amplitude*sin(2*pi*frequency*time+phase)";
    private static final String SQUARE = "low+(high-low)*(mod(frequency*time,1)<duty)";

    /** Every formula the deck carries is written by the SPICE dialect, walking its tree. */
    private static final SpiceExpressionLanguage EXPRESSIONS = SpiceExpressionLanguage.standard();

    private final ComponentLibrary library;
    private final List<SpiceNote>  notes = new ArrayList<>();
    /** Model and subcircuit cards, by name, in the order first needed: each written once. */
    private final Map<String, SpiceCard> definitions = new LinkedHashMap<>();
    /** References already reported on, so a component used by ten parts is noted once. */
    private final Set<String> reported = new java.util.HashSet<>();
    private final Map<String, String> renamedElements = new LinkedHashMap<>();
    /** Every part written as an element, by its id: the name SPICE knows it by. */
    private final Map<String, String> elementNames = new LinkedHashMap<>();
    /** The parts the deck could not carry: a deck without them is another circuit. */
    private final Set<String> leftOut = new LinkedHashSet<>();
    /** Subcircuits that cannot be written, each noted once; and those being written, to refuse one inside itself. */
    private final Set<String> refusedSubcircuits = new java.util.HashSet<>();
    private final Set<String> writingSubcircuits = new java.util.HashSet<>();
    private double span = 0.01;
    /** Parameters at other values than the document's — where the bench's sliders stand. */
    private final Map<String, Double> overrides;

    SpiceExporter(ComponentLibrary library) {
        this(library, Map.of());
    }

    SpiceExporter(ComponentLibrary library, Map<String, Double> overrides) {
        this.library = library;
        this.overrides = Map.copyOf(overrides);
    }

    /**
     * The deck written, and what was not carried across.
     *
     * @param elementNames the name each part written as an element has in SPICE, by its id ({@code 1} → {@code R1})
     * @param leftOut      the parts the deck does not carry: SPICE's answer is then another circuit's
     */
    record Written(SpiceDeck deck, List<SpiceNote> notes, Map<String, String> elementNames, Set<String> leftOut) {
        Written {
            notes = List.copyOf(notes);
            elementNames = Map.copyOf(elementNames);
            leftOut = Set.copyOf(leftOut);
        }

        String text() {
            return SpicePrinter.print(deck);
        }
    }

    Written write(String source) {
        ExperienceDocument document = ExperienceReader.readOne(source, "document");
        SpiceDeck deck = switch (document.kind()) {
            case CIRCUIT -> circuit(document);
            case COMPONENT -> component(document);
            default -> throw new SpiceParseException(SpicePosition.ofLine(document.line()), "SPICE describes circuits "
                    + "and components; this is a " + document.kind().keyword());
        };
        return new Written(deck, notes, elementNames, leftOut);
    }

    // ─── a circuit ──────────────────────────────────────────────────────────────────────────────────

    private SpiceDeck circuit(ExperienceDocument document) {
        double start = 0;
        Optional<Block.Analysis> analysis = document.blocks(Block.Analysis.class).stream()
                .filter(written -> written.kind() == Block.AnalysisKind.TRANSIENT).findFirst();
        if (analysis.isPresent()) {
            Map<String, SourceText> settings = analysis.get().settings();
            if (settings.containsKey("for")) {
                span = seconds(settings.get("for"));
            } else if (settings.containsKey("from") && settings.containsKey("to")) {
                start = seconds(settings.get("from"));
                span = seconds(settings.get("to"));
            }
        }
        List<SpiceStatement> statements = new ArrayList<>();
        Set<String> parameterNames = new LinkedHashSet<>();
        Map<String, Double> parameterValues = new LinkedHashMap<>();
        boolean ranged = false;
        for (Block.Parameter parameter : document.parameters()) {
            parameterNames.add(parameter.name());
            parameterValues.put(parameter.name(), overrides.containsKey(parameter.name())
                    ? overrides.get(parameter.name()) : parameter.quantity().map(quantity -> quantity.value())
                    .orElseGet(() -> SpiceAnalyzer.evaluate(formula(parameter.value().text(), parameter.value().line()),
                            parameterValues)));
            SpiceCard.Parameters card = new SpiceCard.Parameters(List.of(SpiceParameter.of(parameter.name(),
                    parameterValue(parameter))), SpicePosition.NONE);
            if (parameter.description() != null && !parameter.description().isBlank()) {
                card.addLeadingTrivia(List.of(Trivia.comment(parameter.name() + ": " + parameter.description())));
            }
            statements.add(card);
            ranged |= parameter.range().isPresent();
        }
        if (ranged) {
            notes.add(new SpiceNote(0, "the parameters' ranges are left out: SPICE keeps values, not sliders"));
        }
        Scope scope = new Scope(parameterNames, parameterValues);
        for (Block.Part part : document.block(Block.Parts.class).map(Block.Parts::parts).orElse(List.of())) {
            statements.addAll(part(part, scope));
        }
        statements.addAll(definitions.values());
        statements.add(new SpiceCard.Transient(SpiceValue.number((span - start) * PRINTING_STEP), SpiceValue.number(span),
                start == 0 ? null : SpiceValue.number(start), null, false, SpicePosition.NONE));
        statements.add(new SpiceCard.End(SpicePosition.NONE));
        noteLeftOutBlocks(document);
        if (!renamedElements.isEmpty()) {
            notes.add(new SpiceNote(0, "a SPICE element's name begins with its letter: " + String.join(", ",
                    renamedElements.entrySet().stream().map(entry -> entry.getKey() + " is " + entry.getValue()).toList())));
        }
        return SpiceDeck.of(document.title(), statements);
    }

    private void noteLeftOutBlocks(ExperienceDocument document) {
        if (document.block(Block.Layout.class).isPresent()) {
            notes.add(new SpiceNote(0, "the layout has no SPICE counterpart: a netlist is not drawn; left out"));
        }
        document.block(Block.Probes.class).ifPresent(probes -> notes.add(new SpiceNote(0, "the probes ("
                + String.join(", ", probes.probes().stream().map(Block.Probe::id).toList()) + ") have no SPICE "
                + "counterpart; left out — print the nodes they read")));
        int checks = document.blocks(Block.Expectation.class).size();
        if (checks > 0) {
            notes.add(new SpiceNote(0, checks + " expectation(s) have no SPICE counterpart; left out"));
        }
        document.blocks(Block.Analysis.class).stream().filter(written -> written.kind() != Block.AnalysisKind.TRANSIENT)
                .forEach(written -> notes.add(new SpiceNote(0, "the " + written.kind().name().toLowerCase(Locale.ROOT)
                        + " analysis is left out; only the transient is written")));
    }

    private SpiceValue parameterValue(Block.Parameter parameter) {
        if (overrides.containsKey(parameter.name())) {
            return SpiceValue.number(overrides.get(parameter.name()));
        }
        if (parameter.quantity().isPresent()) {
            return SpiceValue.number(parameter.quantity().get().value());
        }
        return value(formula(parameter.value().text(), parameter.value().line()), name -> name);
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

    /** A SPICE element name: the jMX id, its letter put in front when it does not begin with it. */
    private String elementName(char letter, String id) {
        if (Character.toUpperCase(id.charAt(0)) == letter) {
            elementNames.put(id, id);
            return id;
        }
        String name = letter + id;
        renamedElements.put(id, name);
        elementNames.put(id, name);
        return name;
    }

    /** A component's reference as a SPICE name: ngspice reads a {@code -} in a name as a minus. */
    static String spiceName(String reference) {
        return reference.replace('-', '_').replace("@", "_revision_");
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    /** The statements one part becomes; none, with a note, when it is left out. */
    private List<SpiceStatement> part(Block.Part part, Scope scope) {
        List<SpiceStatement> written;
        try {
            written = translatedPart(part, scope);
        } catch (UntranslatableFormula untranslatable) {
            notes.add(new SpiceNote(part.line(), part.id() + ": " + untranslatable.getMessage() + "; left out"));
            written = List.of();
        }
        if (written.isEmpty()) {
            leftOut.add(part.id());
        }
        return written;
    }

    private List<SpiceStatement> translatedPart(Block.Part part, Scope scope) {
        Map<String, Formula> settings = new LinkedHashMap<>();
        part.settings().forEach((name, text) -> {
            // A text — a display's first line, a chip's start-up bytes — is read by the part's behaviour, never by its
            // equations; SPICE has no place for it.
            if (text.text().startsWith("\"")) {
                notes.add(new SpiceNote(part.line(), part.id() + "'s " + name + " is a text, which SPICE has no place "
                        + "for; left out"));
                return;
            }
            settings.put(name, setting(text));
        });
        if (!part.positional().isEmpty()) {
            return primitive(part.id(), part.reference(), part.positional().get(0), part.positional().get(1), settings,
                    scope, part.line());
        }
        Optional<String> source = library.source(part.reference());
        if (source.isEmpty()) {
            notes.add(new SpiceNote(part.line(), part.id() + ": component '" + part.reference() + "' is not in the "
                    + "library; left out"));
            return List.of();
        }
        ExperienceDocument document;
        ComponentModel model;
        try {
            document = ExperienceReader.readOne(source.get(), part.reference());
            model = ComponentDocuments.model(document, library);
        } catch (RuntimeException unreadable) {
            notes.add(new SpiceNote(part.line(), part.id() + ": component '" + part.reference() + "' does not read: "
                    + unreadable.getMessage() + "; left out"));
            return List.of();
        }
        Map<String, String> pins = new LinkedHashMap<>();
        List<String> packagePins = packagePins(model);
        part.named().forEach((name, net) -> {
            if (packagePins.contains(name)) {
                pins.put(name, net);
            } else if (model.parameter(name).isPresent() || unitParameter(model, name)) {
                settings.put(name, setting(new SourceText(net, part.line(), 0, 0)));
            } else {
                notes.add(new SpiceNote(part.line(), part.id() + ": '" + name + "' is neither a pin nor a parameter of "
                        + part.reference() + "; left out"));
            }
        });
        return model(part, document, model, pins, settings, scope);
    }

    /** A setting as a formula: a quantity in SI, or the jMF expression over the parameters. */
    private static Formula setting(SourceText text) {
        String written = text.text();
        if (Quantity.isQuantity(written)) {
            return new Formula.Constant(Quantity.valueOf(written));
        }
        return formula(written, text.line());
    }

    private List<SpiceStatement> primitive(String id, String reference, String positive, String negative,
                                           Map<String, Formula> settings, Scope scope, int line) {
        Map<String, Formula> rest = new LinkedHashMap<>(settings);
        SpiceStatement statement = switch (reference) {
            case "resistor" -> new SpiceElement.Passive(elementName('R', id), positive, negative,
                    value(required(rest, "resistance", id, line), scope), List.of(), SpicePosition.NONE);
            case "capacitor" -> new SpiceElement.Passive(elementName('C', id), positive, negative,
                    value(required(rest, "capacitance", id, line), scope), List.of(), SpicePosition.NONE);
            case "inductor" -> new SpiceElement.Passive(elementName('L', id), positive, negative,
                    value(required(rest, "inductance", id, line), scope), List.of(), SpicePosition.NONE);
            case "diode" -> {
                String modelName = "D_" + id;
                definitions.put(modelName, new SpiceCard.Model(modelName, "D", List.of(SpiceParameter.of("IS",
                        value(required(rest, "I_saturation", id, line), scope))), SpicePosition.NONE));
                yield new SpiceElement.Diode(elementName('D', id), positive, negative, modelName, List.of(),
                        SpicePosition.NONE);
            }
            case "voltage-source" -> source(elementName('V', id), positive, negative,
                    value(required(rest, "U_output", id, line), scope));
            case "current-source" -> source(elementName('I', id), negative, positive,
                    value(required(rest, "I_output", id, line), scope));
            default -> null;
        };
        if (statement == null) {
            notes.add(new SpiceNote(line, id + ": '" + reference + "' is not a part connected in pin order; left out"));
            return List.of();
        }
        if (!rest.isEmpty()) {
            notes.add(new SpiceNote(line, id + ": " + String.join(", ", rest.keySet()) + " — a rating, a tolerance or a "
                    + "drift — has no SPICE counterpart on this element; left out"));
        }
        return List.of(statement);
    }

    private static Formula required(Map<String, Formula> settings, String name, String id, int line) {
        Formula value = settings.remove(name);
        if (value == null) {
            throw new SpiceParseException(SpicePosition.ofLine(line), id + " writes no " + name);
        }
        return value;
    }

    /**
     * A source as it is in a jMX circuit, which starts from rest: a pulse that rises from 0 at time 0 and stays past the
     * end of any run — SPICE's transient starts from the pulse's value at 0 —, and its value as {@code DC}, which the
     * operating point, the DC sweep and the small-signal analyses stand on (checked against ngspice 47: {@code DC 5
     * PULSE(0 5 …)} is 5 V at the operating point and 0 V at the start of the transient).
     */
    private SpiceElement.Source source(String name, String positive, String negative, SpiceValue value) {
        SpiceValue edge = SpiceValue.number(span * STEP_EDGE);
        SpiceValue forever = SpiceValue.number(1e30);
        SpiceWaveform pulse = new SpiceWaveform(SpiceWaveform.Kind.PULSE,
                List.of(SpiceValue.number(0), value, SpiceValue.number(0), edge, edge, forever, forever), SpicePosition.NONE);
        return new SpiceElement.Source(name, positive, negative, value, true, pulse, List.of(), SpicePosition.NONE);
    }

    // ─── parts that are components ──────────────────────────────────────────────────────────────────

    private List<SpiceStatement> model(Block.Part part, ExperienceDocument document, ComponentModel model,
                                       Map<String, String> pins, Map<String, Formula> settings, Scope scope) {
        int line = part.line();
        List<String> pinNames = packagePins(model);
        List<SpiceStatement> leaks = openPins(part, pinNames, pins);
        List<SpiceStatement> written = new ArrayList<>(modelStatements(part, document, model, pinNames, pins, settings, scope));
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
    private List<SpiceStatement> openPins(Block.Part part, List<String> pinNames, Map<String, String> pins) {
        List<String> open = pinNames.stream().filter(pin -> !pins.containsKey(pin)).toList();
        List<SpiceStatement> leaks = new ArrayList<>();
        for (String pin : open) {
            String node = spiceNode(part.id() + "." + pin);
            pins.put(pin, node);
            leaks.add(new SpiceElement.Passive("R" + node + "_leak", node, "0", SpiceValue.number(CircuitTopology.NODE_LEAK_RESISTANCE),
                    List.of(), SpicePosition.NONE));
        }
        if (!open.isEmpty()) {
            notes.add(new SpiceNote(part.line(), part.id() + ": " + String.join(", ", open) + " connect to nothing — each "
                    + "is a node of its own with the engine's leak to ground, " + SpiceNumbers.write(CircuitTopology.NODE_LEAK_RESISTANCE)
                    + " Ω, as the engine solves it"));
        }
        return leaks;
    }

    private List<SpiceStatement> modelStatements(Block.Part part, ExperienceDocument document, ComponentModel model,
                                                 List<String> pinNames, Map<String, String> pins,
                                                 Map<String, Formula> settings, Scope scope) {
        int line = part.line();
        Map<String, Formula> values = parameterValues(model, settings);
        Optional<String> primitive = primitiveType(document);
        if (primitive.isPresent()) {
            String valueSetting = model.parameters().getFirst().name();
            Map<String, Formula> primitiveSettings = new LinkedHashMap<>();
            primitiveSettings.put(valueSetting, values.get(valueSetting));
            settings.forEach((name, formula) -> {
                if (!name.equals(valueSetting)) {
                    primitiveSettings.put(name, formula);
                }
            });
            return primitive(part.id(), primitive.get(), pins.get(pinNames.get(0)), pins.get(pinNames.get(1)),
                    primitiveSettings, scope, line);
        }
        Optional<SpiceDevice> device = deviceOf(document);
        if (device.isPresent() && pins.keySet().containsAll(device.get().pins())) {
            return List.of(device(part, device.get(), model, pins, settings, values, scope));
        }
        Optional<List<SpiceStatement>> generator = generator(part.id(), document, pins, values, scope);
        if (generator.isPresent()) {
            return generator.get();
        }
        Optional<String> subcircuit = subcircuit(part.reference(), document, model);
        if (subcircuit.isEmpty()) {
            return List.of();
        }
        List<SpiceParameter> overrides = new ArrayList<>();
        settings.forEach((name, formula) -> overrides.add(SpiceParameter.of(spiceNode(name), value(formula, scope))));
        return List.of(new SpiceElement.Instance(elementName('X', part.id()), pinNames.stream().map(pins::get).toList(),
                subcircuit.get(), overrides, SpicePosition.NONE));
    }

    private static Optional<SpiceDevice> deviceOf(ExperienceDocument document) {
        return document.type().flatMap(type -> SpiceDevice.ofComponent(type,
                document.block(Block.Symbol.class).map(Block.Symbol::uses).orElse(List.of())));
    }

    /**
     * The primitive a component is written to SPICE as: a primitive the engine still supplies, or a primitive's own
     * document — {@code resistor}, {@code capacitor} — whose equations, on the ideal nominal bench a deck runs on, are
     * exactly its card ({@code resistance} at no corner and no drift). A component of that type with physics of its own,
     * a thermistor, is no primitive here and is written as what it is.
     */
    private static Optional<String> primitiveType(ExperienceDocument document) {
        boolean primitiveDocument = org.jmouse.parallax.object.ObjectDefinition.CircuitElementType.ofComponent(document.slug()).isPresent();
        return primitiveDocument || ComponentDocuments.primitiveOf(document) != null ? document.type() : Optional.empty();
    }

    /** Each of the component's parameters at this part: the part's setting, else the component's own value. */
    private static Map<String, Formula> parameterValues(ComponentModel model, Map<String, Formula> settings) {
        Map<String, Formula> values = new LinkedHashMap<>();
        model.parameters().forEach(parameter -> values.put(parameter.name(),
                settings.getOrDefault(parameter.name(), new Formula.Constant(parameter.value()))));
        return values;
    }

    /**
     * The pins of the package as a part connects them: a pin of every unit by its unit ({@code A.output},
     * {@code B.output}), a shared one — the supplies — once, in the order the component writes its pins.
     */
    static List<String> packagePins(ComponentModel model) {
        if (model.units().isEmpty()) {
            return model.pins().stream().map(ComponentModel.Pin::name).toList();
        }
        List<String> pins = new ArrayList<>();
        for (String unit : model.units()) {
            model.pins().stream().filter(pin -> !pin.shared()).forEach(pin -> pins.add(unit + "." + pin.name()));
        }
        model.pins().stream().filter(ComponentModel.Pin::shared).forEach(pin -> pins.add(pin.name()));
        return pins;
    }

    /** Whether a setting names one unit's parameter: {@code A.gain} on a package of units A and B. */
    private static boolean unitParameter(ComponentModel model, String name) {
        int dot = name.indexOf('.');
        return dot > 0 && model.units().contains(name.substring(0, dot)) && model.parameter(name.substring(dot + 1)).isPresent();
    }

    /**
     * A node of a subcircuit as ngspice reads it as that node: {@code gnd}, in any case, is ground to ngspice, and a
     * component's pin of that name (an op-amp's negative supply) is no ground — it is renamed, as {@code 0} would be.
     */
    static String subcircuitNode(String name) {
        String node = spiceNode(name);
        return node.equalsIgnoreCase("gnd") || node.equals("0") ? node + "_pin" : node;
    }

    /** A pin or a unit's parameter as SPICE can name it: a dot is no part of a SPICE name, so {@code A.output} is {@code A_output}. */
    static String spiceNode(String name) {
        return name.replace('.', '_');
    }

    /** {@code D1 anode cathode 1n4148} and its {@code .model}, one per set of parameter values. */
    private SpiceStatement device(Block.Part part, SpiceDevice device, ComponentModel model, Map<String, String> pins,
                                  Map<String, Formula> settings, Map<String, Formula> values, Scope scope) {
        String reference = part.reference();
        List<SpiceParameter> parameters = new ArrayList<>();
        for (SpiceDevice.ParameterName parameter : device.parameters()) {
            Formula value = values.get(parameter.jmx());
            if (value == null) {
                continue;
            }
            Formula folded = Simplification.simplify(value);
            // A 0 where SPICE reads 0 as a value but jMX means "none" (RBM = RB): written, SPICE would take the 0.
            if (parameter.spiceDefault() == null && folded instanceof Formula.Constant constant && constant.value() == 0) {
                continue;
            }
            if (parameter.temperature() && folded.variables().isEmpty()) {
                parameters.add(SpiceParameter.of(parameter.spice(), SpiceValue.number(parameter.toSpice(
                        SpiceAnalyzer.evaluate(folded, Map.of())))));
            } else if (parameter.temperature()) {
                parameters.add(SpiceParameter.of(parameter.spice(), value(new Formula.Binary(Formula.Operator.SUBTRACT,
                        value, new Formula.Constant(TemperatureScale.ZERO_CELSIUS)), scope)));
            } else {
                parameters.add(SpiceParameter.of(parameter.spice(), value(value, scope)));
            }
        }
        boolean overridden = settings.keySet().stream().anyMatch(name -> device.byJmxName(name).isPresent());
        String modelName = spiceName(reference) + (overridden ? "_" + part.id() : "");
        definitions.putIfAbsent(modelName, new SpiceCard.Model(modelName, device.modelType().name(), parameters,
                SpicePosition.NONE));
        if (reported.add("device:" + reference)) {
            List<String> unmapped = model.parameters().stream().map(ComponentModel.Parameter::name)
                    .filter(name -> device.byJmxName(name).isEmpty()).toList();
            notes.add(new SpiceNote(part.line(), reference + " is written as SPICE's own " + device.modelType()
                    + " model with the parameters SPICE names; its jMX equations are not carried"
                    + (unmapped.isEmpty() ? "" : ", and " + String.join(", ", unmapped) + " have no model parameter")));
        }
        settings.keySet().stream().filter(name -> device.byJmxName(name).isEmpty())
                .forEach(name -> notes.add(new SpiceNote(part.line(), part.id() + "'s " + name + " has no SPICE model "
                        + "parameter; left out")));
        List<String> terminals = device.pins().stream().map(pins::get).toList();
        return switch (device) {
            case DIODE -> new SpiceElement.Diode(elementName('D', part.id()), terminals.get(0), terminals.get(1), modelName,
                    List.of(), SpicePosition.NONE);
            case NPN, PNP -> new SpiceElement.Bipolar(elementName('Q', part.id()), terminals.get(0), terminals.get(1),
                    terminals.get(2), null, modelName, List.of(), SpicePosition.NONE);
            // W = L = 1: the jMX model's KP is the whole channel and its CGSO, CGDO are farads.
            case NMOS, PMOS -> new SpiceElement.Mosfet(elementName('M', part.id()), terminals.get(0), terminals.get(1),
                    terminals.get(2), terminals.get(2), modelName, List.of(unitGeometry("W"), unitGeometry("L")), SpicePosition.NONE);
            // SPICE's J line has no element class of its own here: drain, gate, source, model as words.
            case NJF, PJF -> new SpiceElement.Generic(elementName('J', part.id()), List.of(word(terminals.get(0)),
                    word(terminals.get(1)), word(terminals.get(2)), word(modelName)), SpicePosition.NONE);
        };
    }

    private static SpiceArgument word(String text) {
        return new SpiceArgument.Word(text, SpicePosition.NONE);
    }

    /** A MOSFET's width or length of one metre, written on its device line. */
    private static SpiceArgument unitGeometry(String name) {
        return new SpiceArgument.Assignment(name, new SpiceArgument.Word("1", SpicePosition.NONE), SpicePosition.NONE);
    }

    // ─── generators ─────────────────────────────────────────────────────────────────────────────────

    /**
     * A component that is one voltage between two pins as an equation of the time and its parameters: {@code SIN},
     * {@code PULSE}, or a {@code B} source with the equation.
     */
    private Optional<List<SpiceStatement>> generator(String id, ExperienceDocument document, Map<String, String> pins,
                                                     Map<String, Formula> values, Scope scope) {
        Optional<Block.Equation> equation = generatorEquation(document);
        if (equation.isEmpty()) {
            return Optional.empty();
        }
        String positive = pins.get(equation.get().positive());
        String negative = pins.get(equation.get().negative());
        String shape = equation.get().expression().text().replace(" ", "");
        Function<String, Formula> parameter = name -> values.getOrDefault(name, new Formula.Constant(0));
        if (shape.equals(SINE)) {
            Formula degrees = new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Binary(Formula.Operator.MULTIPLY,
                    parameter.apply("phase"), new Formula.Constant(180)), new Formula.NamedConstant(MathematicalConstant.PI));
            SpiceWaveform sine = new SpiceWaveform(SpiceWaveform.Kind.SIN, List.of(value(parameter.apply("offset"), scope),
                    value(parameter.apply("amplitude"), scope), value(parameter.apply("frequency"), scope),
                    SpiceValue.number(0), SpiceValue.number(0), value(Simplification.simplify(degrees), scope)),
                    SpicePosition.NONE);
            return Optional.of(List.of(new SpiceElement.Source(elementName('V', id), positive, negative, null, false,
                    sine, List.of(), SpicePosition.NONE)));
        }
        if (shape.equals(SQUARE)) {
            Formula period = new Formula.Binary(Formula.Operator.DIVIDE, new Formula.Constant(1), parameter.apply("frequency"));
            Formula folded = Simplification.simplify(period);
            double periodValue = scope.numberOf(folded);
            if (Double.isNaN(periodValue)) {
                notes.add(new SpiceNote(0, id + ": the square's period cannot be worked out from the parameters, and its "
                        + "edges are a share of it; left out"));
                return Optional.of(List.of());
            }
            double edge = periodValue * SQUARE_EDGE;
            Formula width = new Formula.Binary(Formula.Operator.SUBTRACT, new Formula.Binary(Formula.Operator.MULTIPLY,
                    parameter.apply("duty"), period), new Formula.Constant(edge));
            SpiceWaveform pulse = new SpiceWaveform(SpiceWaveform.Kind.PULSE, List.of(value(parameter.apply("low"), scope),
                    value(parameter.apply("high"), scope), SpiceValue.number(0), SpiceValue.number(edge),
                    SpiceValue.number(edge), value(Simplification.simplify(width), scope), value(folded, scope)),
                    SpicePosition.NONE);
            return Optional.of(List.of(new SpiceElement.Source(elementName('V', id), positive, negative, null, false,
                    pulse, List.of(), SpicePosition.NONE)));
        }
        Formula expression;
        try {
            expression = formula(equation.get().expression().text(), equation.get().expression().line());
        } catch (SpiceParseException unreadable) {
            return Optional.empty();
        }
        Formula substituted = Substitution.substitute(expression, values);
        try {
            String written = EXPRESSIONS.write(substituted, name -> behaviouralName(name, Map.of(), scope));
            return Optional.of(List.of(new SpiceElement.Behavioural(elementName('B', id), positive, negative, true,
                    written, SpicePosition.NONE)));
        } catch (UntranslatableFormula untranslatable) {
            return Optional.empty();
        }
    }

    /** The one {@code voltage a -> b} equation of a component that is nothing else; empty for any other. */
    private static Optional<Block.Equation> generatorEquation(ExperienceDocument document) {
        if (document.block(Block.Parts.class).isPresent() || document.block(Block.State.class).isPresent()
                || document.block(Block.Behaviour.class).isPresent()) {
            return Optional.empty();
        }
        List<Block.Equation> equations = document.block(Block.Equations.class).map(Block.Equations::equations)
                .orElse(List.of());
        if (equations.size() != 1 || equations.getFirst().form() != Block.EquationForm.VOLTAGE) {
            return Optional.empty();
        }
        Set<String> pins = new java.util.HashSet<>();
        document.block(Block.Pins.class).ifPresent(block -> block.pins().forEach(pin -> pins.add(pin.name())));
        Formula expression;
        try {
            expression = FormulaLanguage.standard().parse(equations.getFirst().expression().text());
        } catch (RuntimeException unreadable) {
            return Optional.empty();
        }
        boolean ofTimeAlone = expression.variables().stream().noneMatch(pins::contains);
        return ofTimeAlone ? Optional.of(equations.getFirst()) : Optional.empty();
    }

    // ─── subcircuits ────────────────────────────────────────────────────────────────────────────────

    /**
     * The {@code .subckt} a component is, written once among the definitions: its parts, and its equations as
     * {@code B} sources. Empty, with a note, when an equation is one SPICE cannot say.
     */
    private Optional<String> subcircuit(String reference, ExperienceDocument document, ComponentModel model) {
        String name = spiceName(reference);
        if (definitions.containsKey(name)) {
            return Optional.of(name);
        }
        if (refusedSubcircuits.contains(name)) {
            return Optional.empty();
        }
        if (!writingSubcircuits.add(name)) {
            throw new SpiceParseException(SpicePosition.ofLine(document.line()), reference + " contains itself");
        }
        Optional<String> refusal = unsupportedBlocks(document, model);
        List<SpiceStatement> body = new ArrayList<>();
        for (String unit : model.unitNames()) {
            if (refusal.isPresent()) {
                break;
            }
            refusal = body(document, model, unit, body);
        }
        writingSubcircuits.remove(name);
        if (refusal.isPresent()) {
            refusedSubcircuits.add(name);
            notes.add(new SpiceNote(document.line(), reference + " cannot be a SPICE subcircuit: " + refusal.get()
                    + "; the parts that are one are left out"));
            return Optional.empty();
        }
        List<SpiceParameter> parameters = new ArrayList<>(model.parameters().stream()
                .map(parameter -> SpiceParameter.of(parameter.name(), SpiceValue.number(parameter.value()))).toList());
        // Each unit reads its parameters by its own names (A_gain), the package's value unless a part sets the unit's.
        for (String unit : model.units()) {
            model.parameters().forEach(parameter -> parameters.add(SpiceParameter.of(unit + "_" + parameter.name(),
                    SpiceValue.expression(parameter.name()))));
        }
        definitions.put(name, SpiceCard.Subcircuit.of(name, packagePins(model).stream().map(SpiceExporter::subcircuitNode).toList(),
                parameters, body));
        return Optional.of(name);
    }

    /** What a subcircuit cannot say, as the model — the component with what it inherits — has it. */
    private static Optional<String> unsupportedBlocks(ExperienceDocument document, ComponentModel model) {
        if (model.states().stream().anyMatch(ComponentModel.State::discrete)) {
            return Optional.of("it keeps a discrete state, which only its behaviour changes");
        }
        if (model.behaviour() != null) {
            return Optional.of("it has behaviour");
        }
        if (!model.units().isEmpty() && document.block(Block.Parts.class).isPresent()) {
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
    private Optional<String> body(ExperienceDocument document, ComponentModel model, String unit, List<SpiceStatement> body) {
        String prefix = unit.isEmpty() ? "" : unit + "_";
        Set<String> parameters = new LinkedHashSet<>();
        model.parameters().forEach(parameter -> parameters.add(parameter.name()));
        Map<String, Double> values = new LinkedHashMap<>();
        model.parameters().forEach(parameter -> values.put(parameter.name(), parameter.value()));
        Scope scope = new Scope(parameters, values);
        for (Block.Part part : document.block(Block.Parts.class).map(Block.Parts::parts).orElse(List.of())) {
            body.addAll(part(part, scope));
        }
        Map<String, String> nodes = nodes(model, prefix);
        Function<String, String> names = name -> behaviouralName(name, nodes, scope, parameter -> prefix + parameter);
        // The functions its equations may call — those of the libraries it uses, smooth-transitions —, written in place.
        List<Formula.Definition> library = ModelLanguage.LANGUAGE.definitions(ModelLanguage.libraryText(model));
        Equations equations = new Equations(library);
        try {
            model.locals().forEach(equations::local);
            Elements elements = new Elements(unit, body);
            for (ComponentModel.State state : model.states()) {
                elements.state(node(nodes, state.name()));
            }
            for (ComponentModel.Equation equation : model.equations()) {
                Formula expression = equations.read(equation.formula());
                switch (equation.kind()) {
                    case STATE -> elements.rate(node(nodes, equation.state()), EXPRESSIONS.write(expression, names));
                    case VOLTAGE -> elements.voltage(node(nodes, equation.positive()), node(nodes, equation.negative()),
                            EXPRESSIONS.write(requireNoRate(expression), names));
                    case CURRENT -> elements.current(node(nodes, equation.positive()), node(nodes, equation.negative()),
                            Rates.of(expression), names);
                }
            }
        } catch (RuntimeException untranslatable) {
            return Optional.of(untranslatable.getMessage());
        }
        if (equations.ideal && reported.add("ideal:" + document.slug())) {
            notes.add(new SpiceNote(document.line(), document.slug() + " is written as on an ideal bench: 'realistic' "
                    + "is 0, so what it draws there is what it draws in SPICE"));
        }
        if (equations.noisy && reported.add("noise:" + document.slug())) {
            notes.add(new SpiceNote(document.line(), document.slug() + "'s noise sources (noise(…)) have no SPICE "
                    + "counterpart: they add nothing to the other analyses, and SPICE's noise analysis leaves them out"));
        }
        return Optional.empty();
    }

    /**
     * A model's expressions as one unit reads them: its {@code let}s put in place, in order; a pin's members read as its
     * voltage or {@link #PIN_CURRENT}; the ideal bench's terms folded away; a noise source, which adds nothing but to a
     * noise analysis, as 0.
     */
    private static final class Equations {

        private final List<Formula.Definition> library;
        private final Map<String, Formula> locals = new LinkedHashMap<>();
        private boolean ideal;
        private boolean noisy;

        Equations(List<Formula.Definition> library) {
            this.library = library;
        }

        void local(String name, String formula) {
            locals.put(name, read(formula));
        }

        Formula read(String formula) {
            Formula expression = Substitution.replaceHostCalls(ModelLanguage.LANGUAGE.parse(formula, library), call -> {
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
            if (expression.variables().contains(ComponentVocabulary.REALISTIC)) {
                expression = IdealBench.fold(Substitution.substitute(expression, ComponentVocabulary.REALISTIC,
                        new Formula.Constant(0)));
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
            throw new UntranslatableFormula("a rate of change d(…)/dt that is not a term of a current, which "
                    + "SPICE can carry only through a capacitor");
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
            return letter + unit + index++;
        }

        void state(String node) {
            body.add(new SpiceElement.Passive(name('C'), node, "0", SpiceValue.number(1), List.of(), SpicePosition.NONE));
        }

        void rate(String node, String written) {
            body.add(new SpiceElement.Behavioural(name('B'), "0", node, false, written, SpicePosition.NONE));
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
                body.add(new SpiceElement.Behavioural(name('B'), held, "0", true, EXPRESSIONS.write(charge, names),
                        SpicePosition.NONE));
                body.add(new SpiceElement.Source(sense, held, sensed, SpiceValue.number(0), true, null, List.of(),
                        SpicePosition.NONE));
                body.add(new SpiceElement.Passive(name('C'), sensed, "0", SpiceValue.number(1), List.of(), SpicePosition.NONE));
                body.add(new SpiceElement.Generic(name('F'), List.of(word(positive), word(negative), word(sense), word("1")),
                        SpicePosition.NONE));
            }
        }
    }

    /**
     * What {@code current(pin)} is read as: a name of its own. A B source cannot read the current into a pin, so the
     * name only has to survive until an ideal bench folds it away or {@link #behaviouralName} refuses it.
     */
    private static final String PIN_CURRENT = "current_into_";

    /**
     * The names an equation reads as node voltages, and the node each is in the subcircuit: the component's pins and
     * its inner nodes — a unit's under its prefix ({@code A_output}), a shared pin as it is.
     */
    private static Map<String, String> nodes(ComponentModel model, String prefix) {
        Map<String, String> nodes = new LinkedHashMap<>();
        model.pins().forEach(pin -> nodes.put(pin.name(), subcircuitNode(pin.shared() ? pin.name() : prefix + pin.name())));
        model.nodes().forEach(node -> nodes.put(node, subcircuitNode(prefix + node)));
        model.states().forEach(state -> nodes.put(state.name(), subcircuitNode(prefix + state.name())));
        return nodes;
    }

    /** The subcircuit node a name of the model is: a pin, an inner node or a state; ground is 0. */
    private static String node(Map<String, String> nodes, String name) {
        return nodes.getOrDefault(name, name);
    }

    /**
     * How a name in an equation is written in a B source: a node's voltage {@code V(node)}, a parameter in braces, the
     * time and the temperature as ngspice names them.
     */
    private static String behaviouralName(String name, Map<String, String> nodes, Scope scope) {
        return behaviouralName(name, nodes, scope, Function.identity());
    }

    /** @param parameterName how a parameter is named where the expression stands: a unit reads its own ({@code A_gain}) */
    private static String behaviouralName(String name, Map<String, String> nodes, Scope scope,
                                          Function<String, String> parameterName) {
        if (nodes.containsKey(name)) {
            return "V(" + nodes.get(name) + ")";
        }
        if (scope.parameters().contains(name)) {
            return "{" + parameterName.apply(name) + "}";
        }
        // The bench's other figures as the nominal bench a deck runs on sets them: ideal, every part at its value,
        // standard detail. The temperature is ngspice's own, below.
        Double figure = ComponentVocabulary.nominalBench().get(name);
        if (figure != null && !name.equals(ComponentVocabulary.TEMPERATURE)) {
            return SpiceNumbers.write(figure);
        }
        if (name.startsWith(PIN_CURRENT)) {
            throw new UntranslatableFormula("the current into pin '" + name.substring(PIN_CURRENT.length())
                    + "' is not something a B source can read");
        }
        return switch (name) {
            case "time" -> "time";
            case "temperature" -> "(temper + " + TemperatureScale.ZERO_CELSIUS + ")";
            default -> throw new UntranslatableFormula("'" + name + "' is neither a node, a parameter nor "
                    + "the time");
        };
    }

    // ─── a component on its own ─────────────────────────────────────────────────────────────────────

    private SpiceDeck component(ExperienceDocument document) {
        ComponentModel model = ComponentDocuments.model(document, library);
        Optional<SpiceDevice> device = deviceOf(document);
        Set<String> pinNames = new LinkedHashSet<>();
        model.pins().forEach(pin -> pinNames.add(pin.name()));
        Trivia origin = Trivia.comment(document.title() + " — written from jMX component " + document.slug());
        if (device.isPresent() && pinNames.containsAll(device.get().pins())) {
            Map<String, Formula> values = parameterValues(model, Map.of());
            List<SpiceParameter> parameters = new ArrayList<>();
            for (SpiceDevice.ParameterName parameter : device.get().parameters()) {
                Formula value = values.get(parameter.jmx());
                if (value != null) {
                    parameters.add(SpiceParameter.of(parameter.spice(), SpiceValue.number(parameter.toSpice(
                            SpiceAnalyzer.evaluate(value, Map.of())))));
                }
            }
            SpiceCard.Model card = new SpiceCard.Model(spiceName(document.slug()), device.get().modelType().name(),
                    parameters, SpicePosition.NONE);
            card.addLeadingTrivia(List.of(origin, Trivia.comment("connected as " + device.get().modelType() + " "
                    + String.join(" ", device.get().pins()))));
            List<String> unmapped = model.parameters().stream().map(ComponentModel.Parameter::name)
                    .filter(name -> device.get().byJmxName(name).isEmpty()).toList();
            notes.add(new SpiceNote(document.line(), document.slug() + " is written as SPICE's own "
                    + device.get().modelType() + " model; its jMX equations are not carried"
                    + (unmapped.isEmpty() ? "" : ", and " + String.join(", ", unmapped) + " have no model parameter")));
            return SpiceDeck.of(null, List.of(card));
        }
        Optional<String> subcircuit = subcircuit(document.slug(), document, model);
        if (subcircuit.isEmpty()) {
            throw new SpiceParseException(SpicePosition.ofLine(document.line()), notes.isEmpty()
                    ? document.slug() + " cannot be written as SPICE" : notes.getLast().text());
        }
        List<SpiceStatement> statements = new ArrayList<>(definitions.values());
        statements.getFirst().addLeadingTrivia(List.of(origin));
        return SpiceDeck.of(null, statements);
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

    private static Formula formula(String text, int line) {
        try {
            return FormulaLanguage.standard().parse(text);
        } catch (RuntimeException unreadable) {
            throw new SpiceParseException(SpicePosition.ofLine(line), "'" + text + "' does not read as jMF: "
                    + unreadable.getMessage());
        }
    }

    private static double seconds(SourceText text) {
        String written = text.text();
        if (Quantity.isQuantity(written)) {
            return Quantity.valueOf(written);
        }
        throw new SpiceParseException(new SpicePosition(text.line(), text.column()), "the transient's span is a time: '"
                + written + "'");
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
