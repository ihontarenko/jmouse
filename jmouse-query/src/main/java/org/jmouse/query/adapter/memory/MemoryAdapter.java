package org.jmouse.query.adapter.memory;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.query.adapter.Capabilities;
import org.jmouse.query.adapter.QueryAdapter;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.el.function.Rewriter;
import org.jmouse.query.el.node.ColumnsNode;
import org.jmouse.query.el.node.OrderNode;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.schema.QuerySchema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A second backend — the same query, run over a list of maps.
 *
 * <h2>⚠️ Why this exists, and it is not only for tests</h2>
 *
 * <p>It is the thing that proves the language is not SQL wearing a hat. A query written once and run
 * against both a database and a list of rows either means the same thing in both or it does not, and
 * until there were two backends nobody could tell. It is also how the language can be exercised with no
 * database at all — which is what a builder's preview, a validation pass and a unit test each need.</p>
 *
 * <h2>⚠️ It refuses more than the SQL adapter, on purpose</h2>
 *
 * <p>No {@code group}, no {@code join}. Both are honest: there is nothing to join a flat list to, and
 * aggregating in memory is real work that has not been done. Declaring them and returning ungrouped rows
 * would be the exact failure {@link Capabilities} exists to prevent.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class MemoryAdapter implements QueryAdapter<MemoryAdapter.Query> {

    private static final Capabilities CAPABILITIES = Capabilities.of("memory",
            Capabilities.Feature.FILTER,
            Capabilities.Feature.SORT,
            Capabilities.Feature.PROJECT,
            Capabilities.Feature.CONVERT,
            Capabilities.Feature.CLOCK);

    private final QuerySchema         schema;
    private final ExpressionLanguage  language;
    private final Map<String, Object> values;

    public MemoryAdapter(QuerySchema schema) {
        this(schema, new QueryLanguage().expressionLanguage());
    }

    public MemoryAdapter(QuerySchema schema, ExpressionLanguage language) {
        this(schema, language, Map.of());
    }

    /**
     * ⚠️ The same values the SQL side binds — {@code currentMember}, {@code blockedIds} — and the same
     * names. Here they become variables in the evaluation context rather than parameters in a statement,
     * which is exactly the point: <strong>one query text, two mechanisms, one meaning.</strong>
     *
     * <p>A name that is neither an attribute nor a supplied value is refused, as it is by the checker.</p>
     *
     * @param values what the caller supplies by name
     */
    public MemoryAdapter(QuerySchema schema, ExpressionLanguage language, Map<String, Object> values) {
        this.schema = schema;
        this.language = language;
        this.values = Map.copyOf(values);
    }

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public Query compile(QueryBlockNode block) {
        requireSupport(block);

        Names names = new Names();

        Expression where = block.getWhere().map(clause -> names.rewrite(clause.getCondition())).orElse(null);
        List<OrderNode.Key> order = new ArrayList<>();
        List<ColumnsNode.Projection> columns = new ArrayList<>();

        block.getOrder().ifPresent(clause -> clause.getKeys().forEach(key ->
                order.add(new OrderNode.Key(names.rewrite(key.expression()), key.direction()))));

        block.getColumns().ifPresent(clause -> clause.getProjections().forEach(projection ->
                columns.add(new ColumnsNode.Projection(names.rewrite(projection.expression()),
                        projection.alias()))));

        return new Query(language, names, values, where, order, columns);
    }

    /**
     * Renames every attribute reference to a plain variable, and remembers which was which.
     *
     * <p>⚠️ <strong>Necessary because {@code entry[quantity]} is a path, not a name.</strong> Evaluating
     * it would send the engine looking for a map called {@code entry} — so each reference becomes
     * {@code v1}, {@code v2} … and the row's values are put in the context under those. The rewrite uses
     * the same {@link Rewriter} the function inliner does, so a node kind added later refuses here too
     * rather than silently vanishing from a condition.</p>
     */
    private final class Names extends Rewriter {

        private final Map<String, String> variables = new LinkedHashMap<>();

        @Override
        public Expression visitProperty(PropertyNode property) {
            // ⚠️ A supplied value passes through UNRENAMED: it is not read off a row, so it has no column
            // to be renamed to — it is put into the context under the name the query wrote.
            if (values.containsKey(property.getPath())) {
                return property;
            }

            schema.attribute(property.getPath()).orElseThrow(() -> new IllegalArgumentException(
                    "there is nothing called '%s' here".formatted(property.getPath())));

            return new PropertyNode(variables.computeIfAbsent(
                    property.getPath(), ignored -> "v" + (variables.size() + 1)));
        }

        Map<String, String> variables() {
            return variables;
        }
    }

    /** A compiled in-memory query — a filter, a sort and a projection over a list of maps. */
    public record Query(ExpressionLanguage language, Names names, Map<String, Object> values,
                        Expression where, List<OrderNode.Key> order,
                        List<ColumnsNode.Projection> columns) {

        /**
         * Runs it.
         *
         * @param rows the rows, each keyed by the name a query writes — {@code entry[quantity]}
         * @return what the query asked for
         */
        public List<Map<String, Object>> run(List<Map<String, Object>> rows) {
            List<Map<String, Object>> kept = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                if (where == null || Boolean.TRUE.equals(evaluate(where, row, Boolean.class))) {
                    kept.add(row);
                }
            }

            for (int index = order.size() - 1; index >= 0; index--) {
                OrderNode.Key key = order.get(index);
                Comparator<Map<String, Object>> comparator = Comparator.comparing(
                        row -> evaluate(key.expression(), row, Comparable.class),
                        Comparator.nullsLast(Comparator.naturalOrder()));

                kept.sort(key.direction() == OrderNode.Direction.DESCENDING
                        ? comparator.reversed()
                        : comparator);
            }

            if (columns.isEmpty()) {
                return kept;
            }

            List<Map<String, Object>> projected = new ArrayList<>();

            for (Map<String, Object> row : kept) {
                Map<String, Object> tuple = new LinkedHashMap<>();
                int position = 0;

                for (ColumnsNode.Projection projection : columns) {
                    position++;

                    String label = projection.alias() != null
                            ? projection.alias()
                            : "column" + position;

                    tuple.put(label, evaluate(projection.expression(), row, Object.class));
                }

                projected.add(tuple);
            }

            return projected;
        }

        @SuppressWarnings("unchecked")
        private <T> T evaluate(Expression expression, Map<String, Object> row, Class<T> type) {
            EvaluationContext context = language.newContext();

            names.variables().forEach((path, variable) -> context.setValue(variable, row.get(path)));

            // ⚠️ Set AFTER the row, and they cannot collide: a name that is a supplied value was never
            // renamed to a `v…` variable, and one that is both is refused before anything is compiled.
            values.forEach(context::setValue);

            Object value = expression.evaluate(context);

            return value == null ? null : (T) value;
        }
    }
}
