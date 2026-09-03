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
    private String localColumn;

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

    /**
     * The column on the FILTERED row that this bag hangs off — or {@code null} for that row's own key,
     * which is what every ordinary bag wants.
     *
     * <h2>⚠️ The shape it exists for, and why omitting it fails silently</h2>
     *
     * <p>An ordinary bag row points straight at the row being filtered: {@code bag.entry_id = root.id}.
     * A product whose subject area is <em>about</em> something that has a bag has a second shape — an
     * asset is a row of its own carrying a state and a due date, and everything a person recognises it
     * by lives on the entry it describes. There the correlation is
     * {@code field_entries.form_entry_id = assets.form_entry_id}: one hop sideways, not down.</p>
     *
     * <p>⚠️ Left unsaid, such a source parses, compiles, correlates against the wrong column and matches
     * <strong>nothing</strong> — with no error anywhere. Naming it is how the second shape is said out
     * loud rather than worked around by inventing a view, or by giving the source the wrong root, which
     * is the tempting mistake because it compiles and returns rows that are simply about something else.</p>
     */
    public String getLocalColumn() {
        return localColumn;
    }

    public void setLocalColumn(String localColumn) {
        this.localColumn = localColumn;
    }

    @Override
    public String toSource() {
        String written = "bag: %s on %s key %s value %s".formatted(
                SourceWriter.name(table), SourceWriter.name(foreignKey),
                SourceWriter.name(keyColumn), SourceWriter.name(valueColumn));

        /* ⚠️ Written back out, and the round trip is the whole reason this is here rather than only in
           the parser: a clause read and then dropped by the writer describes a source that correlates
           differently from the one the document declared — and the difference is invisible in both. */
        return localColumn == null
                ? written
                : written + " matching " + SourceWriter.name(localColumn);
    }

    @Override
    public String toString() {
        return toSource();
    }
}
