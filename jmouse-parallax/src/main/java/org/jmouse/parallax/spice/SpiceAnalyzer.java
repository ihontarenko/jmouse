package org.jmouse.parallax.spice;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.Symbols;
import org.jmouse.parallax.spice.SpiceNetlist.Element;
import org.jmouse.parallax.spice.SpiceNetlist.ModelType;
import org.jmouse.parallax.spice.SpiceNetlist.Parameter;
import org.jmouse.parallax.spice.SpiceNetlist.PassiveKind;
import org.jmouse.spice.SpiceNumbers;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.expression.SpiceExpressionLanguage;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceElement;
import org.jmouse.spice.node.SpiceParameter;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.spice.node.SpiceWaveform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Reads what a {@link SpiceDeck} means for jMX into a {@link SpiceNetlist}: the elements both sides have, every value a
 * jMF formula over the parameters in scope, nodes in lower case with {@code gnd} as {@code 0}, models by their type.
 *
 * <p>A card jMX has nothing for — a controlled source, a switch, an {@code .ac} sweep, an {@code .include} — is named
 * in a note and left out. A value of a card it does have that does not read as jMF — a function jMF has not, a name
 * no {@code .param} declares — is refused with its line and column: converting the rest without it would change the
 * circuit without saying how.</p>
 */
final class SpiceAnalyzer {

    /** What each element letter jMX has no counterpart for is, so a note can say it in words. */
    private static final Map<Character, String> UNSUPPORTED_ELEMENTS = Map.ofEntries(
            Map.entry('E', "a voltage-controlled voltage source"), Map.entry('F', "a current-controlled current source"),
            Map.entry('G', "a voltage-controlled current source"), Map.entry('H', "a current-controlled voltage source"),
            Map.entry('B', "a behavioural source"), Map.entry('K', "a coupling of inductors"),
            Map.entry('T', "a transmission line"), Map.entry('O', "a lossy transmission line"),
            Map.entry('U', "a distributed RC line"), Map.entry('S', "a voltage-controlled switch"),
            Map.entry('W', "a current-controlled switch"), Map.entry('Z', "a MESFET"),
            Map.entry('A', "an XSPICE code model"), Map.entry('P', "a coupled multiconductor line"));

    /** Dot cards a jMX document has no place for, and why. */
    private static final Map<String, String> UNSUPPORTED_DIRECTIVES = Map.ofEntries(
            Map.entry("noise", "a noise analysis has no jMX counterpart"),
            Map.entry("tf", "a transfer-function analysis has no jMX counterpart"),
            Map.entry("four", "a Fourier analysis has no jMX counterpart"),
            Map.entry("temp", "the temperature is the bench's in jMX, not the circuit's"),
            Map.entry("ic", "initial conditions have no jMX counterpart: a circuit starts from rest"),
            Map.entry("nodeset", "a node set has no jMX counterpart"),
            Map.entry("func", "a user function has no jMX counterpart; write its expression where it is used"),
            Map.entry("global", "global nodes have no jMX counterpart"),
            Map.entry("options", "it sets up SPICE's own solver"), Map.entry("option", "it sets up SPICE's own solver"),
            Map.entry("print", "it sets up SPICE's own output"), Map.entry("plot", "it sets up SPICE's own output"),
            Map.entry("probe", "it sets up SPICE's own output"), Map.entry("save", "it sets up SPICE's own output"),
            Map.entry("measure", "a measurement has no jMX counterpart here"),
            Map.entry("meas", "a measurement has no jMX counterpart here"));

    private final List<SpiceNote>                       notes       = new ArrayList<>();
    private final Map<String, SpiceNetlist.Subcircuit> subcircuits = new LinkedHashMap<>();
    private final Map<String, SpiceNetlist.Model>      models      = new LinkedHashMap<>();

    private SpiceAnalyzer() {
    }

    static SpiceNetlist analyze(SpiceDeck deck) {
        SpiceAnalyzer analyzer = new SpiceAnalyzer();
        deck.title().ifPresent(analyzer::noteTitle);
        Scope scope = Scope.of(declaredParameters(deck.statements()));
        List<Element> elements = new ArrayList<>();
        List<Parameter> parameters = new ArrayList<>();
        SpiceCard.Transient firstTransient = null;
        for (SpiceStatement statement : deck.statements()) {
            if (statement instanceof SpiceCard.Transient written) {
                if (firstTransient != null) {
                    analyzer.note(written.position(), "a second '.tran' is left out; the first one is kept");
                } else {
                    firstTransient = written;
                }
                continue;
            }
            analyzer.statement(statement, scope, parameters).ifPresent(elements::add);
        }
        // Worked out once every '.param' is read: a '.tran' written above the parameters it names still reads them.
        Optional<SpiceNetlist.Transient> analysis = firstTransient == null ? Optional.empty()
                : Optional.of(analyzer.transientAnalysis(firstTransient, scope, parameters));
        if (deck.ignoredAfterEnd() > 0) {
            analyzer.notes.add(new SpiceNote(0, deck.ignoredAfterEnd() + " card(s) after '.end' are not read, as SPICE "
                    + "does not read them"));
        }
        return new SpiceNetlist(deck.title(), elements, analyzer.subcircuits, analyzer.models, parameters, analysis,
                analyzer.notes);
    }

    /** One statement: the element it is, or nothing — a card that declares, or one that is noted. */
    private Optional<Element> statement(SpiceStatement statement, Scope scope, List<Parameter> parameters) {
        switch (statement) {
            case SpiceElement element -> {
                return element(element, scope);
            }
            case SpiceCard.Parameters card -> card.parameters().forEach(parameter -> parameters.add(new Parameter(
                    parameter.name(), value(parameter.value(), scope, "parameter " + parameter.name()), parameter.position())));
            case SpiceCard.Model model -> model(model, scope);
            case SpiceCard.Subcircuit subcircuit -> subcircuit(subcircuit, scope);
            case SpiceCard.Include include -> note(include.position(), "'." + include.directive() + " " + include.path()
                    + "' is not read: no file of that name was given with the netlist");
            case SpiceCard.Transient written -> note(written.position(), "'.tran' inside a subcircuit is not SPICE's; "
                    + "left out");
            case SpiceCard.Ac written -> note(written.position(), "'.ac': an AC sweep is not converted; a jMX circuit "
                    + "computes its transient; left out");
            case SpiceCard.Dc written -> note(written.position(), "'.dc': a DC sweep is not converted; a jMX circuit "
                    + "computes its transient; left out");
            case SpiceCard.OperatingPoint written -> note(written.position(), "'.op': an operating point is not "
                    + "converted; a jMX circuit computes its transient; left out");
            case SpiceCard.Control control -> note(control.position(), "'.control' … '.endc' is a script for SPICE's "
                    + "own interpreter; left out");
            case SpiceCard.Directive directive -> note(directive.position(), "'." + directive.directive() + "': "
                    + UNSUPPORTED_DIRECTIVES.getOrDefault(directive.directive(), "it has no jMX counterpart") + "; left out");
            case SpiceCard.End end -> {
            }
        }
        return Optional.empty();
    }

    // ─── scope ──────────────────────────────────────────────────────────────────────────────────────

    /** The parameters an expression may name: lower-case name → the spelling it was declared with. */
    private record Scope(Map<String, String> names) {
        static Scope of(List<String> declared) {
            return new Scope(Map.of()).with(declared);
        }

        Scope with(List<String> declared) {
            Map<String, String> wider = new LinkedHashMap<>(names);
            declared.forEach(name -> wider.put(name.toLowerCase(Locale.ROOT), name));
            return new Scope(wider);
        }
    }

    /** The names the {@code .param} cards among these declare, wherever they stand: SPICE reads them all first. */
    private static List<String> declaredParameters(List<SpiceStatement> statements) {
        List<String> names = new ArrayList<>();
        statements.stream().filter(SpiceCard.Parameters.class::isInstance)
                .forEach(card -> ((SpiceCard.Parameters) card).parameters().forEach(parameter -> names.add(parameter.name())));
        return names;
    }

    // ─── values ─────────────────────────────────────────────────────────────────────────────────────

    /** A value as a jMF formula: a number, a declared parameter, or an expression read by jMF. */
    private Formula value(SpiceValue value, Scope scope, String what) {
        return switch (value) {
            case SpiceValue.Number number -> {
                if (SpiceNumbers.read(number.written()).map(SpiceNumbers.Engineering::isLoneCapitalM).orElse(false)) {
                    note(number.position(), what + " '" + number.written() + "' is read as SPICE reads it, a "
                            + "thousandth; mega is written 'meg'");
                }
                yield new Formula.Constant(number.value());
            }
            case SpiceValue.Reference reference -> {
                String declared = scope.names().get(reference.name().toLowerCase(Locale.ROOT));
                if (declared == null) {
                    throw new SpiceParseException(reference.position(), "'" + reference.name() + "' is not a value: "
                            + what + " is a number, a declared parameter, or an expression in braces");
                }
                yield new Formula.Variable(declared);
            }
            case SpiceValue.Expression expression -> SpiceExpressionLanguage.standard().read(expression.expression().text(),
                    expression.position(), scope.names());
        };
    }

    private static String node(String written) {
        String lower = written.toLowerCase(Locale.ROOT);
        return lower.equals("gnd") ? "0" : lower;
    }

    // ─── elements ───────────────────────────────────────────────────────────────────────────────────

    private Optional<Element> element(SpiceElement element, Scope scope) {
        return switch (element) {
            case SpiceElement.Passive passive -> {
                leaveOutRest(passive, passive.rest(), "its model, temperature coefficients or initial condition");
                PassiveKind kind = switch (passive.kind()) {
                    case RESISTOR -> PassiveKind.RESISTOR;
                    case CAPACITOR -> PassiveKind.CAPACITOR;
                    case INDUCTOR -> PassiveKind.INDUCTOR;
                };
                yield Optional.of(new SpiceNetlist.Passive(passive.name(), kind, node(passive.positive()),
                        node(passive.negative()), value(passive.value(), scope, passive.name() + "'s value"),
                        passive.position()));
            }
            case SpiceElement.Source source -> source(source, scope);
            case SpiceElement.Diode diode -> {
                leaveOutRest(diode, diode.rest(), "an area or an initial condition");
                yield Optional.of(new SpiceNetlist.Diode(diode.name(), node(diode.anode()), node(diode.cathode()),
                        diode.model(), diode.position()));
            }
            case SpiceElement.Bipolar bipolar -> {
                bipolar.substrate().ifPresent(substrate -> note(bipolar.position(), bipolar.name()
                        + "'s substrate node has no jMX counterpart; left out"));
                leaveOutRest(bipolar, bipolar.rest(), "an area or an initial condition");
                yield Optional.of(new SpiceNetlist.Bipolar(bipolar.name(), node(bipolar.collector()), node(bipolar.base()),
                        node(bipolar.emitter()), bipolar.model(), bipolar.position()));
            }
            case SpiceElement.Mosfet mosfet -> {
                Formula width = null;
                Formula length = null;
                List<SpiceArgument> rest = new ArrayList<>();
                for (SpiceArgument argument : mosfet.rest()) {
                    if (argument instanceof SpiceArgument.Assignment assignment && assignment.name().equalsIgnoreCase("W")) {
                        width = value(argumentValue(assignment.value()), scope, mosfet.name() + "'s W");
                    } else if (argument instanceof SpiceArgument.Assignment assignment && assignment.name().equalsIgnoreCase("L")) {
                        length = value(argumentValue(assignment.value()), scope, mosfet.name() + "'s L");
                    } else {
                        rest.add(argument);
                    }
                }
                leaveOutRest(mosfet, rest, "its other geometry (AD, AS, PD, PS, …)");
                yield Optional.of(new SpiceNetlist.Mosfet(mosfet.name(), node(mosfet.drain()), node(mosfet.gate()),
                        node(mosfet.source()), node(mosfet.bulk()), mosfet.model(), width, length, mosfet.position()));
            }
            case SpiceElement.Instance instance -> {
                Map<String, Formula> parameters = new LinkedHashMap<>();
                for (SpiceParameter parameter : instance.parameters()) {
                    parameters.put(parameter.name(), value(parameter.value(), scope, instance.name() + "'s "
                            + parameter.name()));
                }
                yield Optional.of(new SpiceNetlist.Instance(instance.name(),
                        instance.nodes().stream().map(SpiceAnalyzer::node).toList(), instance.subcircuit(), parameters,
                        instance.position()));
            }
            case SpiceElement.Behavioural behavioural -> unsupported(behavioural);
            case SpiceElement.Controlled controlled -> linearControlled(controlled, scope);
            case SpiceElement.Coupling coupling -> unsupported(coupling);
            case SpiceElement.Generic generic when generic.letter() == 'J' -> jfet(generic);
            case SpiceElement.Generic generic -> unsupported(generic);
        };
    }

    /** {@code J1 drain gate source model [area]}: a JFET; an area is left out with a note. */
    private Optional<Element> jfet(SpiceElement.Generic jfet) {
        List<SpiceArgument> arguments = jfet.arguments();
        if (arguments.size() < 4 || !arguments.subList(0, 4).stream().allMatch(SpiceArgument.Word.class::isInstance)) {
            throw new SpiceParseException(jfet.position(), jfet.name() + ": a JFET is 'name drain gate source model'");
        }
        List<String> words = arguments.subList(0, 4).stream().map(argument -> ((SpiceArgument.Word) argument).text()).toList();
        leaveOutRest(jfet, arguments.subList(4, arguments.size()), "an area or an initial condition");
        return Optional.of(new SpiceNetlist.Jfet(jfet.name(), node(words.get(0)), node(words.get(1)), node(words.get(2)),
                words.get(3), jfet.position()));
    }

    /**
     * {@code E1 out 0 in+ in- gain} and {@code G1 out 0 in+ in- gm}: a source of a voltage, or a current, proportional
     * to a controlling voltage. A polynomial, a table, a {@code VALUE=} expression and the current-controlled F and H
     * are left out with a note.
     */
    private Optional<Element> linearControlled(SpiceElement.Controlled controlled, Scope scope) {
        List<SpiceArgument> control = controlled.control();
        boolean linear = controlled.isVoltageControlled() && control.size() == 3
                && control.get(0) instanceof SpiceArgument.Word && control.get(1) instanceof SpiceArgument.Word
                && !(control.get(0) instanceof SpiceArgument.Word word && word.text().equalsIgnoreCase("POLY"));
        if (!linear) {
            return unsupported(controlled);
        }
        Formula gain = value(argumentValue(control.get(2)), scope, controlled.name() + "'s gain");
        return Optional.of(new SpiceNetlist.LinearControlled(controlled.name(), controlled.isVoltageSource(),
                node(controlled.positive()), node(controlled.negative()),
                node(((SpiceArgument.Word) control.get(0)).text()), node(((SpiceArgument.Word) control.get(1)).text()), gain,
                controlled.position()));
    }

    /** An element line's argument as a value: a number with its scale factor, a parameter's name, or an expression. */
    private static SpiceValue argumentValue(SpiceArgument argument) {
        return switch (argument) {
            case SpiceArgument.Word word when SpiceNumbers.read(word.text()).isPresent() ->
                    new SpiceValue.Number(SpiceNumbers.read(word.text()).get().value(), word.text(), word.position());
            case SpiceArgument.Word word -> new SpiceValue.Reference(word.text(), word.position());
            case SpiceArgument.Expression expression -> new SpiceValue.Expression(expression.expression());
            default -> throw new SpiceParseException(argument.position(), "'" + argument.written()
                    + "' is not a number, a parameter or an expression in braces");
        };
    }

    private Optional<Element> unsupported(SpiceElement element) {
        String what = UNSUPPORTED_ELEMENTS.getOrDefault(element.letter(), "an element of letter '" + element.letter() + "'");
        note(element.position(), element.name() + " (" + what + ") has no jMX counterpart; left out");
        return Optional.empty();
    }

    /** {@code V1 n+ n- [DC] [value] [SIN(…) | PULSE(…)]}: any other waveform is noted and the source left out. */
    private Optional<Element> source(SpiceElement.Source source, Scope scope) {
        String name = source.name();
        if (!source.rest().isEmpty()) {
            note(source.position(), name + ": '" + String.join(" ", source.rest().stream().map(SpiceArgument::written)
                    .toList()) + "' — an AC or distortion specification — is for analyses that are not converted; left out");
        }
        Formula direct = source.direct().map(value -> value(value, scope, name + "'s DC value")).orElse(null);
        SpiceNetlist.Signal signal;
        if (source.waveform().isEmpty()) {
            signal = new SpiceNetlist.Constant(direct == null ? new Formula.Constant(0) : direct);
        } else {
            SpiceWaveform waveform = source.waveform().get();
            IntFunction<Formula> at = index -> waveform.value(index)
                    .map(value -> value(value, scope, name + "'s " + waveform.kind() + " value " + (index + 1))).orElse(null);
            switch (waveform.kind()) {
                case SIN -> {
                    if (waveform.values().size() < 2 || waveform.values().size() > 6) {
                        throw new SpiceParseException(waveform.position(), "SIN is SIN(offset amplitude [frequency "
                                + "[delay [damping [phase]]]])");
                    }
                    signal = new SpiceNetlist.Sine(at.apply(0), at.apply(1), at.apply(2), orZero(at.apply(3)),
                            orZero(at.apply(4)), orZero(at.apply(5)));
                }
                case PULSE -> {
                    if (waveform.values().size() < 2 || waveform.values().size() > 8) {
                        throw new SpiceParseException(waveform.position(), "PULSE is PULSE(initial pulsed [delay [rise "
                                + "[fall [width [period]]]]])");
                    }
                    if (waveform.values().size() == 8) {
                        note(waveform.position(), name + "'s pulse count has no jMX counterpart; left out");
                    }
                    signal = new SpiceNetlist.Pulse(at.apply(0), at.apply(1), orZero(at.apply(2)), orZero(at.apply(3)),
                            orZero(at.apply(4)), at.apply(5), at.apply(6));
                }
                default -> {
                    note(waveform.position(), name + " (" + waveform.kind() + " waveform) has no jMX counterpart; left "
                            + "out — a jMX generator writes its voltage as an equation of the time");
                    return Optional.empty();
                }
            }
            if (direct != null && !isZero(direct)) {
                note(source.position(), name + "'s DC value sets SPICE's operating point only; the transient follows "
                        + "its waveform, and so does jMX — the DC value is left out");
            }
        }
        return Optional.of(new SpiceNetlist.Source(name, source.isVoltage(), node(source.positive()),
                node(source.negative()), signal, source.position()));
    }

    // ─── subcircuits, models, the transient ─────────────────────────────────────────────────────────

    private void subcircuit(SpiceCard.Subcircuit card, Scope outer) {
        List<String> declared = new ArrayList<>();
        card.parameters().forEach(parameter -> declared.add(parameter.name()));
        declared.addAll(declaredParameters(card.statements()));
        Scope scope = outer.with(declared);
        List<Parameter> parameters = new ArrayList<>();
        for (SpiceParameter parameter : card.parameters()) {
            parameters.add(new Parameter(parameter.name(), value(parameter.value(), scope, "parameter " + parameter.name()),
                    parameter.position()));
        }
        List<Element> elements = new ArrayList<>();
        for (SpiceStatement statement : card.statements()) {
            statement(statement, scope, parameters).ifPresent(elements::add);
        }
        String key = card.name().toLowerCase(Locale.ROOT);
        if (subcircuits.containsKey(key)) {
            note(card.position(), "subcircuit '" + card.name() + "' is defined again; this definition replaces the first");
        }
        subcircuits.put(key, new SpiceNetlist.Subcircuit(card.name(),
                card.pins().stream().map(SpiceAnalyzer::node).toList(), parameters, elements, card.position()));
    }

    private void model(SpiceCard.Model card, Scope scope) {
        ModelType type;
        try {
            type = ModelType.valueOf(card.type().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            note(card.position(), "model '" + card.name() + "' is of type " + card.type().toUpperCase(Locale.ROOT)
                    + ", which has no jMX counterpart; left out");
            return;
        }
        Map<String, Formula> parameters = new LinkedHashMap<>();
        for (SpiceParameter parameter : card.parameters()) {
            parameters.put(parameter.name().toUpperCase(Locale.ROOT), value(parameter.value(), scope,
                    "model " + card.name() + "'s " + parameter.name()));
        }
        String key = card.name().toLowerCase(Locale.ROOT);
        if (models.containsKey(key)) {
            note(card.position(), "model '" + card.name() + "' is defined again; this definition replaces the first");
        }
        models.put(key, new SpiceNetlist.Model(card.name(), type, parameters, card.position()));
    }

    /** {@code .tran step stop [start [largest step]] [uic]}, its values worked out from the parameters. */
    private SpiceNetlist.Transient transientAnalysis(SpiceCard.Transient card, Scope scope, List<Parameter> parameters) {
        if (card.initialConditions()) {
            note(card.position(), "'uic' has no jMX counterpart: a circuit starts from rest; left out");
        }
        card.largestStep().ifPresent(step -> note(card.position(), "the largest time step has no jMX counterpart: the "
                + "engine chooses its own; left out"));
        Map<String, Double> known = values(parameters);
        double step = number(value(card.step(), scope, "the transient's step"), known, card.step().position());
        double stop = number(value(card.stop(), scope, "the transient's end"), known, card.stop().position());
        double start = card.start().isPresent()
                ? number(value(card.start().get(), scope, "the transient's start"), known, card.start().get().position()) : 0;
        if (!(stop > start) || start < 0) {
            throw new SpiceParseException(card.position(), "the transient ends after it starts, and starts at 0 or later");
        }
        return new SpiceNetlist.Transient(step, stop, start, card.position());
    }

    /** The parameters' values, each worked out from those before it; one that cannot be is left out. */
    static Map<String, Double> values(List<Parameter> parameters) {
        Map<String, Double> known = new LinkedHashMap<>();
        for (Parameter parameter : parameters) {
            try {
                known.put(parameter.name(), evaluate(parameter.value(), known));
            } catch (RuntimeException unknown) {
                // a parameter of one not yet known: it, and those that need it, stay expressions
            }
        }
        return known;
    }

    /** A formula's value against the values given. */
    static double evaluate(Formula formula, Map<String, Double> values) {
        List<String> names = List.copyOf(values.keySet());
        double[] arguments = names.stream().mapToDouble(values::get).toArray();
        return RealCompiler.compile(formula, Symbols.of(names.toArray(String[]::new)), FormulaPrinter.text(formula))
                .evaluate(arguments);
    }

    private static double number(Formula formula, Map<String, Double> known, SpicePosition position) {
        try {
            return evaluate(formula, known);
        } catch (RuntimeException unknown) {
            throw new SpiceParseException(position, "this value cannot be worked out from the parameters");
        }
    }

    // ─── notes ──────────────────────────────────────────────────────────────────────────────────────

    /** The title, when it reads like an element: SPICE took it as the title, and so does the converter — said aloud. */
    private void noteTitle(String title) {
        if (title.matches("(?i)[RCLVIDQMX][A-Za-z0-9_]*\\s+\\S+\\s+\\S+.*")) {
            notes.add(new SpiceNote(1, "the first line is the netlist's title, as SPICE reads it, so '" + title
                    + "' is not a part; put a title line first if it was meant to be one"));
        }
    }

    private void note(SpicePosition position, String text) {
        notes.add(new SpiceNote(position.line(), text));
    }

    private void leaveOutRest(SpiceElement element, List<SpiceArgument> rest, String what) {
        if (!rest.isEmpty()) {
            note(element.position(), element.name() + ": " + what + " has no jMX counterpart; left out ('"
                    + String.join(" ", rest.stream().map(SpiceArgument::written).toList()) + "')");
        }
    }

    private static Formula orZero(Formula formula) {
        return formula == null ? new Formula.Constant(0) : formula;
    }

    private static boolean isZero(Formula formula) {
        return formula instanceof Formula.Constant constant && constant.value() == 0;
    }
}
