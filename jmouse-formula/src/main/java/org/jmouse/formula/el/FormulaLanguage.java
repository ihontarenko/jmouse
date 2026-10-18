package org.jmouse.formula.el;

import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.Formula;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.formula.MathematicalFunction;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.Symbols;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * jMF — jMouse Formula: the mathematical dialect of jME. It is a jME {@link ExpressionLanguage} assembled from a
 * formula container (power on {@code ^}, the formula grammar, the mathematical functions) and a lexer that keeps
 * {@code b+2.5} from reading as {@code b} and {@code +2.5}. Parsing is jME's; the result is translated into the
 * immutable {@link Formula} tree that jMF compiles, differentiates and prints.
 *
 * <p>The standard instance is shared and thread-safe for parsing, and caches what it has parsed.
 */
public final class FormulaLanguage {

    private static final FormulaLanguage STANDARD = new FormulaLanguage();
    private static final int MAXIMUM_LENGTH = 16000;

    private final ExpressionLanguage  language;
    private final Set<String>         hostCalls;
    private final Map<String, String> rates;

    public FormulaLanguage() {
        FormulaExtensionContainer extensions = new FormulaExtensionContainer();
        DefaultParserContext context = new DefaultParserContext(extensions);
        this.language = new ExpressionLanguage(extensions, new FormulaLexer(), context, ExpressionParser.class);
        this.hostCalls = Set.of();
        this.rates = Map.of();
    }

    private FormulaLanguage(ExpressionLanguage language, Set<String> hostCalls, Map<String, String> rates) {
        this.language = language;
        this.hostCalls = Set.copyOf(hostCalls);
        this.rates = Map.copyOf(rates);
    }

    /**
     * The same language, reading {@code d(f)/d<variable>} — Leibniz's notation, recognised in the tree, never in the
     * text — as the {@link Formula.HostCall} {@code hostCall} over {@code f}: a rate its host integrates rather than a
     * derivative taken here, such as a circuit's {@code d(q)/dt}. With it declared, a {@code d(…)} that is not over
     * {@code d<variable>}, and a {@code d<variable>} that is not under a {@code d(…)}, are refused naming the notation.
     *
     * @param variable the variable the rate is over: {@code t} for {@code d(q)/dt}
     * @param hostCall the name the host binds the rate by; it need not be one a document can write
     */
    public FormulaLanguage withRate(String variable, String hostCall) {
        Map<String, String> declared = new LinkedHashMap<>(rates);
        declared.put(RATE_OPERATOR + variable, hostCall);
        return new FormulaLanguage(language, hostCalls, declared);
    }

    /** The {@code d} of {@code d(f)/dt}, both above and in front of the variable below. */
    public static final String RATE_OPERATOR = "d";

    /**
     * The same language, reading a call of any of {@code names} as a {@link Formula.HostCall} its host binds — a
     * circuit solver's {@code V(out, GND)}, {@code I(R1)}, {@code ddt(q)} — instead of refusing it or reading
     * {@code V(out)} as the product {@code V · out}.
     */
    public FormulaLanguage withHostCalls(String... names) {
        Set<String> declared = new LinkedHashSet<>(hostCalls);
        declared.addAll(List.of(names));
        return new FormulaLanguage(language, declared, rates);
    }

    public static FormulaLanguage standard() {
        return STANDARD;
    }

    /** The jME language underneath, for evaluating a formula with jME's own evaluator. */
    public ExpressionLanguage expressionLanguage() {
        return language;
    }

    /** A jME evaluation context that already knows the named constants (pi, e, tau, phi). */
    public EvaluationContext newContext() {
        EvaluationContext context = language.newContext();
        for (MathematicalConstant constant : MathematicalConstant.values()) {
            context.setValue(constant.symbol(), constant.value());
        }
        return context;
    }

    /** The value of a formula document at these values of its names — for one-off values; sampling compiles once. */
    public double evaluate(String source, Map<String, ? extends Number> values) {
        Formula formula = parse(source);
        List<String> names = List.copyOf(values.keySet());
        double[] arguments = new double[names.size()];
        for (int index = 0; index < names.size(); index++) {
            arguments[index] = values.get(names.get(index)).doubleValue();
        }
        return RealCompiler.compile(formula, Symbols.of(names.toArray(String[]::new)), source).evaluate(arguments);
    }

    /** A formula document — definitions, then the formula — read on its own. */
    public Formula parse(String source) {
        return parse(source, List.of());
    }

    /**
     * The document solved for one name, when its formula reads {@code name = …} and the right side does not use that
     * name: {@code g(t) = t^2; y = g(x) + 1} solved for {@code y} is {@code g(t) = t^2; g(x) + 1}. Any other
     * document — no equation, another left side, the name on both sides — is not solved for it.
     */
    public Optional<String> solvedFor(String source, String name) {
        FormulaStatements.Document document = FormulaStatements.split(FormulaNotation.normalize(source));
        Optional<String[]> sides = FormulaStatements.equationSides(document.expression());
        if (sides.isEmpty() || !sides.get()[0].strip().equals(name)) {
            return Optional.empty();
        }
        StringBuilder solved = new StringBuilder();
        for (FormulaStatements.Definition definition : document.definitions()) {
            solved.append(definition.name());
            if (!definition.parameters().isEmpty()) {
                solved.append('(').append(String.join(", ", definition.parameters())).append(')');
            }
            solved.append(" = ").append(definition.body()).append("; ");
        }
        solved.append(sides.get()[1].strip());
        String candidate = solved.toString();
        if (parse(candidate).variables().contains(name)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    /**
     * A formula document read against a library of definitions it may call as well as its own, such as the
     * functions an object or a scene declares once for all its formulas. A document's own definition of a
     * name hides the library's.
     */
    public Formula parse(String source, List<Formula.Definition> library) {
        if (source == null || source.isBlank()) {
            throw new FormulaException("A formula is required.", source, -1);
        }
        if (source.length() > MAXIMUM_LENGTH) {
            throw new FormulaException("A formula is at most " + MAXIMUM_LENGTH + " characters long.", source, -1);
        }
        String normalized = FormulaNotation.normalize(source);
        FormulaStatements.Document document = FormulaStatements.split(normalized);
        Map<String, Formula.Definition> definitions = new LinkedHashMap<>();
        library.forEach(definition -> definitions.put(definition.name(), definition));
        for (FormulaStatements.Definition statement : document.definitions()) {
            requireDefinable(statement.name(), source);
            // Each body sees only what is defined before it, so a definition can never reach itself.
            requireNotSelfReferring(statement, source);
            Formula body = expression(statement.body(), definitions, source);
            Formula.Definition definition = new Formula.Definition(statement.name(), statement.parameters(), body);
            definitions.put(definition.name(), definition);
        }
        Optional<String[]> sides = FormulaStatements.equationSides(document.expression());
        if (sides.isPresent()) {
            return new Formula.Equation(expression(sides.get()[0], definitions, source),
                    expression(sides.get()[1], definitions, source));
        }
        return expression(document.expression(), definitions, source);
    }

    /** Every definition a document makes, in order, for a library others will call. */
    public List<Formula.Definition> definitions(String source) {
        return definitions(source, List.of());
    }

    /**
     * Every definition a document makes, in order, each able to call the library and the definitions before it.
     * Only the document's own definitions are returned.
     */
    public List<Formula.Definition> definitions(String source, List<Formula.Definition> library) {
        String normalized = FormulaNotation.normalize(source);
        FormulaStatements.Document document = FormulaStatements.split(normalized + ";0");
        Map<String, Formula.Definition> definitions = new LinkedHashMap<>();
        library.forEach(definition -> definitions.put(definition.name(), definition));
        Map<String, Formula.Definition> own = new LinkedHashMap<>();
        for (FormulaStatements.Definition statement : document.definitions()) {
            requireDefinable(statement.name(), source);
            Formula body = expression(statement.body(), definitions, source);
            Formula.Definition definition = new Formula.Definition(statement.name(), statement.parameters(), body);
            definitions.put(statement.name(), definition);
            own.put(statement.name(), definition);
        }
        return List.copyOf(own.values());
    }

    private Formula expression(String text, Map<String, Formula.Definition> definitions, String source) {
        if (text.isBlank()) {
            throw new FormulaException("A side of the formula is empty.", source, -1);
        }
        Expression parsed;
        try {
            parsed = language.compile(text);
        } catch (RuntimeException exception) {
            throw new FormulaException("The formula does not parse: " + exception.getMessage(), source, -1);
        }
        return new FormulaTranslator(Map.copyOf(definitions), hostCalls, rates, source).translate(parsed);
    }

    /**
     * Recursion is impossible by construction — a body sees only earlier definitions — but an author who writes
     * it deserves to hear that rather than that the name is unknown.
     */
    private static void requireNotSelfReferring(FormulaStatements.Definition statement, String source) {
        Pattern ownName = Pattern.compile("(?<![A-Za-z0-9_\\p{IsGreek}])" + Pattern.quote(statement.name())
                + "(?![A-Za-z0-9_\\p{IsGreek}])");
        if (ownName.matcher(statement.body()).find()) {
            throw new FormulaException(statement.name() + " cannot call itself; jMF definitions are not recursive, "
                    + "and a sum or a product usually says the same thing.", source, source.indexOf(statement.body()));
        }
    }

    private static void requireDefinable(String name, String source) {
        if (FormulaTranslator.FORMS.contains(name) || MathematicalFunction.bySymbol(name).isPresent()
                || MathematicalConstant.bySymbol(name).isPresent() || name.equals("i")) {
            throw new FormulaException("'" + name + "' is built into jMF and cannot be redefined.", source,
                    source.indexOf(name));
        }
    }
}
