package org.jmouse.query.sql;

import org.jmouse.core.MimeParser;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.query.el.QueryFunctions;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.ExpressionVisitor;
import org.jmouse.el.node.Expressions;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.*;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.el.node.expression.unary.NegateUnaryOperation;
import org.jmouse.query.schema.QueryAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

/**
 * Turns a parsed jMQ expression into SQL and the values that go with it.
 *
 * <h2>⚠️ Every node returns its own {@link Fragment}, and that is what makes the ordering safe</h2>
 *
 * <p>Parameters bind by position, so a value has to appear in the list exactly where its {@code ?}
 * appears in the text. An earlier version accumulated values in the context and drained them per clause,
 * and it had a real bug: a sort key's values were appended in a separate step from its text, so a key
 * that bound a value dropped it and shifted every remaining parameter one place. The statement ran and
 * answered something else.</p>
 *
 * <p>With each node returning a pair, a parent combines children in the order it writes them and the
 * values follow by construction. ⚠️ <strong>That class of bug is not fixed here so much as made
 * unrepresentable</strong> — there is no second place where text and values could fall out of step.</p>
 *
 * <h2>⚠️ Three-valued logic is the specification, not a side effect</h2>
 *
 * <p>SQL's own semantics win and this adds nothing of its own. {@code NOT (text LIKE …)} over a row with
 * no such field yields unknown and the row is excluded — which is exactly what the language promises,
 * and why a builder offers an explicit <em>and those with no such field?</em> switch. A compiler that
 * helpfully wrapped things in {@code COALESCE} would make that switch a lie.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SqlCompiler implements ExpressionVisitor<Fragment> {

    /** Converter filters meaning "read this text as a number", and which reading each asks for. */
    private static final Map<String, NumericKind> NUMERIC_CONVERTERS = Map.of(
            "int", NumericKind.WHOLE,
            "long", NumericKind.WHOLE,
            "short", NumericKind.WHOLE,
            "byte", NumericKind.WHOLE,
            "bigInt", NumericKind.WHOLE,
            "double", NumericKind.FRACTIONAL,
            "float", NumericKind.FRACTIONAL,
            "bigDecimal", NumericKind.FRACTIONAL);

    /**
     * ⚠️ The interval unit each duration function writes. A closed map owned by the compiler, because
     * neither database accepts a parameter where a unit goes — it is syntax — so it must never be able to
     * come from a query's text.
     */
    private static final Map<String, String> UNITS = Map.of(
            "seconds", "SECOND",
            "minutes", "MINUTE",
            "hours", "HOUR",
            "days", "DAY",
            "weeks", "WEEK",
            "months", "MONTH",
            "years", "YEAR");

    /**
     * ⚠️ What each operator token becomes in SQL — <strong>by token, not by enum</strong>. See
     * {@link #sqlOperator(Operator)}: an operator's identity is the token every layer of the engine
     * already agrees on, and keying this by an implementing class made a dialect's own operator
     * meaningless to the compiler for no reason a query author could have understood.
     */
    private static final Map<Token.Type, String> SQL_OPERATORS = Map.ofEntries(
            Map.entry(BasicToken.T_EQ, "="),
            Map.entry(BasicToken.T_NE, "<>"),
            Map.entry(BasicToken.T_GT, ">"),
            Map.entry(BasicToken.T_GE, ">="),
            Map.entry(BasicToken.T_LT, "<"),
            Map.entry(BasicToken.T_LE, "<="),
            Map.entry(BasicToken.T_AND, "AND"),
            Map.entry(BasicToken.T_OR, "OR"),
            Map.entry(BasicToken.T_PLUS, "+"),
            Map.entry(BasicToken.T_MINUS, "-"),
            Map.entry(BasicToken.T_MULTIPLY, "*"),
            Map.entry(BasicToken.T_DIVIDE, "/"),
            Map.entry(BasicToken.T_PERCENT, "%"));

    /**
     * A converter that takes a value apart with a literal separator — and how each database writes it.
     *
     * <p>⚠️ A method reference on {@link Dialect}, so the pair is a <strong>table</strong> and not an
     * if-chain comparing the same name three times to choose between two calls. Adding {@code | part}
     * is then one method on the dialect and one line here, which is also the only shape in which the
     * question "is this translatable exactly?" gets asked once per filter rather than per branch.</p>
     */
    @FunctionalInterface
    private interface Separated {

        String written(Dialect dialect, String expression, String separator);
    }

    /**
     * ⚠️ Admitted where {@code | split} is refused, because these two translate <em>exactly</em> on both
     * databases and {@code split}'s regular-expression contract does not. The separator is bound, never
     * written into the SQL.
     */
    private static final Map<String, Separated> SEPARATED = Map.of(
            "before", Dialect::before,
            "after", Dialect::after);

    /**
     * Converters that change what a value is <em>in Java</em> and nothing about how it is read here.
     *
     * <p>⚠️ Not the same as "unsupported": {@code | string} over a column is already the column, so
     * writing a {@code CAST} would be noise the database has to parse and a person has to read past.</p>
     */
    private static final Set<String> UNWRITTEN = Set.of("string");

    /** Where the wildcards go for a test that asks about part of a text. */
    private record Match(String before, String after) {
    }

    /**
     * ⚠️ The three text tests, as data. They differ in nothing else — same call, same binding, same
     * dialect method — so the two strings ARE the whole difference, and a table says that where three
     * near-identical branches said it three times.
     */
    private static final Map<String, Match> MATCHES = Map.of(
            "contains", new Match("%", "%"),
            "starts", new Match("", "%"),
            "ends", new Match("%", ""));

    /**
     * ⚠️ Not in {@link #MATCHES} and never will be: it takes no argument, binds nothing, and becomes
     * {@code IS NULL} rather than a comparison — because {@code = ?} with a null value is never true in
     * SQL, not even for a null column.
     */
    private static final String NULL_TEST = "null";

    /**
     * ⚠️ What an empty list compiles to — the honest answer to "is x one of nothing".
     *
     * <p>Named because two places produce it and both must agree: an empty array written in the query,
     * and an empty list handed in as a value. {@code IN ()} is a syntax error in both databases.</p>
     */
    private static final String EMPTY_LIST = "1 = 0";

    /** Its opposite — {@code hasNone} of nothing is true of every row. */
    private static final String EVERY_ROW = "1 = 1";

    private final AttributeMapping  mapping;
    private final MembershipMapping membership;
    private final SqlContext        context;

    /**
     * ⚠️ One compiler per compile, holding its context.
     *
     * <p>A visitor returning a value has nowhere to thread an extra argument through, so the context is
     * a field — which makes an instance single-use. Cheap to create, and the alternative (a context
     * passed in every method) would put it in fourteen signatures to be forgotten in one.</p>
     */
    public SqlCompiler(AttributeMapping mapping, SqlContext context) {
        this(mapping, null, context);
    }

    /**
     * @param membership how a collection is asked about, or {@code null} where the source declares none
     */
    public SqlCompiler(AttributeMapping mapping, MembershipMapping membership, SqlContext context) {
        this.mapping = mapping;
        this.membership = membership;
        this.context = context;
    }

    /**
     * Compiles one expression.
     *
     * @param expression what to compile
     * @return the SQL, and the values it binds, in order
     */
    public Fragment compile(Expression expression) {
        return Expressions.walk(expression, this);
    }

    /** The database this compiler is writing for — a caller quoting an alias needs the same one. */
    public Dialect dialect() {
        return context.dialect();
    }

    @Override
    public Fragment visitLiteral(LiteralNode<?> literal) {
        if (literal instanceof StringLiteralNode text) {
            return bound(MimeParser.unquote(text.getValue()));
        }

        // ⚠️ A null literal is written, not bound. `= ?` with a null value is never true in SQL, so the
        // comparison has to become `IS NULL` — which visitBinary can only see if the null is visible here.
        return literal.getValue() == null ? Fragment.of("NULL") : bound(literal.getValue());
    }

    /**
     * An attribute reference — whatever the product says it reads as.
     *
     * <p>⚠️ The result is <strong>raw</strong>: for a bag it is the text column, untyped. Making it a
     * number is the converter's job and refusing to compare it without one is the checker's. Doing
     * either here would put one decision in two places.</p>
     */
    @Override
    public Fragment visitProperty(PropertyNode property) {
        // ⚠️ A name the CALLER supplied — `currentMember`, a tenant, a threshold — is a value, not
        // something read off the row, so it is bound. Asked first because a value is the caller's own
        // word: if it could be shadowed by an attribute, which one a query meant would depend on the
        // schema rather than on what the caller said. The checker refuses a name that is both.
        if (context.hasValue(property.getPath())) {
            return supplied(property.getPath(), context.value(property.getPath()));
        }

        QueryAttribute attribute = context.schema().attribute(property.getPath())
                .orElseThrow(() -> new SqlCompileException(
                        "there is nothing called '%s' here".formatted(property.getPath())));

        return mapping.expression(attribute, context);
    }

    /**
     * A converter pipe.
     *
     * <p>⚠️ Only the numeric readings mean anything to a data source. {@code | upper} and friends are not
     * registered by the dialect precisely because no two backends can be relied on to translate them
     * identically — so anything else is refused rather than compiled into a guess.</p>
     */
    @Override
    public Fragment visitFilter(FilterNode filter) {
        Fragment    inner = compile(filter.getLeft());
        NumericKind kind  = NUMERIC_CONVERTERS.get(filter.getName());

        // ⚠️ A converter over something the schema ALREADY calls a number is a no-op, and it has to be.
        // The guard is a text guard — it tests the value against a digits-only pattern before casting —
        // and a DECIMAL column renders as "240.000000000000000", which that pattern rejects. The result
        // was a query that ran, matched nothing, and looked like an honest empty result. A person writing
        // `| int` out of habit on a numeric column must not be punished for it.
        if (kind != null && isDeclaredNumber(filter.getLeft())) {
            return inner;
        }

        // ⚠️ `substitute`, not `formatted`. A guarded cast names the value TWICE, so an inner expression
        // that binds a parameter has to bind it twice — once per occurrence. Against a bare column the
        // difference is invisible; it appears the first time somebody writes `| before("|") | int`.
        if (kind == NumericKind.WHOLE) {
            return inner.substitute(context.dialect().textAsIntegerTemplate());
        }

        if (kind == NumericKind.FRACTIONAL) {
            return inner.substitute(context.dialect().textAsDecimalTemplate());
        }

        if (UNWRITTEN.contains(filter.getName())) {
            return inner;
        }

        Separated separated = SEPARATED.get(filter.getName());

        if (separated != null) {
            Fragment separator = bound(argument(filter.getArguments()));

            return new Fragment(
                    separated.written(context.dialect(), inner.sql(), separator.sql()),
                    join(inner.parameters(), separator.parameters()));
        }

        throw new SqlCompileException(
                ("'| %s' has no meaning in a query — a data source cannot be relied on to read it the "
                 + "same way this engine would").formatted(filter.getName()));
    }

    @Override
    public Fragment visitTest(TestNode test) {
        return negated(test, written(test));
    }

    /**
     * The test itself, before negation.
     *
     * <h2>⚠️ Split out so that {@code is not} cannot be forgotten by one branch</h2>
     *
     * <p>It was: a membership question returned <strong>early</strong>, past the line that wraps a
     * negated test in {@code NOT (…)}. So {@code labels is not hasAny(['x'])} compiled to plain
     * {@code EXISTS (…)} and answered the exact opposite — silently, on every row, which is the one
     * failure this cluster exists to prevent. One return point is what makes that unrepresentable
     * rather than merely fixed.</p>
     */
    private Fragment written(TestNode test) {
        Fragment asked = membership(test);

        if (asked != null) {
            return asked;
        }

        Fragment left  = compile(test.getLeft());
        Match    match = MATCHES.get(test.getName());

        // ⚠️ A table rather than three switch arms: `contains`, `starts` and `ends` are one question
        // asked with the wildcards in different places, and written as arms the difference between them
        // was two string literals buried in otherwise identical lines.
        if (match != null) {
            return like(left, match, test);
        }

        if (NULL_TEST.equals(test.getName())) {
            return new Fragment("%s IS NULL".formatted(left.sql()), left.parameters());
        }

        throw new SqlCompileException(
                "'is %s' has no meaning in a query".formatted(test.getName()));
    }

    /** {@code is not …} — one place, so no branch can answer the opposite of what it was asked. */
    private Fragment negated(TestNode test, Fragment written) {
        return test.isNegated() ? written.wrap("NOT (", ")") : written;
    }

    /**
     * ⚠️ The pattern is built from a <strong>bound</strong> value, never written into the SQL.
     * {@code LIKE} is the one place a value has to be decorated before binding, and decorating it in Java
     * keeps the {@code %} on the value's side of the boundary.
     */
    /**
     * {@code is contains(…)} and its two siblings — a bound pattern, never a written one.
     *
     * <p>⚠️ The wildcards go INTO the value, not into the SQL. Written into the text they would be one
     * concatenation away from a query somebody typed, and a value containing a `%` would quietly become
     * a wildcard of its own.</p>
     */
    private Fragment like(Fragment left, Match match, TestNode test) {
        Object   argument = argument(test.getArguments());
        Fragment pattern  = bound(match.before() + argument + match.after());

        return new Fragment(
                context.dialect().caseInsensitiveLike(left.sql(), pattern.sql()),
                join(left.parameters(), pattern.parameters()));
    }

    /** {@code x in […]} — one placeholder per element, so nothing is ever concatenated. */
    @Override
    public Fragment visitMembership(InOperationNode membership) {
        // ⚠️ Asked before anything is compiled: "is x one of nothing" is answerable without knowing what
        // x is, and `IN ()` is a syntax error in both databases. It binds no values either, so it cannot
        // shift the parameters around it.
        if (isEmptyList(membership.getRight())) {
            return Fragment.of(EMPTY_LIST);
        }

        Fragment left = compile(membership.getLeft());

        // ⚠️ `x in someView` — a named view standing in for a set. Looked up before the right side is
        // compiled, because compiled as an expression it is a bare name the checker already refused.
        Optional<Fragment> subquery = subquery(membership.getRight());

        if (subquery.isPresent()) {
            return new Fragment(
                    "%s IN (%s)".formatted(left.sql(), subquery.get().sql()),
                    join(left.parameters(), subquery.get().parameters()));
        }

        if (membership.getRight() instanceof ArrayNode array) {
            List<Fragment> items = items(array);

            // ⚠️ An empty list is `IN ()`, a syntax error in both databases. `1 = 0` is the same question
            // honestly answered: nothing is a member of nothing.
            if (items.isEmpty()) {
                return Fragment.of(EMPTY_LIST);
            }

            Fragment inside = combine(items, ", ");

            return new Fragment(
                    "%s IN (%s)".formatted(left.sql(), inside.sql()),
                    join(left.parameters(), inside.parameters()));
        }

        Fragment right = compile(membership.getRight());

        return new Fragment(
                "%s IN (%s)".formatted(left.sql(), right.sql()),
                join(left.parameters(), right.parameters()));
    }

    /**
     * The inner {@code SELECT} a named view stands for, where the right side names one.
     *
     * <h2>⚠️ A view, never an anonymous nested block</h2>
     *
     * <p>The inner question has a name, so it can be opened, tested and reviewed on its own — and the AST
     * stays a flat list of blocks, with no depth to limit and no recursion to guard.</p>
     *
     * <p>⚠️ Empty when the name is not a declared view, and the caller carries on as before. A name that is
     * neither a view nor a supplied value was already refused by the checker; what must never happen is
     * this quietly compiling an unknown name into an empty set, which is a query that returns nothing and
     * reports success.</p>
     */
    private Optional<Fragment> subquery(Node right) {
        if (!(right instanceof PropertyNode named)) {
            return Optional.empty();
        }

        return context.subquery(named.getPath());
    }

    @Override
    public Fragment visitFallback(NullSafeFallbackNode fallback) {
        Fragment nullable = compile(fallback.getNullable());
        Fragment otherwise = compile(fallback.getOtherwise());

        return new Fragment(
                "COALESCE(%s, %s)".formatted(nullable.sql(), otherwise.sql()),
                join(nullable.parameters(), otherwise.parameters()));
    }

    @Override
    public Fragment visitNegation(NegateUnaryOperation negation) {
        return compile(negation.getOperand()).wrap("NOT (", ")");
    }

    @Override
    public Fragment visitBinary(BinaryOperation operation) {
        Operator operator = operation.getOperator();

        // ⚠️ Before either side is compiled, for the same reason as the duration below: a list bound where
        // one value belongs produces `x = ?, ?, ?`, which arrives as a driver message about placeholder
        // counts and names nothing the author wrote.
        refuseListAsValue(operation.getLeft());
        refuseListAsValue(operation.getRight());

        // ⚠️ Caught BEFORE either side is compiled. A duration has no meaning as a value — `days(7)` is
        // not the number seven — so it exists only in this shape, and the shape has to be recognised
        // rather than assembled from two compiled halves.
        if (isArithmetic(operator) && operation.getRight() instanceof FunctionNode call
            && QueryFunctions.isDuration(call.getName())) {
            return shift(operation.getLeft(), call, operator.getType() == BasicToken.T_MINUS);
        }

        Fragment left = compile(operation.getLeft());
        Fragment right = compile(operation.getRight());

        // ⚠️ `= NULL` is never true in SQL, not even for a null column. A comparison against a literal
        // null has to become `IS NULL`, or the query silently returns nothing.
        if (operator.getType() == BasicToken.T_EQ && "NULL".equals(right.sql())) {
            return new Fragment("%s IS NULL".formatted(left.sql()), left.parameters());
        }

        if (operator.getType() == BasicToken.T_NE && "NULL".equals(right.sql())) {
            return new Fragment("%s IS NOT NULL".formatted(left.sql()), left.parameters());
        }

        return new Fragment(
                "(%s %s %s)".formatted(left.sql(), sqlOperator(operator), right.sql()),
                join(left.parameters(), right.parameters()));
    }

    @Override
    public Fragment visitArray(ArrayNode array) {
        return combine(items(array), ", ");
    }

    /**
     * A call the language answers to itself — an aggregate, the clock, or a length of time.
     *
     * <p>⚠️ A <strong>duration</strong> reaching here alone is refused: {@code days(7)} is meaningless on
     * its own and only means something added to or subtracted from a moment, which
     * {@link #visitBinary} handles. Compiling it into a bare number here would let
     * {@code issue.points > days(7)} run and compare a story-point count against seven.</p>
     */
    @Override
    public Fragment visitCall(org.jmouse.el.node.expression.FunctionNode call) {
        String name = call.getName();

        if (QueryFunctions.NOW.equals(name)) {
            return bound(context.now());
        }

        if (QueryFunctions.isAggregate(name)) {
            return aggregate(call);
        }

        if (QueryFunctions.isDuration(name)) {
            throw new SqlCompileException(
                    ("'%s(…)' is a length of time and means nothing on its own — "
                     + "add it to or subtract it from a moment, as in \"now() - %s(7)\"")
                            .formatted(name, name));
        }

        throw new SqlCompileException("there is nothing called '%s(…)' here".formatted(name));
    }

    /**
     * {@code count()}, {@code sum(x)}, {@code avg(x)}, {@code min(x)}, {@code max(x)}.
     *
     * <p>⚠️ {@code count()} with nothing in it becomes {@code COUNT(*)} — "how many rows" — while
     * {@code count(x)} counts the rows where {@code x} is not null. They are different questions and SQL
     * spells them differently, so the language does too rather than quietly picking one.</p>
     */
    private Fragment aggregate(org.jmouse.el.node.expression.FunctionNode call) {
        String name = call.getName().toUpperCase();
        List<Fragment> arguments = call.getArguments() instanceof ArgumentsNode node
                ? items(node)
                : List.of();

        if (arguments.isEmpty()) {
            if (!QueryFunctions.COUNT.equalsIgnoreCase(call.getName())) {
                throw new SqlCompileException(
                        "'%s(…)' needs something to work on".formatted(call.getName()));
            }

            return Fragment.of("COUNT(*)");
        }

        return combine(arguments, ", ").wrap(name + "(", ")");
    }

    /**
     * {@code now() - days(7)} — a moment shifted by a length of time.
     *
     * <p>⚠️ The <strong>amount is bound; the unit is written</strong>, and that asymmetry is not a
     * shortcut. Neither database accepts a parameter where an interval unit goes — it is syntax, not a
     * value — so the unit comes from a closed list this compiler owns and can never be user text.</p>
     */
    private Fragment shift(Expression moment, FunctionNode duration, boolean subtract) {
        Fragment shifted = compile(moment);
        Object amount = argument(duration.getArguments());
        Fragment bound = bound(amount);
        String unit = UNITS.get(duration.getName());

        return new Fragment(
                context.dialect().shift(shifted.sql(), bound.sql(), unit, subtract),
                join(shifted.parameters(), bound.parameters()));
    }

    /**
     * {@code issue.labels is hasAny(['regression'])} — a question about a collection, or {@code null}
     * where this test is not one.
     *
     * <h2>⚠️ Caught BEFORE the left side is compiled</h2>
     *
     * <p>A collection has no expression to compile — that is what makes it a collection — so compiling
     * the left first would refuse a perfectly good question with a sentence about reading many values as
     * one. The shape has to be recognised, exactly as {@code now() - days(7)} is.</p>
     *
     * <p>⚠️ The same three tests over anything <em>else</em> fall through untouched: {@code hasAny} over
     * a text column has no meaning in SQL and is refused below, by name.</p>
     */
    private Fragment membership(TestNode test) {
        MembershipMapping.Question question = switch (test.getName()) {
            case "hasAny" -> MembershipMapping.Question.ANY;
            case "hasAll" -> MembershipMapping.Question.ALL;
            case "hasNone" -> MembershipMapping.Question.NONE;
            default -> null;
        };

        if (question == null || !(test.getLeft() instanceof PropertyNode property)) {
            return null;
        }

        QueryAttribute attribute = context.schema().attribute(property.getPath()).orElse(null);

        if (attribute == null || attribute.access() != QueryAttribute.Access.COLLECTION) {
            return null;
        }

        if (membership == null) {
            throw new SqlCompileException(
                    ("'%s' is a collection, and this source was configured without anywhere to look its "
                     + "items up").formatted(property.getPath()));
        }

        // ⚠️ Asked about nothing: `hasAny` and `hasAll` of an empty list are false for every row, and
        // `hasNone` is true for every row — all three without touching the table. Decided here rather
        // than in a mapping, because it is a property of the question and not of where the items live.
        if (isEmptyList(test.getArguments())) {
            return Fragment.of(question == MembershipMapping.Question.NONE ? EVERY_ROW : EMPTY_LIST);
        }

        return membership.membership(attribute, question, askedAbout(test.getArguments()), context);
    }

    /**
     * The values a membership question names, however they were written.
     *
     * <p>⚠️ {@code hasAny(['a', 'b'])} and {@code hasAny('a', 'b')} are the same question, and both
     * spellings exist in the wild — the first because that is how the in-memory test takes a collection,
     * the second because it reads better. Flattened here rather than made a rule people must remember.</p>
     */
    private List<Fragment> askedAbout(Expression arguments) {
        if (arguments instanceof ArrayNode array) {
            return items(array);
        }

        if (arguments instanceof ArgumentsNode written) {
            List<Node> children = written.getChildren();

            if (children.size() == 1 && children.getFirst() instanceof ArrayNode array) {
                return items(array);
            }

            return items(written);
        }

        return arguments == null ? List.of() : List.of(compile(arguments));
    }

    /**
     * A value the caller supplied, as one placeholder — or as several, when it is a list.
     *
     * <h2>⚠️ A list becomes {@code ?, ?, ?}, and it has to</h2>
     *
     * <p>{@code issue.assignee in blockedIds} is the shape a bound parameter takes in every product,
     * and a driver has no notion of "a parameter that is several values": bound as one, a {@code List}
     * either fails or is stringified into something that matches nothing. The count of placeholders is
     * the count of items, decided here where both are known.</p>
     *
     * <p>⚠️ An <strong>empty</strong> list is {@code IN ()} — a syntax error in both databases — so it
     * becomes the honest answer to the same question: nothing is a member of nothing. Written as a
     * fragment that binds no values, it also cannot shift the parameters that follow it.</p>
     *
     * <p>⚠️ And a list where a single value belongs is refused rather than compiled into
     * {@code x = ?, ?, ?}. See {@link #refuseListAsValue}.</p>
     */
    private Fragment supplied(String name, Object value) {
        Collection<?> items = asCollection(value);

        if (items == null) {
            return bound(value);
        }

        // ⚠️ An empty list is NOT handled here, and the first attempt was: returning `1 = 0` as an
        // OPERAND produced `member_id IN (1 = 0)`, which is broken SQL that no test would have caught
        // because it still looks like a condition. Emptiness answers the whole comparison, so it is
        // decided where the comparison is — see visitMembership and membership.
        if (items.isEmpty()) {
            throw new SqlCompileException(
                    ("'%s' was supplied as an empty list, and an empty list is only meaningful as the "
                     + "whole of a membership question — not as one side of a comparison").formatted(name));
        }

        List<String> holes  = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        items.forEach(item -> {
            holes.add("?");
            values.add(item);
        });

        return new Fragment(String.join(", ", holes), values);
    }

    /**
     * Whether this is a list the caller supplied that has nothing in it.
     *
     * <p>⚠️ Written for arguments as well as for one operand: {@code hasAny(blockedIds)} arrives wrapped
     * in an {@code ArgumentsNode}, and unwrapping it here is what stops the empty case being handled in
     * one of the two places and forgotten in the other.</p>
     */
    private boolean isEmptyList(Expression side) {
        Expression written = side;

        if (written instanceof ArgumentsNode arguments
            && arguments.getChildren().size() == 1
            && arguments.getChildren().getFirst() instanceof Expression only) {

            written = only;
        }

        if (!(written instanceof PropertyNode property) || !context.hasValue(property.getPath())) {
            return false;
        }

        Collection<?> items = asCollection(context.value(property.getPath()));

        return items != null && items.isEmpty();
    }

    /** The value as a collection, or {@code null} where it is a single value. */
    private static Collection<?> asCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }

        return value instanceof Object[] array ? List.of(array) : null;
    }

    /**
     * ⚠️ Refuses a list where one value belongs — {@code issue.assignee == blockedIds}.
     *
     * <p>Compiled, it would be {@code c.assignee = ?, ?, ?}: a syntax error in one database and a
     * parameter count mismatch in the other, arriving as a driver message about placeholders that names
     * nothing the author wrote. Refused here it names the value and says which question it fits.</p>
     */
    private void refuseListAsValue(Expression side) {
        if (side instanceof PropertyNode property
            && context.hasValue(property.getPath())
            && asCollection(context.value(property.getPath())) != null) {

            throw new SqlCompileException(
                    ("'%s' is several values, and this compares against one. Ask '… in %s' instead — or "
                     + "'is hasAny(%s)' where the attribute itself holds many")
                            .formatted(property.getPath(), property.getPath(), property.getPath()));
        }
    }

    private boolean isArithmetic(Operator operator) {
        Token.Type type = operator.getType();

        return type == BasicToken.T_PLUS || type == BasicToken.T_MINUS;
    }

    /**
     * The SQL an operator is written as.
     *
     * <h2>⚠️ Keyed by TOKEN, not by which enum happens to implement the operator</h2>
     *
     * <p>This used to switch on {@code instanceof LogicalOperator} and then on the enum constant, which
     * quietly made the compiler depend on <em>one</em> implementation of {@link Operator} rather than on
     * what the operator is. A dialect contributing its own — jMQ registers {@code and}/{@code or} whose
     * only difference is how they are <em>written back out</em> — then compiled to
     * "'AND' has no meaning in a query", which is a sentence about nothing the author did.</p>
     *
     * <p>A token is the identity the whole engine already agrees on: the lexer emits it, the parser
     * dispatches on it, and the extension container keys operators by it. So anything registered for
     * {@code T_AND} means AND here, whoever contributed it.</p>
     */
    private String sqlOperator(Operator operator) {
        String written = SQL_OPERATORS.get(operator.getType());

        if (written == null) {
            throw new SqlCompileException("'%s' has no meaning in a query".formatted(operator.getName()));
        }

        return written;
    }

    private List<Fragment> items(ArgumentsNode arguments) {
        List<Fragment> compiled = new ArrayList<>();

        for (Node child : arguments.getChildren()) {
            if (child instanceof Expression item) {
                compiled.add(compile(item));
            }
        }

        return compiled;
    }

    private Fragment combine(List<Fragment> fragments, String separator) {
        Fragment assembled = Fragment.empty();

        for (Fragment fragment : fragments) {
            assembled = assembled.then(fragment, separator);
        }

        return assembled;
    }

    /**
     * The one argument a {@code contains} takes, as a value rather than as SQL.
     *
     * <p>⚠️ Read directly instead of compiled, because it has to be decorated with {@code %} before it is
     * bound — and a compiled argument is already a {@code ?} with its value out of reach.</p>
     */
    private Object argument(Expression arguments) {
        if (arguments instanceof ArgumentsNode node && !node.getChildren().isEmpty()
            && node.getChildren().getFirst() instanceof Expression first) {
            return literal(first);
        }

        if (arguments != null) {
            return literal(arguments);
        }

        throw new SqlCompileException("this test needs something to compare against");
    }

    private Object literal(Expression expression) {
        if (expression instanceof StringLiteralNode text) {
            return MimeParser.unquote(text.getValue());
        }

        if (expression instanceof LiteralNode<?> value) {
            return value.getValue();
        }

        throw new SqlCompileException("only a fixed value can be compared against here, not an expression");
    }

    /**
     * Whether the schema says this is already a number, so a converter has nothing to do.
     *
     * <p>Only a bare attribute reference is judged. Anything else is an expression whose type this
     * cannot know, and guessing there would be worse than the guard it is trying to avoid.</p>
     */
    private boolean isDeclaredNumber(Expression side) {
        return side instanceof PropertyNode property
               && context.schema().attribute(property.getPath())
                       .map(attribute -> attribute.type() == org.jmouse.query.schema.QueryType.NUMBER)
                       .orElse(false);
    }

    private static Fragment bound(Object value) {
        return new Fragment("?", List.of(value));
    }

    private static List<Object> join(List<Object> first, List<Object> second) {
        List<Object> combined = new ArrayList<>(first);

        combined.addAll(second);

        return combined;
    }

    /** Which of the dialect's two numeric readings a converter asks for. */
    private enum NumericKind {
        WHOLE,
        FRACTIONAL
    }
}
