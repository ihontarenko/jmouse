package org.jmouse.query.sql.mapping;

/**
 * A table one hop away, and how to get there.
 *
 * <p>{@code new JoinedTable("statuses", "status_id", "id")} — <em>our</em> {@code status_id} points at
 * <em>their</em> {@code id}. An attribute then names a column of it: {@code statuses.category}.</p>
 *
 * <p>⚠️ Deliberately no join type, no condition and no direction. It is always a {@code LEFT JOIN} on
 * equality, because that is the only shape a declared source needs: a normalised value that may or may
 * not be there. Anything richer is a query, and a query is what the language is for — a mapping that
 * could express one would be a second place to write queries, unreadable and unchecked.</p>
 *
 * <h2>⚠️ The pointer is not always a column</h2>
 *
 * <p>A product that stores its values in a bag keeps its foreign keys there too. Innoventa's stock
 * position holds the part it counts as a <em>bag row</em> — {@code field_entries.text_value} for the
 * field the installation named as its part field — so the table it points at cannot be reached by any
 * column of {@code form_entries}. {@link #through} says so:</p>
 *
 * <pre>{@code
 * JoinedTable.through("form_entries", "e.part", "id")
 * // LEFT JOIN field_entries j1 ON j1.form_entry_id = e.id AND j1.field_id = 'part'
 * // LEFT JOIN form_entries  j2 ON j2.id = j1.text_value
 * }</pre>
 *
 * <p>⚠️ <strong>It names an ATTRIBUTE, not a bag key.</strong> The attribute already knows what the
 * store calls it, and which access it has — so the same declaration works whether the pointer turns out
 * to be a bag row, a column, or a value one join further on. Naming the bag key here instead would put
 * the language's spelling rules in a mapping, which is the mistake the schema's two names exist to
 * prevent.</p>
 *
 * @param table          the table one hop away — {@code statuses}
 * @param localColumn    the column on our row pointing at it — {@code status_id}; {@code null} when the
 *                       pointer is an attribute
 * @param foreignColumn  the column on its row being pointed at — {@code id}
 * @param localAttribute the attribute whose value points at it — {@code e.part}; {@code null} when the
 *                       pointer is a column of our own row
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record JoinedTable(String table, String localColumn, String foreignColumn, String localAttribute) {

    /** The ordinary shape: a column of our own row points at theirs. */
    public JoinedTable(String table, String localColumn, String foreignColumn) {
        this(table, localColumn, foreignColumn, null);
    }

    /**
     * The pointer is an attribute's value rather than a column — see the class note.
     *
     * @param table          the table one hop away
     * @param localAttribute the attribute whose value points at it, as the schema names it
     * @param foreignColumn  the column on its row being pointed at
     */
    public static JoinedTable through(String table, String localAttribute, String foreignColumn) {
        return new JoinedTable(table, null, foreignColumn, localAttribute);
    }

    /** Whether the pointer is an attribute rather than a column of our own row. */
    public boolean pointsThroughAttribute() {
        return localAttribute != null;
    }
}
