package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.SourceWriter;

/**
 * {@code collection labels on issue_id value name} — many rows per row, and which column holds each.
 *
 * <p>Labels, tags, watchers, links: the shape every product has and none of them spells the same.</p>
 *
 * <h2>⚠️ Why this is not a bag, and not a join</h2>
 *
 * <table>
 *   <caption>Three shapes, three rules</caption>
 *   <tr><th>bag</th><td>many rows, each saying <em>which</em> attribute it is — one value per attribute,
 *       so it maps to an expression</td></tr>
 *   <tr><th>join</th><td>exactly one row on the other side — one value, so it maps to an expression</td></tr>
 *   <tr><th>collection</th><td>many rows, no key column, <strong>no single value</strong> — so it maps to
 *       no expression at all</td></tr>
 * </table>
 *
 * <p>⚠️ That is the whole reason it is declared separately. Reached through a join, a row with three
 * labels would come back <strong>three times</strong>, and every count over that result would be wrong
 * while looking entirely reasonable. A collection is therefore asked about only with a test —
 * {@code issue.labels is hasAny(['regression'])} — which compiles to {@code EXISTS} and multiplies
 * nothing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class CollectionNode extends AbstractExpression {

    private String table;
    private String foreignKey;
    private String valueColumn;

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    /** The column pointing back at the row these belong to. */
    public String getForeignKey() {
        return foreignKey;
    }

    public void setForeignKey(String foreignKey) {
        this.foreignKey = foreignKey;
    }

    /** The column holding one item. */
    public String getValueColumn() {
        return valueColumn;
    }

    public void setValueColumn(String valueColumn) {
        this.valueColumn = valueColumn;
    }

    @Override
    public String toSource() {
        return "collection %s on %s value %s".formatted(
                SourceWriter.name(table), SourceWriter.name(foreignKey), SourceWriter.name(valueColumn));
    }

    @Override
    public String toString() {
        return toSource();
    }
}
