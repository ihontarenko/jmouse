package org.jmouse.formula.el;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.QuantityParser;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.MathematicalFunction;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.Symbols;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * jMF — jMouse Formula: the mathematical dialect of jME. It is a jME {@link ExpressionLanguage} assembled from a
 * formula container (power on {@code ^}, the formula grammar, the mathematical functions) and a lexer in the
 * mathematical dialect, which has no type suffixes and no step operators. A document's statements are read by
 * {@link FormulaDocumentParser}, their expressions by jME; the result is translated into the immutable {@link Formula}
 * tree that jMF compiles, differentiates and prints.
 *
 * <p>The standard instance is shared and thread-safe for parsing, and caches the documents it has read.
 */
public final class FormulaLanguage {

    private static final FormulaLanguage STANDARD = new FormulaLanguage();
    private static final int MAXIMUM_LENGTH = 16000;

    /** The parts every derived language shares: how text becomes statements, and what it has read. */
    private record Reading(ExpressionLanguage language, Lexer lexer, ParserContext context,
                           FormulaDocumentParser document, Map<String, List<FormulaStatement>> statements) {
    }

    private final Reading             reading;
    private final Set<String>         hostCalls;
    private final Map<String, String> rates;

    public FormulaLanguage() {
        this(reading(false), Set.of(), Map.of());
    }

    private static Reading reading(boolean quantities) {
        FormulaExtensionContainer extensions = new FormulaExtensionContainer();
        if (quantities) {
            extensions.addParser(new QuantityParser());
        }
        DefaultParserContext context = new DefaultParserContext(extensions);
        Lexer lexer = new DefaultLexer(new DefaultTokenizer(
                new ExpressionSplitter(ExpressionSplitter.Dialect.MATHEMATICS), new ExpressionRecognizer()));
        return new Reading(new ExpressionLanguage(extensions, lexer, context, ExpressionParser.class), lexer, context,
                new FormulaDocumentParser(), new ConcurrentHashMap<>());
    }

    private FormulaLanguage(Reading reading, Set<String> hostCalls, Map<String, String> rates) {
        this.reading = reading;
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
        return new FormulaLanguage(reading, hostCalls, declared);
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
        return new FormulaLanguage(reading, declared, rates);
    }

    /**
     * The same language, reading a number with its unit as one operand in SI — {@code 60 ms} is {@code 0.06},
     * {@code -30 deg} is {@code -π/6} — where the standard one reads {@code 2 m} as twice {@code m}: for a text written
     * with units, such as a drawing's values.
     */
    public FormulaLanguage withQuantities() {
        return new FormulaLanguage(reading(true), hostCalls, rates);
    }

    public static FormulaLanguage standard() {
        return STANDARD;
    }

    /** The jME language underneath, for evaluating a formula with jME's own evaluator. */
    public ExpressionLanguage expressionLanguage() {
        return reading.language();
    }

    /** A jME evaluation context that already knows the named constants (pi, e, tau, phi). */
    public EvaluationContext newContext() {
        EvaluationContext context = reading.language().newContext();
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
        List<FormulaStatement> statements = statements(source);
        if (!(statements.getLast() instanceof FormulaStatement.Assignment equation)
                || !equation.left().text().equals(name)) {
            return Optional.empty();
        }
        StringBuilder solved = new StringBuilder();
        for (FormulaStatement.Assignment definition : definitions(statements.subList(0, statements.size() - 1), source)) {
            FormulaStatement.Head head = definition.head().orElseThrow();
            solved.append(head.name());
            if (!head.parameters().isEmpty()) {
                solved.append('(').append(String.join(", ", head.parameters())).append(')');
            }
            solved.append(" = ").append(definition.right().text()).append("; ");
        }
        solved.append(equation.right().text());
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
        List<FormulaStatement> statements = statements(source);
        Map<String, Formula.Definition> definitions = new LinkedHashMap<>();
        library.forEach(definition -> definitions.put(definition.name(), definition));
        for (FormulaStatement.Assignment statement : definitions(statements.subList(0, statements.size() - 1), source)) {
            // Each body sees only what is defined before it, so a definition can never reach itself.
            requireNotSelfReferring(statement, source);
            Formula.Definition definition = definition(statement, definitions, source);
            definitions.put(definition.name(), definition);
        }
        return switch (statements.getLast()) {
            case FormulaStatement.Assignment equation -> new Formula.Equation(
                    translate(equation.left(), definitions, source), translate(equation.right(), definitions, source));
            case FormulaStatement.Plain plain -> translate(plain.expression(), definitions, source);
        };
    }

    /**
     * A formula document's statements as they were typed — {@code r = sqrt(x^2 + y^2)}, {@code sin(r) / r} —, each cut
     * from the source where this grammar says it begins and ends, so a {@code ;} inside brackets or a statement that
     * goes on to the next line is never mistaken for its end.
     */
    public List<String> statementTexts(String source) {
        List<FormulaStatement> statements = statements(source);
        FormulaNotation.Normalized normalized = FormulaNotation.normalizeTracked(source);
        return statements.stream().map(statement -> source.substring(normalized.sourceIndex(statement.offset()),
                normalized.sourceIndex(statement.end())).strip()).toList();
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
        Map<String, Formula.Definition> definitions = new LinkedHashMap<>();
        library.forEach(definition -> definitions.put(definition.name(), definition));
        Map<String, Formula.Definition> own = new LinkedHashMap<>();
        for (FormulaStatement.Assignment statement : definitions(library(source), source)) {
            Formula.Definition definition = definition(statement, definitions, source);
            definitions.put(definition.name(), definition);
            own.put(definition.name(), definition);
        }
        return List.copyOf(own.values());
    }

    /** A formula document's statements: at least one, the last of them the formula. */
    private List<FormulaStatement> statements(String source) {
        List<FormulaStatement> statements = source == null ? List.of() : library(source);
        if (statements.isEmpty()) {
            throw new FormulaException("A formula is required.", source, -1);
        }
        return statements;
    }

    /** A library's statements, read once per text: an empty library, or one of comments alone, defines nothing. */
    private List<FormulaStatement> library(String source) {
        if (source.isBlank()) {
            return List.of();
        }
        if (source.length() > MAXIMUM_LENGTH) {
            throw new FormulaException("A formula is at most " + MAXIMUM_LENGTH + " characters long.", source, -1);
        }
        String normalized = FormulaNotation.normalize(source);
        List<FormulaStatement> statements = reading.statements().get(normalized);
        if (statements == null) {
            statements = read(normalized, source);
            reading.statements().put(normalized, statements);
        }
        return statements;
    }

    private List<FormulaStatement> read(String normalized, String source) {
        Node root;
        try {
            TokenCursor cursor = reading.lexer().tokenize(new StringSource("FORMULA", normalized));
            root = BasicNode.forToken(cursor.current());
            reading.document().parse(cursor, root, reading.context());
        } catch (FormulaException refusal) {
            throw new FormulaException(refusal.getMessage(), source, refusal.position());
        } catch (RuntimeException exception) {
            throw new FormulaException("The formula does not parse: " + exception.getMessage(), source, -1);
        }
        return root.getFirst().getChildren().stream()
                .map(node -> ((FormulaDocumentParser.FormulaStatementNode) node).statement()).toList();
    }

    /** These statements as definitions: each is {@code name = …} or {@code name(parameters) = …}. */
    private static List<FormulaStatement.Assignment> definitions(List<FormulaStatement> statements, String source) {
        return statements.stream().map(statement -> {
            if (statement instanceof FormulaStatement.Assignment assignment && assignment.head().isPresent()) {
                requireDefinable(assignment.head().get().name(), source);
                return assignment;
            }
            throw new FormulaException("'" + written(statement) + "' is neither a definition nor the last statement; "
                    + "only the last one is the formula itself.", source, statement.offset());
        }).toList();
    }

    private static String written(FormulaStatement statement) {
        return switch (statement) {
            case FormulaStatement.Assignment assignment -> assignment.left().text() + " = " + assignment.right().text();
            case FormulaStatement.Plain plain -> plain.expression().text();
        };
    }

    private Formula.Definition definition(FormulaStatement.Assignment statement,
                                          Map<String, Formula.Definition> definitions, String source) {
        FormulaStatement.Head head = statement.head().orElseThrow();
        return new Formula.Definition(head.name(), head.parameters(), translate(statement.right(), definitions, source));
    }

    private Formula translate(FormulaStatement.Side side, Map<String, Formula.Definition> definitions, String source) {
        return new FormulaTranslator(Map.copyOf(definitions), hostCalls, rates, source).translate(side.expression());
    }

    /**
     * Recursion is impossible by construction — a body sees only earlier definitions — but an author who writes
     * it deserves to hear that rather than that the name is unknown.
     */
    private static void requireNotSelfReferring(FormulaStatement.Assignment statement, String source) {
        String name = statement.head().orElseThrow().name();
        if (statement.right().names().contains(name)) {
            throw new FormulaException(name + " cannot call itself; jMF definitions are not recursive, "
                    + "and a sum or a product usually says the same thing.", source, statement.offset());
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
