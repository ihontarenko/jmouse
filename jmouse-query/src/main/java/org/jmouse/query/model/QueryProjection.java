package org.jmouse.query.model;

import org.jmouse.el.node.Expression;
import org.jmouse.query.el.node.ColumnsNode;
import org.jmouse.query.el.node.OrderNode;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.el.node.ViewNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a parsed view into {@link QueryModel} — the third output of one AST.
 *
 * <pre>
 * .jmq text ──parse──▶ AST ──┬── toSource()  ──▶ .jmq text   (the un-parse)
 *                            ├── compile()   ──▶ Fragment    (an adapter)
 *                            └── project()   ──▶ QueryModel  (this)
 * </pre>
 *
 * <h2>⚠️ Every part comes back as the language's own text, not as a structure</h2>
 *
 * <p>A condition becomes {@code "entry[quantity] | int < 5"} rather than a tree of operator nodes. Three
 * reasons, and the first is the one that matters:</p>
 *
 * <ol>
 *   <li><strong>It stays honest.</strong> Text can be handed back to the parser and produce the same
 *       condition; a hand-built JSON tree is a second grammar that has to be kept in step with the real
 *       one, and the two drift the first time an operator is added.</li>
 *   <li>A builder that meets something it cannot draw can show it — which is exactly what decision 1
 *       requires of it.</li>
 *   <li>It is small, and it is readable in a log.</li>
 * </ol>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class QueryProjection {

    private QueryProjection() {
    }

    /**
     * A view, as data.
     *
     * @param view a parsed view
     * @return the model
     */
    public static QueryModel project(ViewNode view) {
        return new QueryModel(
                view.getTitle(),
                view.getTarget(),
                text(view.getWhere().map(clause -> clause.getCondition()).orElse(null)),
                columns(view),
                group(view),
                text(view.getHaving().map(clause -> clause.getCondition()).orElse(null)),
                order(view),
                view.isGrouped());
    }

    private static List<QueryModel.Projection> columns(QueryBlockNode block) {
        List<QueryModel.Projection> projections = new ArrayList<>();

        block.getColumns().ifPresent(clause -> clause.getProjections().forEach(projection ->
                projections.add(new QueryModel.Projection(
                        projection.expression().toSource(), projection.alias()))));

        return projections;
    }

    private static List<String> group(QueryBlockNode block) {
        List<String> keys = new ArrayList<>();

        block.getGroup().ifPresent(clause -> clause.getKeys().forEach(key -> keys.add(key.toSource())));

        return keys;
    }

    private static List<QueryModel.Sort> order(QueryBlockNode block) {
        List<QueryModel.Sort> keys = new ArrayList<>();

        block.getOrder().ifPresent(clause -> clause.getKeys().forEach(key ->
                keys.add(new QueryModel.Sort(
                        key.expression().toSource(),
                        key.direction() == OrderNode.Direction.DESCENDING))));

        return keys;
    }

    private static String text(Expression expression) {
        return expression == null ? null : expression.toSource();
    }
}
