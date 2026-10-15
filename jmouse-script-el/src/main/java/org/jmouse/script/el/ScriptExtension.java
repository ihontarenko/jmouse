package org.jmouse.script.el;

import org.jmouse.core.access.AttributeResolver;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Filter;
import org.jmouse.el.extension.Function;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.Test;
import org.jmouse.el.extension.attribute.JavaBeanAttributeResolver;
import org.jmouse.el.extension.attribute.ListAttributeResolver;
import org.jmouse.el.extension.attribute.MapAttributeResolver;
import org.jmouse.el.extension.filter.*;
import org.jmouse.el.extension.filter.converter.*;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.extension.test.*;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.*;
import org.jmouse.formula.el.FormulaCoreExtension;
import org.jmouse.formula.el.FormulaOperator;
import org.jmouse.script.el.function.AppendFunction;
import org.jmouse.script.el.parser.BehaviourParser;
import org.jmouse.script.el.parser.HandlerParser;
import org.jmouse.script.el.parser.ParserPriority;
import org.jmouse.script.el.parser.ScriptDocumentParser;
import org.jmouse.script.el.parser.ScriptParser;
import org.jmouse.script.el.StatementExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * jMS — the one script language: the statement core, jME's expressions, jMF's mathematics, and — optionally —
 * {@code on … do … end}, the interception of an event, with {@code script} and {@code behaviour} grouping the
 * handlers of a file.
 *
 * <p>A document needs none of the event constructions. A list of statements that returns a value is a program
 * ({@link ProgramEvaluator}); a file with handlers is a script a host drives ({@link ScriptEvaluator}). What either
 * may reach is never the document's to claim — it is the facades and the budget the evaluation context was given
 * by whoever runs it.</p>
 *
 * <ul>
 *   <li><strong>Statements</strong> are the core's ({@link StatementExtension}): {@code include}, {@code function},
 *       {@code if}, {@code for}, {@code local}, {@code return}, assignment.</li>
 *   <li><strong>Expressions</strong> are jME's grammar — filters, tests, {@code ??}, ranges, {@code in}, map and
 *       list literals, and {@code @facade.method()}.</li>
 *   <li><strong>Mathematics</strong> is jMF's vocabulary: {@code ^} as a power and every mathematical function
 *       ({@code sin}, {@code gamma}, {@code erf}, {@code binomial}, …). jMF's <em>formula grammar</em> — implicit
 *       multiplication, definitions, equations — is not taken: {@code 3 r} would be ambiguous among statements.</li>
 * </ul>
 *
 * <h2>⚠️ What is deliberately absent</h2>
 *
 * <p>The vocabulary is closed: no reflection ({@code ClassFilter}, {@code ClassFunction},
 * {@code JavaReflectedFunction} would make the facade boundary decorative), no wall clock ({@code NowFunction}
 * answers differently on two machines; a host that wants time gives a clock facade), and no
 * {@code setVariable}/{@code getVariable} (the language has {@code local} and assignment).</p>
 *
 * <p>⚠️ The order of {@link #getParsers()} carries no meaning. Dispatch sorts by {@link ParserPriority} and the
 * core's {@code StatementPriority}, and those classes — not this list — say why each shape is offered first.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptExtension implements Extension {

    /**
     * The dialect's own vocabulary, as names, computed once.
     *
     * <p>⚠️ <strong>Derived from the lists below rather than written out again.</strong> Two lists of
     * the same names is one list plus a defect, waiting for somebody to add a filter to only one.</p>
     *
     * <p>⚠️ And <strong>held</strong> rather than recomputed: the binder asks for all three, and each
     * question used to construct a whole {@code ScriptExtension} — thirty-odd parser objects included —
     * in order to read a handful of strings off it.</p>
     */
    private static final Set<String> FUNCTION_NAMES = namesOf(new ScriptExtension().getFunctions(), Function::getName);
    private static final Set<String> FILTER_NAMES   = namesOf(new ScriptExtension().getFilters(), Filter::getName);
    private static final Set<String> TEST_NAMES     = namesOf(new ScriptExtension().getTests(), Test::getName);

    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return List.of(
                new JavaBeanAttributeResolver(),
                new MapAttributeResolver(),
                new ListAttributeResolver()
        );
    }

    /**
     * jMF's mathematical functions — they replace jME's own sqrt, exp, min and max, which they include — and
     * {@code append}, how a script accumulates a list.
     */
    @Override
    public List<Function> getFunctions() {
        List<Function> functions = new ArrayList<>(FormulaCoreExtension.mathematicalFunctions());
        functions.add(new AppendFunction());
        return List.copyOf(functions);
    }

    @Override
    public List<Filter> getFilters() {
        return List.of(
                new FilterFilter(),
                new MapFilter(),
                new LowerFilter(),
                new UpperFilter(),
                new SubFilter(),
                new DefaultFilter(),
                new TrimFilter(),
                new LengthFilter(),
                new SplitFilter(),
                new JoinFilter(),
                new LastFilter(),
                new FirstFilter(),
                // type-converters
                new ToBooleanFilter(),
                new ToByteFilter(),
                new ToShortFilter(),
                new ToIntFilter(),
                new ToLongFilter(),
                new ToFloatFilter(),
                new ToDoubleFilter(),
                new ToCharacterFilter(),
                new ToStringFilter(),
                new ToListFilter(),
                new ToArrayFilter(),
                new ToIteratorFilter()
        );
    }

    @Override
    public List<Test> getTests() {
        return List.of(
                new EvenTest(),
                new OddTest(),
                new ArrayTest(),
                new CollectionTest(),
                new MapTest(),
                new IterableTest(),
                new HasAllTest(),
                new HasAnyTest(),
                new HasNoneTest(),
                new NullTest(),
                new TypeTest(),
                new StartsTest(),
                new EndsTest(),
                new ContainsTest()
        );
    }

    /**
     * jME's expression parsers, the statement core's, and jMS's own four: the document, {@code script},
     * {@code behaviour} and the {@code on} handler.
     */
    @Override
    public List<Parser> getParsers() {
        List<Parser> parsers = new ArrayList<>(List.of(
                new ExpressionParser(),
                new OperatorParser(),
                new PrimaryExpressionParser(),
                new AutodetectFirstParser(),
                new RangeParser(),
                new FunctionParser(),
                new ScopedCallParser(),
                new BeanAccessParser(),
                // ⚠️ Registered so they PARSE, not so they may be used. `PrimaryExpressionParser` reads `(a)` as
                // the opening of a lambda before it reads it as a parenthesised expression, so a language that
                // left LambdaParser out would fail on ordinary brackets — and a host refuses these shapes, by name
                // and with a line, when it audits the tree.
                new LambdaParser(),
                new PlaceholderParser(),
                new VariableAliasParser(),
                new TestParser(),
                new FilterParser(),
                new PropertyParser(),
                new LiteralParser(),
                new ArrayParser(),
                new MapParser(),
                new KeyValueParser(),
                new ParenthesesParser(),
                new ArgumentsParser(),
                new NamesParser(),
                new ParametersParser()
        ));
        parsers.addAll(new StatementExtension().getParsers());
        // jms specific — the order here is documented by ParserPriority, not by this list
        parsers.add(new ScriptDocumentParser());
        parsers.add(new ScriptParser());
        parsers.add(new BehaviourParser());
        parsers.add(new HandlerParser());
        return List.copyOf(parsers);
    }

    /**
     * jME's operators with {@code ^} as jMF's power — the caret is free since jME no longer reads it as XOR — and
     * {@code /} as jMF's exact division, so {@code 7 / 2} is {@code 3.5} (decision 69).
     */
    @Override
    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>();
        operators.addAll(Arrays.asList(MathematicOperator.values()));
        operators.remove(MathematicOperator.DIVIDE);
        operators.add(FormulaOperator.DIVIDE);
        operators.add(FormulaOperator.POWER);
        operators.addAll(Arrays.asList(UnaryOperator.values()));
        operators.addAll(Arrays.asList(LogicalOperator.values()));
        operators.addAll(Arrays.asList(ComparisonOperator.values()));
        operators.add(FilterOperator.FILTER);
        operators.add(TestOperator.IS);
        operators.add(InOperator.IN);
        operators.add(NullCoalesceOperator.NULL_COALESCE);
        operators.add(RangeOperator.RANGE);
        operators.add(ConcatOperator.CONCAT);
        return operators;
    }

    /**
     * The function names this dialect provides on its own, beside whatever a host declares.
     *
     * <p>⚠️ Read by the binder, so that {@code sqrt(x)} is not refused as "a function this host does not
     * declare" — the host did not declare it and never should have to. Derived from
     * {@link #getFunctions()} rather than written out again, because two lists of the same names is one
     * list and a bug waiting for somebody to add a function to only one of them.</p>
     *
     * @return the built-in function names
     */
    public static Set<String> builtinFunctions() {
        return FUNCTION_NAMES;
    }

    /**
     * The filter names this dialect provides.
     *
     * @return the built-in filter names
     */
    public static Set<String> builtinFilters() {
        return FILTER_NAMES;
    }

    /**
     * The test names this dialect provides.
     *
     * @return the built-in test names
     */
    public static Set<String> builtinTests() {
        return TEST_NAMES;
    }

    /**
     * Collects the names of a list of extension elements.
     *
     * @param elements the registered elements
     * @param name    how to ask one for its name
     * @param <E>     the element type
     * @return their names
     */
    private static <E> Set<String> namesOf(List<E> elements, java.util.function.Function<E, String> name) {
        return elements.stream().map(name).collect(Collectors.toUnmodifiableSet());
    }
}
