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

    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return List.of(
                new JavaBeanAttributeResolver(),
                new MapAttributeResolver(),
                new ListAttributeResolver()
        );
    }

    /**
     * No functions at all — the registry is where {@code class(…)} and {@code set(…)} would live.
     *
     * @return an empty list
     */
    @Override
    public List<Function> getFunctions() {
        return List.of();
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

    @Override
    public List<Test> getTests() {
        return List.of(
                new NullTest(),
                new StartsTest(),
                new EndsTest(),
                new HasAllTest(),
                new HasAnyTest(),
                new HasNoneTest()
        );
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
