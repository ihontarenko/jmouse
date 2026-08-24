package org.jmouse.query.el.dialect;

import org.jmouse.core.access.AttributeResolver;
import org.jmouse.el.extension.*;
import org.jmouse.el.extension.attribute.JavaBeanAttributeResolver;
import org.jmouse.el.extension.attribute.ListAttributeResolver;
import org.jmouse.el.extension.attribute.MapAttributeResolver;
import org.jmouse.el.extension.filter.AfterFilter;
import org.jmouse.el.extension.filter.BeforeFilter;
import org.jmouse.el.extension.filter.converter.*;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.extension.test.*;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The cut-down expression language a {@code where} is allowed to be written in.
 *
 * <p>Registering this instead of the document extension gives a language that can read a
 * <em>condition</em> and nothing else — no tags, no declarations. It is what an ad-hoc expression
 * arriving from a URL, a header or a text box is parsed against.</p>
 *
 * <h2>⚠️ Confinement is about REACH, not about expressiveness</h2>
 *
 * <p>These are two different questions and this project has already conflated them once. The policy
 * language's {@code ConditionDialect} carries the confession in its own javadoc: it registered no
 * functions <em>and</em> no {@code FunctionParser}, *"which read as one decision and was two. Only one
 * of them was load-bearing."*</p>
 *
 * <table>
 *   <caption>The two axes, kept apart deliberately</caption>
 *   <tr><th>Axis</th><th>jMQ's answer</th></tr>
 *   <tr><td><strong>Reach</strong> — may an expression touch a bean, mutate state, do I/O?</td>
 *       <td>⚠️ <strong>No.</strong> Closed, and not negotiable.</td></tr>
 *   <tr><td><strong>Expressiveness</strong> — what may it <em>say</em> about the data?</td>
 *       <td>As much as a compiler can translate.</td></tr>
 * </table>
 *
 * <p>So arithmetic stays, the converter pipes stay, function <em>syntax</em> stays. A query language
 * that cannot write {@code price | decimal * 1.2} is one every report has to route around, and a
 * language people route around stops being the single place a filter is written.</p>
 *
 * <h2>What is left out, and why each one</h2>
 *
 * <table>
 *   <caption>Removed by registering neither the parser nor the function</caption>
 *   <tr><th>Left out</th><th>Why</th></tr>
 *   <tr><td>{@code @bean.method(args)}, {@code class('fqcn')}</td>
 *       <td>reaches any bean in the container from inside a string somebody typed into a text box.
 *           Nothing a filter asks needs it, and everything an attacker wants starts there</td></tr>
 *   <tr><td>{@code set(…)}, static imports</td>
 *       <td>mutates the evaluation context, so one clause silently changes what the next one sees —
 *           and a {@code where} whose meaning depends on evaluation order cannot be compiled at all</td></tr>
 *   <tr><td>lambdas, scoped calls</td>
 *       <td>no data source can translate either, so admitting them would mean parsing something that is
 *           always refused one stage later, with a worse message</td></tr>
 *   <tr><td>the {@code ..} range operator</td>
 *       <td>reads as a between and is not one; it builds a list, and over a large bound it builds a
 *           large one</td></tr>
 * </table>
 *
 * <h2>⚠️ {@code in} is admitted here, and that is a decision rather than an oversight</h2>
 *
 * <p>{@code ConditionDialect} leaves it out, because it answered wrongly rather than failing: its right
 * operand parsed at precedence zero, so {@code x in list and y == 1} grouped as
 * {@code x in (list and (y == 1))} and returned {@code false} for every row; and a one-element list of
 * one string was expanded into that string's characters.</p>
 *
 * <p>Both were fixed in the engine rather than routed around, because {@code in} is central to what this
 * language is for — {@code entry[owner] in userIds} is the shape a bound parameter takes. Routing around
 * a defect leaves it for the next caller; this cluster was simply the first that could not.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryDialect implements Extension {

    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return List.of(
                new JavaBeanAttributeResolver(),
                new MapAttributeResolver(),
                new ListAttributeResolver()
        );
    }

    /**
     * The type converters, and nothing else.
     *
     * <p>⚠️ <strong>Not a convenience — the answer to a problem neither JQL nor JPQL has.</strong> Both
     * of those read a field's type from a schema. A schemaless bag holds every value as text, so an
     * ordered comparison without a converter compares strings, and {@code "900" > "1000"} is
     * <em>true</em> because {@code "9" > "1"}. The pipe is how a type is declared at the point of use,
     * visibly and locally.</p>
     *
     * <p>⚠️ The string filters — {@code upper}, {@code trim}, {@code split}, {@code join} — are
     * deliberately absent. Not because they are dangerous, but because a data source cannot be relied on
     * to translate them identically, and a filter that means one thing in memory and another in SQL is
     * the failure this whole design exists to prevent. They belong in a later ticket, one at a time,
     * each with its translation agreed.</p>
     */
    @Override
    public List<Filter> getFilters() {
        return List.of(
                new ToBooleanFilter(),
                new ToByteFilter(),
                new ToShortFilter(),
                new ToIntFilter(),
                new ToLongFilter(),
                new ToFloatFilter(),
                new ToDoubleFilter(),
                new ToBigIntFilter(),
                new ToBigDecimalFilter(),
                new ToStringFilter(),
                new ToInstantFilter(),
                // ⚠️ Literal separators, not regexes — see BeforeFilter. These are admitted because their
                // translation is EXACT on both databases; `split` and `first` are not, and stay out.
                new BeforeFilter(),
                new AfterFilter()
        );
    }

    /**
     * The tests a condition may ask.
     *
     * <p>⚠️ {@code even}, {@code odd}, {@code array}, {@code collection}, {@code map}, {@code iterable}
     * and {@code type} are left out: they ask about a value's Java <em>shape</em>, which a row in a
     * database does not have. Admitting them would mean parsing a question no data source can be asked.</p>
     */
    @Override
    public List<Test> getTests() {
        return List.of(
                new NullTest(),
                new StartsTest(),
                new EndsTest(),
                new ContainsTest(),
                new HasAllTest(),
                new HasAnyTest(),
                new HasNoneTest()
        );
    }

    /**
     * The parsers a condition is read with.
     *
     * <p>⚠️ {@link FunctionParser} is registered while <strong>no function is</strong>, and the pair is
     * deliberate. That parser is pure syntax — {@code Identifier Arguments} — and reaches no bean;
     * {@code @bean.method()} is a different parser behind a token this dialect never registers. So the
     * hole lives in the function <em>registry</em>, and leaving the registry empty closes it while
     * keeping the door open for a product to contribute {@code now()} or {@code currentUser()} without
     * this class changing.</p>
     */
    @Override
    public List<Parser> getParsers() {
        return List.of(
                new ExpressionParser(),
                new OperatorParser(),
                new PrimaryExpressionParser(),
                new AutodetectFirstParser(),
                new FunctionParser(),
                new TestParser(),
                new FilterParser(),
                new PropertyParser(),
                new LiteralParser(),
                new ArrayParser(),
                new ParenthesesParser(),
                new ArgumentsParser()
        );
    }

    /**
     * The operators a condition may use.
     *
     * <p>⚠️ {@link UnaryOperator} — {@code ++} and {@code --} — is left out. Both <em>assign</em>, and an
     * expression that changes what it reads cannot be compiled into a query at all.</p>
     */
    @Override
    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>();

        operators.addAll(Arrays.asList(MathematicOperator.values()));
        operators.addAll(Arrays.asList(LogicalOperator.values()));
        operators.addAll(Arrays.asList(ComparisonOperator.values()));

        // ⚠️ AFTER the core pair, and it replaces it: operators are held by token type, so the last
        // registration for T_AND and T_OR wins. `and`/`or` and `&&`/`||` both still parse — this only
        // decides which one a document is written back as, and a query document is read by people who
        // are not writing code. See QueryLogicalOperator.
        operators.addAll(Arrays.asList(QueryLogicalOperator.values()));

        operators.add(FilterOperator.FILTER);
        operators.add(TestOperator.IS);
        operators.add(InOperator.IN);
        operators.add(NullCoalesceOperator.NULL_COALESCE);
        operators.add(ConcatOperator.CONCAT);

        return operators;
    }
}
