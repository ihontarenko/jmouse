package org.jmouse.query.sql.mapping;

/**
 * Where a product keeps its loose values, in that product's own spelling.
 *
 * <p>Every bag store is the same four things: a table, a column pointing back at the row it belongs to,
 * a column holding <em>which</em> attribute a row is, and a column holding its value. The shape does not
 * vary between products — only the spelling does.</p>
 *
 * <p>So a product declares the spelling and the library owns the join. The alternative, which this
 * replaced, had every product assembling {@code "LEFT JOIN " + table + " " + alias + " ON " + …} by
 * hand: the same SQL written repeatedly by the least-reviewed code in the path, each copy free to get
 * the alias, the quoting or the correlation subtly wrong.</p>
 *
 * <h2>⚠️ {@code localColumn} — what the bag hangs off, when it is not the row's own key</h2>
 *
 * <p>Usually a bag row points straight at the row being filtered, and the correlation is
 * {@code bag.foreign_key = root.id}. But a product whose subject area is <em>about</em> something that
 * has a bag has a second shape: an asset is a row of its own carrying a state and a due date, and
 * <strong>its values live on the entry it describes</strong>. There the correlation is
 * {@code field_entries.form_entry_id = assets.form_entry_id} — one hop sideways, not down.</p>
 *
 * <p>⚠️ Left as {@code null} it is the target's key, which is what every ordinary bag wants. Naming it is
 * how the second shape is said out loud rather than worked around by inventing a view or by giving the
 * source the wrong root — and the wrong root is the tempting mistake, because it compiles and returns
 * rows that are simply about something else.</p>
 *
 * @param table       the table the values live in — {@code field_entries}
 * @param foreignKey  the column pointing at the row they belong to — {@code form_entry_id}
 * @param keyColumn   the column saying which attribute a row is — {@code field_id}
 * @param valueColumn the column holding the value — {@code text_value}
 * @param localColumn the column on the FILTERED row that the bag hangs off, or {@code null} for its key
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record BagTable(String table, String foreignKey, String keyColumn, String valueColumn,
                       String localColumn) {

    /** A bag that hangs off the filtered row's own key — the ordinary shape. */
    public BagTable(String table, String foreignKey, String keyColumn, String valueColumn) {
        this(table, foreignKey, keyColumn, valueColumn, null);
    }
}
