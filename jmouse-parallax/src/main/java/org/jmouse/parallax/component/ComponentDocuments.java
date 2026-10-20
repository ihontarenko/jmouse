package org.jmouse.parallax.component;

import org.jmouse.parallax.script.Amount;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.el.type.ValueType;
import org.jmouse.parallax.script.Quantity;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.component.ComponentModel.Criterion;
import org.jmouse.parallax.component.ComponentModel.Equation;
import org.jmouse.parallax.component.ComponentModel.EquationKind;
import org.jmouse.parallax.component.ComponentModel.Expectation;
import org.jmouse.parallax.component.ComponentModel.Parameter;
import org.jmouse.parallax.component.ComponentModel.Pin;
import org.jmouse.parallax.component.ComponentModel.PinNature;
import org.jmouse.parallax.component.ComponentModel.State;
import org.jmouse.parallax.component.ComponentModel.SubcircuitLine;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@code component} or {@code family} jMX document turned into the {@link ComponentModel} the solver runs.
 *
 * <p>Everything the document names is resolved here, through the product's {@link ComponentLibrary}: the function
 * libraries its equations call arrive as definitions, so the solver never looks anything up by name.</p>
 *
 * <h2>⚠️ No behaviour is Java's</h2>
 *
 * <p>Every part's physics is its document's equations — a resistor's and a capacitor's too, written in
 * {@code resistor.jmx} and {@code capacitor.jmx}. A part written on a sheet in pin order ({@code R1 resistor (a, b)}) is
 * bound to its document's model by the netlist. The engine's own devices are only what it adds itself: a node's leak to
 * ground, a failed part's stand-in, a logic pin's capacitance, an ideal source a generator's equation drives.</p>
 */
public final class ComponentDocuments {

    /**
     * The shared document whose {@code heat} law a part warms by when it gives a {@code thermal_resistance} and neither
     * writes, uses nor inherits a law of its own — the one place the common law of a part's heat is written.
     */
    public static final String SHARED_HEAT = "heat";

    /** The parameter that says a part warms at all: kelvin above the bench per watt. */
    public static final String THERMAL_RESISTANCE = "thermal_resistance";

    /** How many built models are kept: a catalogue's worth, and the drafts of the ones being written. */
    private static final int KEPT_MODELS = 512;

    /**
     * Every model built so far, by the text it was built from, with the texts of every document it read through its
     * library on the way — its base, its families, its function libraries, its heat law. A model is a function of those
     * texts alone, so it is built again only when one of them changed: one slider move used to build every part's model
     * five times over, each time reading and checking every document it uses.
     */
    private static final Map<BuiltKey, Built> BUILT = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BuiltKey, Built> eldest) {
            return size() > KEPT_MODELS;
        }
    });

    private record BuiltKey(String documentName, String source) {
    }

    /** A model, and what each document it read through its library held then — empty for one the library did not hold. */
    private record Built(ComponentModel model, Map<String, Optional<String>> read) {

        /** Whether every document it read still reads the same through {@code library}. */
        boolean currentIn(ComponentLibrary library) {
            for (Map.Entry<String, Optional<String>> document : read.entrySet()) {
                if (!document.getValue().equals(library.source(document.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }

    /** A library that remembers what it was asked and what it answered. */
    private record RecordingLibrary(ComponentLibrary library, Map<String, Optional<String>> read) implements ComponentLibrary {

        RecordingLibrary(ComponentLibrary library) {
            this(library, new LinkedHashMap<>());
        }

        @Override
        public Optional<String> source(String reference) {
            return read.computeIfAbsent(reference, library::source);
        }
    }

    private ComponentDocuments() {
    }

    /**
     * Reads one component or family document and builds its model, resolving what it includes through the library;
     * built once for its text and the texts it uses ({@link #BUILT}).
     */
    public static ComponentModel read(String source, String documentName, ComponentLibrary library) {
        BuiltKey key = new BuiltKey(documentName, source);
        Built known = BUILT.get(key);
        if (known != null && known.currentIn(library)) {
            return known.model();
        }
        RecordingLibrary recording = new RecordingLibrary(library);
        ComponentModel model = model(ExperienceReader.readOne(source, documentName), recording);
        BUILT.put(key, new Built(model, Map.copyOf(recording.read())));
        return model;
    }

    /**
     * The model of a document. A component whose {@code family} is another component is a member of it — a datasheet
     * part of a general device: {@code component "1N4148" … family diode} writes its numbers, package and checks, and
     * takes the diode's physics ({@link ComponentModel#memberOf}). A {@code family} document is a logic family instead,
     * read where a digital pin needs it.
     */
    public static ComponentModel model(ExperienceDocument document, ComponentLibrary library) {
        ComponentModel written = written(document, library);
        ComponentModel model = baseComponent(document, library).map(base -> written.memberOf(base, isModelled(document)))
                .orElse(written);
        if (model.heat() == null && document.kind() == Kind.COMPONENT && model.parameter(THERMAL_RESISTANCE).isPresent()) {
            model = model.withHeat(sharedHeat(document, library));
        }
        DimensionCheck.require(model);
        return model;
    }

    /** The library's common heat law, for a part that warms and writes no law of its own. */
    private static ComponentModel.Heat sharedHeat(ExperienceDocument document, ComponentLibrary library) {
        return library.source(SHARED_HEAT).map(source -> ExperienceReader.readOne(source, SHARED_HEAT))
                .map(shared -> heatOf(shared, library))
                .orElseThrow(() -> new IllegalArgumentException(document.title() + " gives a thermal_resistance, writes no "
                        + "heat law of its own, and the library holds no shared '" + SHARED_HEAT + "' law to warm it by."));
    }

    /**
     * The heat law a document writes ({@code heat { … }}), or the one of the first functions document it uses that
     * writes one; null for neither.
     */
    private static ComponentModel.Heat heatOf(ExperienceDocument document, ComponentLibrary library) {
        Optional<Block.Heat> written = document.block(Block.Heat.class);
        if (written.isPresent()) {
            Map<String, String> rates = new LinkedHashMap<>();
            Map<String, String> locals = new LinkedHashMap<>();
            for (Block.Equation line : written.get().lines()) {
                (line.form() == Block.EquationForm.DERIVATIVE ? rates : locals).put(line.subject(), line.expression().text());
            }
            return new ComponentModel.Heat(document.title(), rates, locals, written.get().temperature().text(),
                    formulas(document, library));
        }
        for (String reference : document.uses()) {
            ComponentModel.Heat used = library.source(reference).map(source -> ExperienceReader.readOne(source, reference))
                    .filter(candidate -> candidate.kind() == Kind.FUNCTIONS && candidate.block(Block.Heat.class).isPresent())
                    .map(candidate -> heatOf(candidate, library)).orElse(null);
            if (used != null) {
                return used;
            }
        }
        return null;
    }

    /** The component this one extends, when it names one — the base whose physics it specializes. */
    private static Optional<ComponentModel> baseComponent(ExperienceDocument document, ComponentLibrary library) {
        if (document.kind() != Kind.COMPONENT || document.base().isEmpty()) {
            return Optional.empty();
        }
        String name = document.base().get();
        if (name.equals(document.slug())) {
            throw new IllegalArgumentException(document.title() + " extends itself");
        }
        // A base that is not there is refused, never read past: a part without its base's physics is no part at all,
        // and nothing would say so — an LM358 whose op-amp was missing ran with an output stuck at nought.
        ExperienceDocument base = library.source(name).map(source -> ExperienceReader.readOne(source, name))
                .orElseThrow(() -> new IllegalArgumentException(document.title() + " extends '" + name
                        + "', which the library does not hold."));
        if (base.kind() != Kind.COMPONENT) {
            throw new IllegalArgumentException(document.title() + " extends '" + name + "', which is a "
                    + base.kind().keyword() + ", not a component.");
        }
        return Optional.of(model(base, library));
    }

    private static ComponentModel written(ExperienceDocument document, ComponentLibrary library) {
        if (document.kind() != Kind.COMPONENT && document.kind() != Kind.FAMILY) {
            throw new IllegalArgumentException("'" + document.title() + "' is a " + document.kind().keyword()
                    + ", not a component or a family");
        }
        List<String> units = document.units();
        List<Parameter> parameters = parameters(document);
        Map<String, Double> values = new LinkedHashMap<>();
        parameters.forEach(parameter -> values.put(parameter.name(), parameter.value()));
        List<Pin> pins = document.block(Block.Pins.class).map(block -> block.pins().stream()
                .map(pin -> pin(pin, units, document)).toList()).orElse(List.of());
        List<String> nodes = new ArrayList<>();
        List<String> digitalNodes = new ArrayList<>();
        document.block(Block.Nodes.class).ifPresent(block -> block.nodes()
                .forEach(node -> (node.digital() ? digitalNodes : nodes).add(node.name())));
        List<State> states = document.block(Block.State.class).map(block -> block.variables().stream()
                .map(variable -> new State(variable.name(), initial(variable.initial().text(), document),
                        variable.discrete(), variable.description())).toList()).orElse(List.of());
        Map<String, String> locals = new LinkedHashMap<>();
        Map<String, ValueType> localTypes = new LinkedHashMap<>();
        List<Equation> equations = new ArrayList<>();
        document.block(Block.Equations.class).ifPresent(block -> block.equations().forEach(equation -> {
            switch (equation.form()) {
                case LOCAL -> {
                    locals.put(equation.subject(), equation.expression().text());
                    if (equation.type() != null) {
                        localTypes.put(equation.subject(), equation.type());
                    }
                }
                case CURRENT, VOLTAGE -> equations.add(new Equation(equation.form() == Block.EquationForm.CURRENT
                        ? EquationKind.CURRENT : EquationKind.VOLTAGE, equation.positive(), equation.negative(), null,
                        equation.expression().text(), equation.expression().line()));
                case DERIVATIVE -> equations.add(new Equation(EquationKind.STATE, null, null, equation.subject(),
                        equation.expression().text(), equation.expression().line()));
                default -> throw new IllegalArgumentException(document.title() + ", line " + equation.expression().line()
                        + ": a component's equations are current a -> b = …, voltage a -> b = …, d(x)/dt = … and local name = …");
            }
        }));
        return new ComponentModel(document.title(), document.kind() == Kind.FAMILY, document.type().orElse(""),
                header(document, library), units, parameters, pins, nodes, digitalNodes, symbol(document),
                subcircuit(document, pins), formulas(document, library), states, locals, localTypes, equations,
                document.block(Block.Behaviour.class).map(Block.Behaviour::script).orElse(null),
                expectations(document, values), displays(document), measures(document), leads(document, pins),
                document.block(Block.Display.class).map(Block.Display::units).orElse(Map.of()),
                document.block(Block.Display.class).map(Block.Display::captions).orElse(Map.of()), heatOf(document, library));
    }

    /** How each pin's lead looks, as written; a pin the component does not have is refused. */
    private static Map<String, ComponentModel.LeadLook> leads(ExperienceDocument document, List<Pin> pins) {
        Map<String, ComponentModel.LeadLook> leads = new LinkedHashMap<>();
        document.block(Block.Leads.class).ifPresent(block -> block.looks().forEach((pin, look) -> {
            if (pins.stream().noneMatch(candidate -> candidate.name().equals(pin))) {
                throw new IllegalArgumentException(document.title() + ": '" + pin + "' in its leads is not one of its pins.");
            }
            leads.put(pin, new ComponentModel.LeadLook(look.colour(), look.stripes()));
        }));
        return leads;
    }

    /** What an instrument reads, by name, as written; nothing for a component that is no instrument. */
    private static Map<String, ComponentModel.Measure> measures(ExperienceDocument document) {
        Map<String, ComponentModel.Measure> measures = new LinkedHashMap<>();
        document.block(Block.Measure.class).ifPresent(measure -> measure.figures().forEach((name, figure) ->
                measures.put(name, new ComponentModel.Measure(figure.expression().text(), figure.unit(), figure.signal()))));
        return measures;
    }

    /**
     * What the component shows on its symbol while it runs, by name, as written: its {@code display} figures, and what
     * its {@code symbol uses} line binds each input of its symbols to — both read the same way, so a symbol input and an
     * indicator are fed alike, and one name cannot be both.
     */
    private static Map<String, String> displays(ExperienceDocument document) {
        Map<String, String> displays = new LinkedHashMap<>();
        document.block(Block.Display.class).ifPresent(display -> display.figures().forEach((name, expression) -> displays.put(name, expression.text())));
        document.block(Block.Symbol.class).ifPresent(symbol -> symbol.bindings().forEach((input, expression) -> {
            if (displays.putIfAbsent(input, expression.text()) != null) {
                throw new IllegalArgumentException("'" + input + "' is both displayed and bound to a symbol input: "
                        + "a symbol input of that name reads the display figure without a binding");
            }
        }));
        return displays;
    }

    // ─── header and parameters ──────────────────────────────────────────────────────────────────────

    private static Map<String, List<String>> header(ExperienceDocument document, ComponentLibrary library) {
        Map<String, List<String>> header = new LinkedHashMap<>();
        Meta meta = document.meta();
        Optional.ofNullable(meta.summary()).ifPresent(summary -> header.put("summary", List.of(summary)));
        Optional.ofNullable(meta.source()).ifPresent(source -> header.put("source", List.of(source)));
        meta.field("package").filter(Meta.MetaValue.Text.class::isInstance)
                .map(value -> ((Meta.MetaValue.Text) value).text()).ifPresent(text -> header.put("package", List.of(text)));
        // How the part fails when it overheats on a realistic bench: `failure open`, `failure short`, or the pins a
        // short ties, `failure short drain source`.
        meta.field("failure").map(ComponentDocuments::words).filter(words -> !words.isEmpty()).ifPresent(words -> {
            // Refused here, as it is written, rather than when the part first fails.
            BenchConditions.Failure.of(words);
            header.put("failure", List.copyOf(words));
        });
        // What a part of it is called on a sheet before its number: `designator R` names R1, R2 …
        meta.field("designator").map(ComponentDocuments::firstWord).ifPresent(designator -> {
            if (!designator.matches("[A-Z]{1,3}")) {
                throw new IllegalArgumentException("A designator is one to three capital letters, as R, C or VM — '"
                        + designator + "' is not one.");
            }
            header.put("designator", List.of(designator));
        });
        // The logic family its digital pins follow is the `use` reference that names a family document.
        usedFamily(document, library).ifPresent(family -> header.put("family", List.of(family)));
        return header;
    }

    /** The logic family a document uses — the one `use` reference whose target is a {@code family} document. */
    private static Optional<String> usedFamily(ExperienceDocument document, ComponentLibrary library) {
        return document.uses().stream().filter(reference -> usedKind(reference, library) == Kind.FAMILY).findFirst();
    }

    /** The kind of a used document, or {@code null} when the library does not hold it. */
    private static Kind usedKind(String reference, ComponentLibrary library) {
        return library.source(reference).map(source -> ExperienceReader.readOne(source, reference).kind()).orElse(null);
    }

    /** A header field's words: {@code failure short drain source} is three; a text is one; anything else none. */
    private static List<String> words(Meta.MetaValue value) {
        return switch (value) {
            case Meta.MetaValue.Text text -> text.text().isBlank() ? List.of() : List.of(text.text().strip().split("\\s+"));
            case Meta.MetaValue.Names names -> names.names();
            default -> List.of();
        };
    }

    /** A header field written as a word or a text: {@code failure open}, {@code designator "R"}; empty for anything else. */
    private static String firstWord(Meta.MetaValue value) {
        return switch (value) {
            case Meta.MetaValue.Text text -> text.text();
            case Meta.MetaValue.Names names -> names.names().isEmpty() ? "" : names.names().getFirst();
            default -> "";
        };
    }

    /** Each parameter in SI; an expression is computed from the parameters written before it. */
    private static List<Parameter> parameters(ExperienceDocument document) {
        List<Parameter> parameters = new ArrayList<>();
        Map<String, Double> known = new LinkedHashMap<>();
        for (Block.Parameter parameter : document.parameters()) {
            if (parameter.name().equals(ComponentVocabulary.REALISTIC)) {
                throw new IllegalArgumentException("'realistic' is the bench's — 1 on a realistic bench, 0 on an ideal one —, "
                        + "given to every component's equations; it cannot be a parameter.");
            }
            if (parameter.name().equals(ComponentVocabulary.TEMPERATURE)) {
                throw new IllegalArgumentException("'temperature' is the bench's, in kelvin, given to every component's "
                        + "equations — it cannot be a parameter. Name the temperature a figure is measured at T_nominal, "
                        + "and write the drift against it: I_saturation * (temperature / T_nominal)^3.");
            }
            if (ComponentVocabulary.isReserved(parameter.name())) {
                throw new IllegalArgumentException("'" + parameter.name() + "' is a name the engine gives every component's "
                        + "equations; it cannot be a parameter.");
            }
            double value = parameter.quantity().map(Quantity::value)
                    .orElseGet(() -> evaluate(parameter.value().text(), known, document, parameter.value().line()));
            known.put(parameter.name(), value);
            parameters.add(new Parameter(parameter.name(), value, parameter.has(Block.ParameterFlag.PER_UNIT),
                    parameter.description(), parameter.quantity().map(Quantity::unit).orElse("")));
        }
        return parameters;
    }

    private static double evaluate(String expression, Map<String, Double> values, ExperienceDocument document, int line) {
        try {
            return FormulaLanguage.standard().evaluate(expression, values);
        } catch (RuntimeException refused) {
            throw new IllegalArgumentException(document.title() + ", line " + line + ": '" + expression
                    + "' cannot be computed — " + refused.getMessage(), refused);
        }
    }

    // ─── pins, states, symbol ───────────────────────────────────────────────────────────────────────

    /** A pin's package numbers resolved against the declared units: one for all, or one per unit. */
    private static Pin pin(Block.Pin pin, List<String> units, ExperienceDocument document) {
        List<Integer> numbers = pin.numbers();
        Map<String, Integer> byUnit = new LinkedHashMap<>();
        boolean shared = units.isEmpty() || numbers.size() <= 1;
        if (shared) {
            byUnit.put("", numbers.isEmpty() ? 0 : numbers.getFirst());
        } else if (numbers.size() == units.size()) {
            for (int unit = 0; unit < units.size(); unit++) {
                byUnit.put(units.get(unit), numbers.get(unit));
            }
        } else {
            throw new IllegalArgumentException(document.title() + ": pin '" + pin.name() + "' needs one number for each unit ("
                    + String.join(", ", units) + ") or one for all");
        }
        return new Pin(pin.name(), byUnit, shared, PinNature.valueOf(pin.nature().name()), pin.output(),
                pin.attributes(), pin.description());
    }

    /** A number, {@code X}, {@code Z}, or a list of them: what a state starts as. */
    private static Object initial(String text, ExperienceDocument document) {
        String written = text.strip();
        if (written.startsWith("[") && written.endsWith("]")) {
            return Arrays.stream(written.substring(1, written.length() - 1).split(","))
                    .map(item -> initial(item, document)).toArray();
        }
        if (written.equals("X") || written.equals("Z")) {
            return written;
        }
        double value;
        try {
            value = Double.parseDouble(written);
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException(document.title() + ": '" + written + "' is not an initial value");
        }
        return value == Math.rint(value) && Math.abs(value) < 1e9 ? (Object) (int) value : (Object) value;
    }

    /**
     * A parametric symbol in the lines a drawing reads: its shape, then its sides ({@code box},
     * {@code left trig, thr, ctrl}). A part drawn from a symbol document ({@code symbol uses …}) has none: its drawing
     * is that document's.
     */
    private static List<String> symbol(ExperienceDocument document) {
        List<String> lines = new ArrayList<>();
        document.block(Block.Symbol.class).ifPresent(symbol -> symbol.shape().ifPresent(shape -> {
            lines.add(shape);
            symbol.sides().forEach((side, entries) -> lines.add(side + " " + String.join(", ", entries)));
        }));
        return lines;
    }

    // ─── internal netlist ───────────────────────────────────────────────────────────────────────────

    /**
     * A primitive's one device, or a component's internal parts. A primitive's value is its first parameter: the
     * resistance of a resistor, the output voltage of a source.
     */
    private static List<SubcircuitLine> subcircuit(ExperienceDocument document, List<Pin> pins) {
        CircuitElementType primitive = primitiveOf(document);
        if (primitive != null) {
            if (pins.size() != 2 || document.parameters().isEmpty()) {
                throw new IllegalArgumentException(document.title() + ": a " + document.type().orElseThrow()
                        + " has two pins and its value as its first parameter");
            }
            return List.of(new SubcircuitLine(primitive.primitiveComponent(), primitive, null, pins.get(0).name(), pins.get(1).name(),
                    document.parameters().getFirst().name(), Map.of()));
        }
        return document.block(Block.Parts.class).map(parts -> parts.parts().stream()
                .map(part -> line(part, document)).toList()).orElse(List.of());
    }

    /** The solver's device for a document that is a primitive, or {@code null} for a part with behaviour of its own. */
    public static CircuitElementType primitiveOf(ExperienceDocument document) {
        boolean member = document.kind() == Kind.COMPONENT && document.base().isPresent();
        return isModelled(document) || member ? null : document.type().flatMap(CircuitElementType::ofComponent).orElse(null);
    }

    /** Whether a document writes behaviour of its own: equations, events or parts. */
    private static boolean isModelled(ExperienceDocument document) {
        return document.block(Block.Equations.class).isPresent() || document.block(Block.Behaviour.class).isPresent()
                || document.block(Block.Parts.class).isPresent();
    }

    /** As in a circuit: a part connected in pin order is a primitive, one connected by pin name is its component. */
    private static SubcircuitLine line(Block.Part part, ExperienceDocument document) {
        CircuitElementType primitive = part.named().isEmpty() ? CircuitElementType.ofComponent(part.reference()).orElse(null) : null;
        if (primitive == null) {
            return new SubcircuitLine(part.id(), CircuitElementType.MODEL, part.reference(), null, null, null, part.named());
        }
        if (part.positional().size() != 2) {
            throw new IllegalArgumentException(document.title() + ", line " + part.line() + ": a " + part.reference()
                    + " is connected by its two pins in order: (a, b)");
        }
        // Its value is its first setting, as a primitive's is its component's first parameter.
        String value = part.settings().values().stream().map(setting -> setting.text().replace(" ", "")).findFirst().orElse(null);
        return new SubcircuitLine(part.id(), primitive, null, part.positional().get(0), part.positional().get(1),
                value, Map.of());
    }

    // ─── formulas and expectations ──────────────────────────────────────────────────────────────────

    /**
     * The definitions the equations may call: those of every function library the document uses — and of the libraries
     * those use in turn, first, each once —, then the document's own {@code formulas}.
     */
    private static List<String> formulas(ExperienceDocument document, ComponentLibrary library) {
        List<String> definitions = new ArrayList<>();
        usedFormulas(document, library, new HashSet<>(), definitions);
        document.block(Block.Formulas.class).flatMap(Block.Formulas::definitions)
                .ifPresent(text -> definitions.add(text.text()));
        return definitions;
    }

    /** Every function library a document uses, depth first, so a definition comes after the ones it calls. */
    private static void usedFormulas(ExperienceDocument document, ComponentLibrary library, Set<String> seen,
                                     List<String> into) {
        for (String reference : document.uses()) {
            if (!seen.add(reference)) {
                continue;
            }
            library.source(reference).map(source -> ExperienceReader.readOne(source, reference))
                    .filter(used -> used.kind() == Kind.FUNCTIONS)
                    .ifPresent(used -> {
                        usedFormulas(used, library, seen, into);
                        used.block(Block.Formulas.class).flatMap(Block.Formulas::definitions).ifPresent(text -> into.add(text.text()));
                    });
        }
    }

    private static List<Expectation> expectations(ExperienceDocument document, Map<String, Double> values) {
        return document.blocks(Block.Expectation.class).stream().map(expectation -> new Expectation(expectation.title(),
                expectation.reference(), expectation.bench(), expectation.duration(), expectation.temperature(),
                expectation.realistic(), expectation.measure(),
                criterion(expectation.criterion(), values, document), expectation.line())).toList();
    }

    /** The criterion as numbers: literals as written, expressions computed from the component's parameters. */
    private static Criterion criterion(Block.Criterion criterion, Map<String, Double> values, ExperienceDocument document) {
        if (criterion.kind() == Block.CriterionKind.IS) {
            return new Criterion(Criterion.Kind.IS, 0, 0, criterion.exact());
        }
        double[] bounds = criterion.bounds(amount -> value(amount, values, document));
        return new Criterion(criterion.kind() == Block.CriterionKind.NEAR ? Criterion.Kind.NEAR : Criterion.Kind.WITHIN,
                bounds[0], bounds[1], null);
    }

    private static double value(Amount amount, Map<String, Double> values, ExperienceDocument document) {
        return amount.quantity().map(Quantity::value)
                .orElseGet(() -> evaluate(amount.expression().text(), values, document, amount.expression().line()));
    }
}
