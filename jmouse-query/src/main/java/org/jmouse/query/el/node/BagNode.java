package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.SourceWriter;

/**
 * {@code bag field_entries on form_entry_id key field_id value text_value} — a table of loose values.
 *
 * <p>Four names, and the shape does not vary between products: a table, a column pointing back at the
 * row a value belongs to, a column saying <em>which</em> attribute a row is, and a column holding the
 * value itself.</p>
 *
 * <p>⚠️ <strong>The key column may hold an id rather than a name</strong>, and nothing here needs to
 * care. Innoventa's {@code field_entries.field_id} points at another table entirely; each attribute's
 * {@code from} carries whatever the store uses, and this only says which column to compare it against.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BagNode extends AbstractExpression {

    private String table;
    private String foreignKey;
    private String keyColumn;
    private String valueColumn;

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getForeignKey() {
        return foreignKey;
    }

    public void setForeignKey(String foreignKey) {
        this.foreignKey = foreignKey;
    }

    public String getKeyColumn() {
        return keyColumn;
    }

    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public String getValueColumn() {
        return valueColumn;
    }

    public void setValueColumn(String valueColumn) {
        this.valueColumn = valueColumn;
    }

    @Override
    public String toSource() {
        return "bag: %s on %s key %s value %s".formatted(
                SourceWriter.name(table), SourceWriter.name(foreignKey),
                SourceWriter.name(keyColumn), SourceWriter.name(valueColumn));
    }

    @Override
    public String toString() {
        return toSource();
    }
}
