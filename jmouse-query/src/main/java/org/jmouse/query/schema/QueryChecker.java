package org.jmouse.query.schema;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.ExpressionVisitor;
import org.jmouse.el.node.Expressions;
import org.jmouse.query.el.QueryFunctions;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.*;
import org.jmouse.query.el.node.ColumnsNode;
import org.jmouse.query.el.node.OrderNode;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.el.node.QueryDocumentNode;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a parsed query against the data it names, before anything tries to run it.
 *
 * <p>Two questions, and both have to be answered here rather than by a compiler, because a compiler's
 * refusal arrives in the vocabulary of a data source and this one has to arrive in the vocabulary of the
 * person who typed it:</p>
 *
 * <ol>
 *   <li><strong>Does this attribute exist?</strong> — a name remembered slightly wrong is the most
 *       common mistake there is, and the answer can name the real ones.</li>
 *   <li>⚠️ <strong>Is this comparison typed?</strong> — the one that matters.</li>
 * </ol>
 *
 * <h2>⚠️ Why the second check exists at all</h2>
 *
 * <p>A schemaless bag holds every value as text. Compared as text, {@code "900" > "1000"} is
 * <strong>true</strong>, because {@code "9" > "1"}. So {@code entry[resistance] > 3300} returns the wrong
 * rows on every run, forever, and nothing anywhere reports it — the query is valid, the data is valid,
 * the answer is simply wrong. This is the single failure the whole design is built to prevent, and the
 * cost of preventing it is four characters: {@code | int}.</p>
 *
 * <p>⚠️ It applies to <strong>{@code order} as well as {@code where}</strong>, which is the half that is
 * easy to forget. A list sorted by untyped text puts 900 after 1000 just as surely, and nobody reading
 * the screen has a reason to doubt it.</p>
 *
 * <p>⚠️ It does <strong>not</strong> apply to a real column. Its type comes from the schema, so
 * {@code issue.storyPoints > 5} is already unambiguous — demanding a converter there would teach people
 * the pipe is noise they must sprinkle everywhere, and people who believe that stop reading refusals.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryChecker {

    /**
     * ⚠️ What a declared value stands in as: a promised {@code TEXT} column, so that nothing asks it for
     * a converter. A value arrives already typed — it is a Java object the caller handed over — and the
     * whole converter rule exists for text nobody promised anything about.
     */
    private static final QueryAttribute VALUE =
            new QueryAttribute("<value>", "<value>", QueryType.TEXT, QueryAttribute.Access.COLUMN);

    /** The comparisons that ask which of two values is greater. Equality is not among them. */
    private static final Set<ComparisonOperator> ORDERED = EnumSet.of(
            ComparisonOperator.GT,
            ComparisonOperator.GTE,
            ComparisonOperator.LT,
            ComparisonOperator.LTE);

    private final QuerySchema schema;
    private final Set<String> values;

    public QueryChecker(QuerySchema schema) {
        this(schema, Set.of());
    }

    /**
     * @param values the names the caller will supply as bound values — {@code currentMember} — which are
     *               therefore neither attributes nor typos
     */
    public QueryChecker(QuerySchema schema, Set<String> values) {
        this.schema = schema;
        this.values = Set.copyOf(values);
    }

    /**
     * Checks every view and function in a document.
     *
     * @param document the parsed document
     */
    public void check(QueryDocumentNode document) {
        document.getViews().forEach(this::check);
        document.getFunctions().forEach(this::check);
    }

    /**
     * Checks one block's clauses.
     *
     * @param block a view or a function
     */
    public void check(QueryBlockNode block) {
        block.getWhere().ifPresent(where -> checkCondition(where.getCondition()));

        block.getHaving().ifPresent(having -> check(having.getCondition()));
        block.getGroup().ifPresent(group -> group.getKeys().forEach(this::check));

        block.getOrder().ifPresent(order -> {
            for (OrderNode.Key key : order.getKeys()) {
                // ⚠️ An order key gets the same rule as a condition, with its own wording. Sorting untyped
                // text puts 900 after 1000, and a sorted list carries no hint that it was sorted wrongly.
                requireTypedSorting(key.expression());
                check(key.expression());
            }
        });

        block.getColumns().ifPresent(columns -> {
            for (ColumnsNode.Projection projection : columns.getProjections()) {
                check(projection.expression());
            }
        });
    }

    /**
     * Checks a condition that chooses <strong>rows</strong> — a {@code where}, and equally a bare filter
     * that arrived in a URL, a config value, an annotation or an agent's tool call.
     *
     * <h2>⚠️ The rule belongs to the condition, not to the clause that happens to hold it</h2>
     *
     * <p>An aggregate is a fact about a <em>group</em>, and a row filter has no group for it to be true
     * of yet. That was once checked only where a {@code where} clause was parsed — so the entry point
     * with no clauses around it, {@code compileFilter}, let {@code count() > 3} straight through and
     * built {@code WHERE COUNT(*) > ?}. The database then refused it, four layers away, with a sentence
     * about group functions and nothing about the query somebody typed.</p>
     *
     * <p>⚠️ A bare filter is the <strong>least</strong> trusted input this language takes, so it is the
     * last place a check may be weaker than a document's.</p>
     *
     * @param condition the condition to check
     */
    public void checkCondition(Expression condition) {
        refuseAggregate(condition);
        check(condition);
    }

    /**
     * Checks one expression — every attribute it names, and every comparison it makes.
     *
     * <p>⚠️ Aggregates are allowed here: this is also how {@code having}, {@code columns} and
     * {@code order} are checked, and all three may name one. Use {@link #checkCondition} for anything
     * that chooses rows.</p>
     *
     * @param expression the expression to check
     */
    public void check(Expression expression) {
        if (expression == null) {
            return;
        }

        switch (expression) {
            case PropertyNode property -> requireKnown(property.getPath());

            case BinaryOperation operation -> {
                refuseCollectionAsValue(operation.getLeft());
                refuseCollectionAsValue(operation.getRight());

                if (isOrdered(operation.getOperator())) {
                    String written = operation.getOperator().getSpelling();

                    requireTypedOrdering(operation.getLeft(), written);
                    requireTypedOrdering(operation.getRight(), written);
                }

                check(operation.getLeft());
                check(operation.getRight());
            }

            // ⚠️ A converter's LEFT is checked, but it is no longer bare: reaching it through here means
            // the ordering rule has already been satisfied by the pipe itself.
            case FilterNode filter -> {
                check(filter.getLeft());
                check(filter.getArguments());
            }

            case TestNode test -> {
                check(test.getLeft());
                check(test.getArguments());
            }

            case InOperationNode membership -> {
                check(membership.getLeft());
                check(membership.getRight());
            }

            case NullSafeFallbackNode fallback -> {
                check(fallback.getNullable());
                check(fallback.getOtherwise());
            }

            case TernaryNode ternary -> {
                check(ternary.getCondition());
                check(ternary.getThenBranch());
                check(ternary.getElseBranch());
            }

            case UnaryOperation unary -> check(unary.getOperand());

            case org.jmouse.el.node.expression.FunctionNode call -> check(call.getArguments());

            // ArrayNode extends ArgumentsNode, so both arrive here and both hold their items as children.
            case ArgumentsNode arguments -> {
                for (Node child : arguments.getChildren()) {
                    if (child instanceof Expression item) {
                        check(item);
                    }
                }
            }

            // A literal names nothing and compares nothing.
            default -> {
            }
        }
    }

    /**
     * Refuses a collection where a single value is expected.
     *
     * <h2>⚠️ Because the alternative answers, and answers wrongly</h2>
     *
     * <p>{@code issue.labels == 'regression'} reads perfectly well and cannot mean anything: the row has
     * three labels, and a comparison has one left-hand side. Left to a backend it becomes a join, the row
     * comes back once per label, and every count over that result is quietly wrong.</p>
     *
     * <p>So it is refused here, pointing at the question that <em>can</em> be asked.</p>
     */
    private void refuseCollectionAsValue(Expression side) {
        if (side instanceof PropertyNode property
            && requireKnown(property.getPath()).access() == QueryAttribute.Access.COLLECTION) {

            throw new QueryCheckException(
                    ("'%s' holds many values per row, so it cannot be compared with one. Ask it a "
                     + "question instead: '%s is hasAny([…])', 'is hasAll([…])' or 'is hasNone([…])'")
                            .formatted(property.getPath(), property.getPath()));
        }
    }

    /**
     * Refuses an ordered comparison over an attribute whose type nobody promised.
     *
     * <p>Only a <strong>bare</strong> attribute reference is refused. Anything else — a converter, a
     * literal, an arithmetic expression — either carries a type already or is not an attribute at all.</p>
     */
    private void requireTypedOrdering(Expression side, String operator) {
        if (needsType(side) instanceof PropertyNode property) {
            throw QueryCheckException.untypedComparison(property.getPath(), operator);
        }
    }

    /** The same rule as {@link #requireTypedOrdering}, worded for a sort rather than a comparison. */
    private void requireTypedSorting(Expression key) {
        if (needsType(key) instanceof PropertyNode property) {
            throw QueryCheckException.untypedOrdering(property.getPath());
        }
    }

    /**
     * The bare attribute reference that has no promised type, if this expression is one.
     *
     * <p>Only a <strong>bare</strong> reference qualifies. Anything else — a converter, a literal, an
     * arithmetic expression — either carries a type already or is not an attribute at all.</p>
     */
    private PropertyNode needsType(Expression side) {
        if (side instanceof PropertyNode property && requireKnown(property.getPath()).needsConverterForOrdering()) {
            return property;
        }

        return null;
    }

    /**
     * Looks an attribute up, refusing by name when the schema has never heard of it.
     *
     * <h2>⚠️ A declared VALUE is not an attribute, and not a typo either</h2>
     *
     * <p>{@code issue.assignee == currentMember} names two different kinds of thing: one is read off the
     * row, the other is handed in by whoever runs the query. So a name the caller declared as a value
     * passes here and is bound as a parameter later — and a name that is <strong>both</strong> is refused,
     * because nothing downstream could then say which of the two a query meant.</p>
     */
    private QueryAttribute requireKnown(String path) {
        if (values.contains(path)) {
            schema.attribute(path).ifPresent(clash -> {
                throw new QueryCheckException(
                        ("'%s' is both something this data has and something you are supplying, so there "
                         + "is no way to tell which one a query means; rename the value you pass in")
                                .formatted(path));
            });

            return VALUE;
        }

        return schema.attribute(path)
                .orElseThrow(() -> QueryCheckException.unknownAttribute(path, schema.attributes()));
    }

    /**
     * Refuses an aggregate anywhere inside a condition meant for rows.
     *
     * <p>⚠️ Walks the whole expression rather than checking the top: {@code x == 1 and count() > 3} hides
     * the aggregate one level down, and that is exactly how somebody writes it.</p>
     */
    private void refuseAggregate(Expression expression) {
        Expressions.walk(expression, new ExpressionVisitor<Void>() {

            @Override
            public Void visitCall(org.jmouse.el.node.expression.FunctionNode call) {
                if (QueryFunctions.isAggregate(call.getName())) {
                    throw new QueryCheckException(
                            ("'%s(…)' asks something about a GROUP of rows, and a 'where' filters rows one "
                             + "by one — so there is no group for it to be true of yet. "
                             + "Move it to 'having', which filters the groups afterwards")
                                    .formatted(call.getName()));
                }

                return null;
            }

            @Override
            public Void visitUnsupported(Expression other) {
                return null;
            }
        });

        children(expression).forEach(this::refuseAggregate);
    }

    /** The sub-expressions of anything the walk above does not recurse into itself. */
    private List<Expression> children(Expression expression) {
        return switch (expression) {
            case BinaryOperation operation -> List.of(operation.getLeft(), operation.getRight());
            case FilterNode filter -> filter.getLeft() == null ? List.of() : List.of(filter.getLeft());
            case TestNode test -> test.getLeft() == null ? List.of() : List.of(test.getLeft());
            case InOperationNode membership -> List.of(membership.getLeft(), membership.getRight());
            case NullSafeFallbackNode fallback -> List.of(fallback.getNullable(), fallback.getOtherwise());
            case org.jmouse.el.node.expression.unary.NegateUnaryOperation negation ->
                    List.of(negation.getOperand());
            case null, default -> List.of();
        };
    }

    private boolean isOrdered(Operator operator) {
        return operator instanceof ComparisonOperator comparison && ORDERED.contains(comparison);
    }
}
