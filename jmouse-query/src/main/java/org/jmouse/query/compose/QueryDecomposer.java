package org.jmouse.query.compose;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FilterNode;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.query.el.dialect.QueryLogicalOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * jMQ in, builder rows out — by walking the AST, never by matching text.
 *
 * <h2>⚠️ Empty is a first-class answer, and the most important one</h2>
 *
 * <p>Most of what jMQ can say is not a row of three controls. A ternary, a range, an {@code or} between
 * conditions, a value that is a call or a supplied name — all of it is perfectly good, and none of it can
 * be drawn. For every one of them this answers <strong>empty</strong>, and the caller's job is then to
 * say <em>this was written by hand</em> and hand over the text.</p>
 *
 * <p>⚠️ Guessing instead is worse than useless. A reader that accepted anything on the right of a
 * comparison and a writer that quoted whatever it was handed turned {@code submitter == currentMember}
 * into a comparison against the <em>word</em> — an empty list, no refusal, no clue. That is the defect
 * this class exists to make structurally impossible: the only values it will read back are the ones
 * {@link QueryComposer} can write.</p>
 *
 * <h2>⚠️ Rows join with {@code and}, and a top-level {@code or} is refused</h2>
 *
 * <p>Not because {@code or} is hard, but because a row of controls cannot show precedence. Somebody
 * looking at three rows cannot see where the brackets are, and a builder that redraws
 * {@code a and (b or c)} as three rows will re-emit it as something else.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryDecomposer {

    /**
     * The rows this condition is made of.
     *
     * @param condition a parsed condition, or {@code null} for none
     * @return the rows, or empty when the builder could not have written it
     */
    public Optional<List<ConditionRow>> rows(Expression condition) {
        if (condition == null) {
            return Optional.of(List.of());
        }

        List<Expression> links = new ArrayList<>();

        if (!flatten(condition, links)) {
            return Optional.empty();
        }

        List<ConditionRow> rows = new ArrayList<>();

        for (Expression link : links) {
            ConditionRow row = row(link);

            if (row == null) {
                return Optional.empty();
            }

            rows.add(row);
        }

        return Optional.of(rows);
    }

    /**
     * Splits an {@code and} chain into its links.
     *
     * <p>⚠️ Only {@code and}. A top-level {@code or} answers false, and the whole condition then goes
     * back as text.</p>
     */
    private boolean flatten(Expression condition, List<Expression> links) {
        if (condition instanceof BinaryOperation operation
            && operation.getOperator() == QueryLogicalOperator.AND) {
            return flatten(operation.getLeft(), links) && flatten(operation.getRight(), links);
        }

        if (condition instanceof BinaryOperation operation
            && operation.getOperator() == QueryLogicalOperator.OR) {
            // ⚠️ The one `or` a row can carry: `(x is null or <negative comparison>)`, which is what the
            // absence switch writes. Anything else that ORs is not a row.
            return missing(operation) != null && links.add(condition);
        }

        links.add(condition);

        return true;
    }

    private ConditionRow row(Expression link) {
        Expression comparison = link;
        boolean    includeMissing = false;

        if (link instanceof BinaryOperation operation
            && operation.getOperator() == QueryLogicalOperator.OR) {
            String absent = missing(operation);

            if (absent == null) {
                return null;
            }

            comparison = operation.getRight();
            includeMissing = true;
        }

        for (RowOperators operator : RowOperators.values()) {
            Optional<RowOperator.Reading> reading = operator.read(comparison);

            if (reading.isEmpty()) {
                continue;
            }

            ConditionRow row = row(operator, reading.get(), includeMissing);

            if (row != null) {
                return row;
            }
        }

        return null;
    }

    private ConditionRow row(RowOperators operator, RowOperator.Reading reading, boolean includeMissing) {
        String attribute = attribute(reading.left());

        if (attribute == null) {
            return null;
        }

        if (!operator.needsValue()) {
            return new ConditionRow(attribute, operator.spelling(), null, includeMissing);
        }

        // ⚠️ A literal, or this is not a row. `currentMember` is a supplied value and `now() - days(7)`
        // is an expression; both are good queries and neither is three controls.
        if (!(reading.value() instanceof LiteralNode<?> literal)) {
            return null;
        }

        return new ConditionRow(attribute, operator.spelling(), value(literal), includeMissing);
    }

    /**
     * The attribute a side names — through its converter, if it has one.
     *
     * <p>⚠️ The converter is dropped rather than reported. It is not a fact about the row; it is what the
     * schema said this attribute needs, and {@link QueryComposer} will put back whatever it needs when
     * the row is written out again. Carrying it through the row would give a client the chance to send a
     * different one.</p>
     */
    private String attribute(Expression side) {
        Expression named = side instanceof FilterNode piped ? piped.getLeft() : side;

        return named instanceof PropertyNode property ? property.getPath() : null;
    }

    /**
     * ⚠️ A parsed string literal still carries its quotes — the node keeps the source spelling and
     * unquotes only when evaluated. A row must hold the value a person typed, so they come off here.
     */
    private Object value(LiteralNode<?> literal) {
        Object held = literal.getValue();

        if (literal instanceof StringLiteralNode && held instanceof String text) {
            return unquoted(text);
        }

        return held;
    }

    private String unquoted(String text) {
        if (text.length() < 2) {
            return text;
        }

        char opening = text.charAt(0);

        return (opening == '\'' || opening == '"') && text.charAt(text.length() - 1) == opening
                ? text.substring(1, text.length() - 1)
                : text;
    }

    /** The attribute an {@code (x is null or …)} pair asks about, or {@code null} if it is not one. */
    private String missing(BinaryOperation operation) {
        Optional<RowOperator.Reading> absent = RowOperators.EMPTY.read(operation.getLeft());

        if (absent.isEmpty()) {
            return null;
        }

        String asked = attribute(absent.get().left());

        // ⚠️ Both halves must be about the SAME attribute. `(a is null or b != 1)` is a real query and
        // means something else entirely — drawing it as one row about `b` would lose `a`.
        for (RowOperators operator : RowOperators.values()) {
            Optional<RowOperator.Reading> comparison = operator.read(operation.getRight());

            if (comparison.isPresent() && operator.negative()) {
                return asked != null && asked.equals(attribute(comparison.get().left())) ? asked : null;
            }
        }

        return null;
    }
}
