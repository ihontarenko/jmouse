package org.jmouse.parallax.circuit;

import org.jmouse.parallax.script.Amount;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.el.type.ValueType;
import org.jmouse.parallax.script.Quantity;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.circuit.ComponentModel.Criterion;
import org.jmouse.parallax.circuit.ComponentModel.Equation;
import org.jmouse.parallax.circuit.ComponentModel.EquationKind;
import org.jmouse.parallax.circuit.ComponentModel.Expectation;
import org.jmouse.parallax.circuit.ComponentModel.Parameter;
import org.jmouse.parallax.circuit.ComponentModel.Pin;
import org.jmouse.parallax.circuit.ComponentModel.PinNature;
import org.jmouse.parallax.circuit.ComponentModel.State;
import org.jmouse.parallax.circuit.ComponentModel.SubcircuitLine;
import org.jmouse.parallax.object.ComponentLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A {@code component} or {@code family} jMX document turned into the {@link ComponentModel} the solver runs.
 *
 * <p>Everything the document names is resolved here, through the product's {@link ComponentLibrary}: the function
 * libraries its equations call arrive as definitions, so the solver never looks anything up by name.</p>
 *
 * <h2>⚠️ The engine's primitives</h2>
 *
 * <p>A component whose {@code type} is one of {@link #PRIMITIVES} <em>and</em> that describes no behaviour of its own —
 * no equations, no events, no parts — is a primitive: the solver supplies its device, with exact stamps and exact
 * source edges, and the document carries the parameters, pins and drawing. A modelled part keeps its role in
 * {@code type} without becoming one: the 1N4148 is {@code type diode} and is solved from its own equations. This is the
 * one place a component's behaviour is still Java's; it goes when the solver stamps any component from its equations.</p>
 */
public final class ComponentDocuments {

    /** The devices the solver supplies, by a component's {@code type}, and the letter its netlist line carries. */
    public static final Map<String, Character> PRIMITIVES = Map.of(
            "resistor", 'R', "capacitor", 'C', "inductor", 'L', "diode", 'D',
            "voltage-source", 'V', "current-source", 'I');

    private ComponentDocuments() {
    }

    /** Reads one component or family document and builds its model, resolving what it includes through the library. */
    public static ComponentModel read(String source, String documentName, ComponentLibrary library) {
        return model(ExperienceReader.readOne(source, documentName), library);
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
        DimensionCheck.require(model);
        return model;
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
        return library.source(name).map(source -> ExperienceReader.readOne(source, name))
                .filter(base -> base.kind() == Kind.COMPONENT).map(base -> model(base, library));
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
                document.block(Block.Display.class).map(Block.Display::captions).orElse(Map.of()));
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

    /** What the component shows on its symbol while it runs, by indicator name, as written. */
    private static Map<String, String> displays(ExperienceDocument document) {
        Map<String, String> displays = new LinkedHashMap<>();
        document.block(Block.Display.class).ifPresent(display -> display.figures().forEach((name, expression) -> displays.put(name, expression.text())));
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
        // How the part fails when it overheats on a realistic bench: `failure open` or `failure short`.
        meta.field("failure").map(ComponentDocuments::firstWord).filter(failure -> failure.equals("open") || failure.equals("short")).ifPresent(failure -> header.put("failure", List.of(failure)));
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
            if (parameter.name().equals(Netlist.REALISTIC)) {
                throw new IllegalArgumentException("'realistic' is the bench's — 1 on a realistic bench, 0 on an ideal one —, "
                        + "given to every component's equations; it cannot be a parameter.");
            }
            if (parameter.name().equals(Netlist.TEMPERATURE)) {
                throw new IllegalArgumentException("'temperature' is the bench's, in kelvin, given to every component's "
                        + "equations — it cannot be a parameter. Name the temperature a figure is measured at T_nominal, "
                        + "and write the drift against it: I_saturation * (temperature / T_nominal)^3.");
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
        Character primitive = primitiveOf(document);
        if (primitive != null) {
            if (pins.size() != 2 || document.parameters().isEmpty()) {
                throw new IllegalArgumentException(document.title() + ": a " + document.type().orElseThrow()
                        + " has two pins and its value as its first parameter");
            }
            return List.of(new SubcircuitLine(primitive + "1", primitive, null, pins.get(0).name(), pins.get(1).name(),
                    isSource(primitive) ? "DC" : null, document.parameters().getFirst().name(), null, Map.of()));
        }
        return document.block(Block.Parts.class).map(parts -> parts.parts().stream()
                .map(part -> line(part, document)).toList()).orElse(List.of());
    }

    private static boolean isSource(char primitive) {
        return primitive == 'V' || primitive == 'I';
    }

    /** The solver's device for a document that is a primitive, or {@code null} for a part with behaviour of its own. */
    public static Character primitiveOf(ExperienceDocument document) {
        boolean member = document.kind() == Kind.COMPONENT && document.base().isPresent();
        return isModelled(document) || member ? null : document.type().map(PRIMITIVES::get).orElse(null);
    }

    /** Whether a document writes behaviour of its own: equations, events or parts. */
    private static boolean isModelled(ExperienceDocument document) {
        return document.block(Block.Equations.class).isPresent() || document.block(Block.Behaviour.class).isPresent()
                || document.block(Block.Parts.class).isPresent();
    }

    /** As in a circuit: a part connected in pin order is a primitive, one connected by pin name is its component. */
    private static SubcircuitLine line(Block.Part part, ExperienceDocument document) {
        Character primitive = part.named().isEmpty() ? PRIMITIVES.get(part.reference()) : null;
        if (primitive == null) {
            return new SubcircuitLine(part.id(), 'U', part.reference(), null, null, null, null, null, part.named());
        }
        if (part.positional().size() != 2) {
            throw new IllegalArgumentException(document.title() + ", line " + part.line() + ": a " + part.reference()
                    + " is connected by its two pins in order: (a, b)");
        }
        String waveform = null;
        if (isSource(primitive)) {
            var written = part.settings().get("waveform");
            waveform = written == null ? "DC" : written.text().toUpperCase(Locale.ROOT);
        }
        String value = part.settings().entrySet().stream()
                .filter(setting -> !setting.getKey().equals("waveform") && !setting.getKey().equals("frequency"))
                .map(setting -> setting.getValue().text().replace(" ", "")).findFirst().orElse(null);
        String frequency = part.settings().containsKey("frequency") ? part.settings().get("frequency").text().replace(" ", "") : null;
        return new SubcircuitLine(part.id(), primitive, null, part.positional().get(0), part.positional().get(1),
                waveform, value, frequency, Map.of());
    }

    // ─── formulas and expectations ──────────────────────────────────────────────────────────────────

    /** The definitions the equations may call: the document's own, and those of every function library it includes. */
    private static List<String> formulas(ExperienceDocument document, ComponentLibrary library) {
        List<String> definitions = new ArrayList<>();
        // A used function library's definitions come first, then the document's own `formulas { … }` block.
        for (String reference : document.uses()) {
            library.source(reference).map(source -> ExperienceReader.readOne(source, reference))
                    .filter(used -> used.kind() == Kind.FUNCTIONS)
                    .flatMap(used -> used.block(Block.Formulas.class).flatMap(Block.Formulas::definitions))
                    .ifPresent(text -> definitions.add(text.text()));
        }
        document.block(Block.Formulas.class).flatMap(Block.Formulas::definitions)
                .ifPresent(text -> definitions.add(text.text()));
        return definitions;
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
