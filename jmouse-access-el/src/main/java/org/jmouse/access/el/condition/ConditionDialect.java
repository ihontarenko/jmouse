package org.jmouse.access.el.condition;

import org.jmouse.core.access.AttributeResolver;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Filter;
import org.jmouse.el.extension.Function;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.Test;
import org.jmouse.el.extension.attribute.JavaBeanAttributeResolver;
import org.jmouse.el.extension.attribute.ListAttributeResolver;
import org.jmouse.el.extension.attribute.MapAttributeResolver;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.extension.operator.NullCoalesceOperator;
import org.jmouse.el.extension.operator.TestOperator;
import org.jmouse.el.extension.test.*;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The cut-down expression language an authorization condition is allowed to be written in.
 *
 * <p>jMouse EL is configurable by whitelist, which is the right foundation — but a whitelist of
 * <em>functions</em> is not enough on its own, because two of the three problems live elsewhere.
 * Everything below is left out deliberately.</p>
 *
 * <h2>Escape hatches — a rule that can reach outside itself</h2>
 * <table>
 *   <caption>Removed by registering neither the parser nor the function</caption>
 *   <tr><th>Feature</th><th>Why it is off</th></tr>
 *   <tr><td>{@code @bean.method(args)}, {@code @bean#CONST}, {@code @bean:$field}</td>
 *       <td>calls any method on any container bean. {@code @userRepository.deleteAll()} inside a
 *           policy file bypasses every decision made above it</td></tr>
 *   <tr><td>{@code class('fqcn')}</td><td>the same hole, one step longer</td></tr>
 *   <tr><td>{@code set(…)}, static imports</td>
 *       <td>mutate the evaluation context, so one predicate changes what the next one sees</td></tr>
 *   <tr><td>filters</td><td>a filter may touch I/O, and none of them answer a question a rule asks</td></tr>
 * </table>
 *
 * <h2>⚠️ Functions are registered now, and the whitelist moved rather than opened</h2>
 *
 * <p>The dialect used to register no functions <em>and</em> no {@link FunctionParser}, which read as one
 * decision and was two. Only one of them was load-bearing.
 *
 * <p>{@link FunctionParser} is pure syntax — {@code Identifier ( ":" Identifier )? Arguments?}. It
 * reaches no bean: {@code @bean.method()} is a different parser behind a token
 * {@link ConditionVocabulary} still refuses lexically. Every hole in the table above lives in the
 * <strong>function registry</strong>, and the registry is now a {@link FunctionCatalog} carrying
 * {@link AccessFunction} and nothing else — so what a policy may call is what a product deliberately
 * contributed, and {@code class(…)} is not on that list because nobody implemented the marker for it.
 *
 * <p>What this buys is a predicate that may <em>read state</em> — how much has been consumed, what a
 * counter says — which the five members of a
 * {@link org.jmouse.access.spi.ConditionContext} cannot answer between them. What it does not buy is a
 * predicate that may reach anywhere: those are different permissions, and conflating them is why the
 * dialect was stricter than it needed to be.
 *
 * <h2>Operators that answer wrongly instead of failing</h2>
 *
 * <p>⚠️ These live in the evaluator rather than in a registry, so no amount of function whitelisting
 * removes them — they have to be left out as <em>operators</em>. {@code in} is wrong for a
 * one-element collection and binds unexpectedly against comparison; {@code minusDays} mishandles
 * null. In a form field a silently wrong answer is cosmetic. In an authorization rule it is a
 * hole.</p>
 *
 * <p>Arithmetic goes too. Nothing an authorization predicate needs is arithmetic, and {@code **} is
 * a cheap way to hang a request.</p>
 *
 * <h2>What is left, and why it is enough</h2>
 *
 * <p>Literals, property access, {@code == != &gt; &gt;= &lt; &lt;=} and their word aliases,
 * {@code and} / {@code or} / {@code !}, {@code ??}, the ternary, and the {@code is} tests. An
 * authorization predicate compares something it was given to something written in the file; that is
 * the whole job.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ConditionDialect implements Extension {

    /**
     * The six tests the restricted dialect admits, chosen one by one.
     *
     * <p>They are stateless, so one instance apiece serves every dialect and every evaluation.
     */
    private static final List<Test> BUILT_IN_TESTS = List.of(
            new NullTest(),
            new StartsTest(),
            new EndsTest(),
            new HasAllTest(),
            new HasAnyTest(),
            new HasNoneTest()
    );

    private final FunctionCatalog functions;
    private final TestCatalog     tests;

    /** The dialect a product contributing nothing gets — byte for byte what it always was. */
    public ConditionDialect() {
        this(FunctionCatalog.empty(), TestCatalog.empty());
    }

    public ConditionDialect(FunctionCatalog functions) {
        this(functions, TestCatalog.empty());
    }

    public ConditionDialect(FunctionCatalog functions, TestCatalog tests) {
        this.functions = functions == null ? FunctionCatalog.empty() : functions;
        this.tests     = tests == null ? TestCatalog.empty() : tests;
    }

    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return List.of(
                new JavaBeanAttributeResolver(),
                new MapAttributeResolver(),
                new ListAttributeResolver()
        );
    }

    /**
     * Exactly what a product contributed as an {@link AccessFunction}, and nothing else.
     *
     * <p>⚠️ This is the whitelist. {@code class(…)}, {@code set(…)} and the reflection function are
     * absent not because they are filtered out but because nothing implements the marker for them —
     * which is a stronger guarantee than a deny-list that has to be kept in step with
     * {@code jmouse-el}'s catalogue.
     *
     * @return the contributed functions, empty where there are none
     */
    @Override
    public List<Function> getFunctions() {
        return functions.functions();
    }

    /**
     * No filters at all — one of them touches I/O and none of them answer a question a rule asks.
     *
     * @return an empty list
     */
    @Override
    public List<Filter> getFilters() {
        return List.of();
    }

    /**
     * The six chosen built-ins, plus whatever this installation contributed as an {@link AccessTest}.
     *
     * <p>The six are listed one by one rather than taken wholesale, and that has always been the point:
     * a dialect that admitted every test {@code jmouse-el} ships would be a dialect nobody chose.
     * {@link TestCatalog} keeps the same bargain for the contributed half — it collects
     * {@code AccessTest} and never the raw {@link Test}, so a bean some unrelated module happens to
     * expose does not become authorization vocabulary.
     */
    @Override
    public List<Test> getTests() {
        List<Test> available = new ArrayList<>(BUILT_IN_TESTS);

        available.addAll(tests.tests());

        return available;
    }

    /**
     * The names of the built-ins, for the load-time check — which must accept {@code x is null} while
     * refusing {@code x is nulll}.
     *
     * <p>Derived from the instances above rather than written out a second time: a hand-kept list beside
     * them is a list that goes one commit stale and starts refusing a test that works.
     */
    public static Set<String> builtInTestNames() {
        return BUILT_IN_TESTS.stream().map(Test::getName).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Parsers enough to read an expression, and none of the ones that reach a bean, call a function,
     * build a lambda or bind a variable.
     *
     * <p>{@link ArgumentsParser} is here because a test without arguments is barely a test —
     * {@code is starts('admin')} and {@code is hasAny('DRAFT', 'LIVE')} are the useful half of what
     * {@link #getTests()} offers, and both are unreadable without it. It opens nothing: every
     * argument is parsed by the same {@link ExpressionParser} as the rest of the condition, so an
     * argument can hold exactly what a condition can hold.</p>
     *
     * <p>⚠️ {@link FunctionParser} is here for the same reason and opens no more than
     * {@link ArgumentsParser} did — it turns {@code name(args)} into a call and resolves nothing. What
     * a call may resolve to is {@link #getFunctions()}'s answer, and that is where the whitelist is.
     * Still absent: the bean, lambda and variable-binding parsers.</p>
     *
     * @return the parsers a condition may be built from
     */
    @Override
    public List<Parser> getParsers() {
        return List.of(
                new ExpressionParser(),
                new OperatorParser(),
                new PrimaryExpressionParser(),
                new PropertyParser(),
                new LiteralParser(),
                new TestParser(),
                new FunctionParser(),
                new ParenthesesParser(),
                new ArgumentsParser()
        );
    }

    @Override
    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>(Arrays.asList(ComparisonOperator.values()));

        operators.add(LogicalOperator.AND);
        operators.add(LogicalOperator.OR);
        operators.add(LogicalOperator.NOT);
        operators.add(NullCoalesceOperator.NULL_COALESCE);
        operators.add(TestOperator.IS);

        return operators;
    }
}
