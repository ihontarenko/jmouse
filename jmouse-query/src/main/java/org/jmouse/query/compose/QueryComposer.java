package org.jmouse.query.compose;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FilterNode;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.literal.BooleanLiteralNode;
import org.jmouse.el.node.expression.literal.DoubleLiteralNode;
import org.jmouse.el.node.expression.literal.LongLiteralNode;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.query.el.dialect.QueryLogicalOperator;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.schema.QueryType;

import java.util.List;

/**
 * Builder rows in, jMQ out — through the <strong>nodes</strong>, never through a string.
 *
 * <h2>⚠️ Composing is building an AST and asking it to write itself</h2>
 *
 * <p>Every row becomes real {@link Expression} nodes and the text comes from {@code toSource()}. Nothing
 * here concatenates a fragment of the language. That matters for three reasons, and each of them has
 * already cost this codebase a defect:</p>
 *
 * <ul>
 *   <li><strong>Quoting is the node's.</strong> {@code StringLiteralNode} knows a literal carries no
 *       escape sequence and refuses a value holding both kinds of quote rather than emitting text that
 *       will not parse. A template writing {@code '%s'} does not know that.</li>
 *   <li><strong>Spelling is the operator's.</strong> {@code and} rather than {@code &&} comes from
 *       {@link QueryLogicalOperator#getSpelling()}. One place decides, so the un-parse and the parser
 *       cannot disagree about what the language looks like.</li>
 *   <li><strong>What can be composed is what can be recognised.</strong> Both directions live on
 *       {@link RowOperator}, so an operator cannot exist in half.</li>
 * </ul>
 *
 * <h2>⚠️ The converter is placed here, not sent by the caller</h2>
 *
 * <p>An ordered comparison over an untyped value needs one, and forgetting it is the difference between
 * {@code 900 > 1000} being false and {@code "900" > "1000"} being true. So it is decided by
 * {@link ConverterPolicy} against the schema — the same object that told the screen a converter was
 * needed at all.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryComposer {

    private final QuerySchema     schema;
    private final ConverterPolicy converters;

    public QueryComposer(QuerySchema schema, ConverterPolicy converters) {
        this.schema = schema;
        this.converters = converters;
    }

    /**
     * The rows as one condition, joined with {@code and}.
     *
     * <p>⚠️ Rows are joined with {@code and} and there is no choice about it. A builder offering
     * {@code or} between rows is a builder offering precedence, and precedence in a row of controls is
     * invisible — the person cannot see where the brackets went. Anything needing {@code or} is written
     * as text, where the brackets are on the screen.</p>
     *
     * @param rows what the builder holds
     * @return the condition, or {@code null} when there are no usable rows
     */
    public Expression condition(List<ConditionRow> rows) {
        Expression composed = null;

        for (ConditionRow row : rows) {
            Expression written = written(row);

            if (written == null) {
                continue;
            }

            composed = composed == null
                    ? written
                    : new BinaryOperation(composed, QueryLogicalOperator.AND, written);
        }

        return composed;
    }

    /**
     * The rows as jMQ.
     *
     * @param rows what the builder holds
     * @return the filter, or an empty string — ⚠️ never something always true, because an empty builder
     *         means <em>no filter</em> and a listing showing everything should say so by filtering nothing
     */
    public String filter(List<ConditionRow> rows) {
        Expression composed = condition(rows);

        return composed == null ? "" : composed.toSource();
    }

    /**
     * One sort key, with the converter the schema asks for.
     *
     * <p>⚠️ Written here rather than in a screen for the same reason as the filter: a sort over an
     * untyped value without a converter orders words, and a list ordered by {@code "1000"} before
     * {@code "900"} looks sorted.</p>
     *
     * @param attribute  what to sort by, or blank for no sort
     * @param descending which way
     * @return the order clause's body, or an empty string
     */
    public String order(String attribute, boolean descending) {
        if (attribute == null || attribute.isBlank()) {
            return "";
        }

        QueryAttribute described = require(attribute);
        Expression     left      = converted(described, true);

        return "%s %s".formatted(left.toSource(), descending ? "desc" : "asc");
    }

    // ── One row ─────────────────────────────────────────────────────────────────

    private Expression written(ConditionRow row) {
        if (row == null || row.attribute() == null || row.attribute().isBlank()) {
            return null;
        }

        QueryAttribute described = require(row.attribute());
        RowOperators   operator  = RowOperators.spelled(row.operator());

        // ⚠️ An unfinished row is skipped rather than refused: somebody adding a row and not having typed
        // the value yet is the normal state of a builder, not a mistake to shout about.
        if (operator.needsValue() && isBlank(row.value())) {
            return null;
        }

        if (!operator.needsValue() && row.value() != null) {
            throw new ComposeException(
                    "'%s' compares against nothing, so it was given a value it cannot use: %s"
                            .formatted(operator.spelling(), row.value()));
        }

        Expression left    = converted(described, operator.ordered());
        Expression value   = operator.needsValue() ? literal(described, row.value()) : null;
        Expression written = operator.write(left, value);

        return row.includeMissing() && operator.negative() ? orMissing(described, written) : written;
    }

    /**
     * ⚠️ The absence question, and why it is asked in the control rather than answered afterwards.
     *
     * <p>{@code entry[name] is not contains('stm')} leaves out every row that has no {@code name} at all
     * — there is nothing there to not contain it. Most people mean to include those, and discover they
     * did not only by staring at a result they cannot explain.</p>
     */
    private Expression orMissing(QueryAttribute attribute, Expression written) {
        Expression missing = RowOperators.EMPTY.write(reference(attribute), null);

        return new BinaryOperation(missing, QueryLogicalOperator.OR, written);
    }

    private Expression converted(QueryAttribute attribute, boolean ordered) {
        Expression reference = reference(attribute);

        if (!ordered) {
            return reference;
        }

        String converter = converters.converterFor(attribute);

        if (converter == null || converter.isBlank()) {
            return reference;
        }

        FilterNode piped = new FilterNode(converter);

        piped.setLeft(reference);

        return piped;
    }

    private Expression reference(QueryAttribute attribute) {
        return new PropertyNode(attribute.name());
    }

    /**
     * The value as a literal — ⚠️ decided by the <strong>schema</strong>, never by what the value looks
     * like.
     *
     * <p>{@code 0603} is a component name in one of these products, and reading it as a number would
     * silently drop the leading zero. A converter counts as the schema saying <em>number</em>: comparing
     * a converted left-hand side against a quoted string leaves MySQL answering anyway and PostgreSQL
     * refusing outright, which is one query behaving two ways.</p>
     */
    private Expression literal(QueryAttribute attribute, Object value) {
        if (value instanceof Boolean flag) {
            return new BooleanLiteralNode(flag);
        }

        if (attribute.type() == QueryType.BOOLEAN) {
            return new BooleanLiteralNode(Boolean.parseBoolean(String.valueOf(value)));
        }

        if (numeric(attribute)) {
            return number(String.valueOf(value).trim());
        }

        return new StringLiteralNode(String.valueOf(value));
    }

    private boolean numeric(QueryAttribute attribute) {
        return attribute.type() == QueryType.NUMBER || converters.converterFor(attribute) != null;
    }

    /**
     * ⚠️ A value the schema calls numeric but that is not a number stays a string rather than becoming a
     * broken literal. The refusal then comes from the checker, in its own words, about the comparison —
     * which is the sentence somebody can act on.
     */
    private Expression number(String value) {
        try {
            return value.indexOf('.') < 0
                    ? new LongLiteralNode(Long.parseLong(value))
                    : new DoubleLiteralNode(Double.parseDouble(value));
        } catch (NumberFormatException notANumber) {
            return new StringLiteralNode(value);
        }
    }

    private QueryAttribute require(String name) {
        return schema.attribute(name).orElseThrow(() -> new ComposeException(
                "There is nothing called '%s' here.".formatted(name)));
    }

    private boolean isBlank(Object value) {
        return value == null || (value instanceof CharSequence text && text.toString().isBlank());
    }
}
