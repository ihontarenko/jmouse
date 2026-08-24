package org.jmouse.query.sql.mapping;

import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.sql.AttributeMapping;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.SqlContext;

/**
 * An attribute that is a real column of the target's own table.
 *
 * <p>No join, no cast, no alias to allocate — the column carries its type from the schema and reads as
 * itself. It is the whole of what a product with an ordinary table needs.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ColumnMapping implements AttributeMapping {

    private static final ColumnMapping QUALIFIED = new ColumnMapping();

    /**
     * The column of the target's table, qualified by its alias.
     *
     * <p>⚠️ Qualified rather than bare, always. An unqualified column is ambiguous the moment a join
     * appears — and a join appears as soon as one bag attribute joins the query, which is a change the
     * mapping never sees.</p>
     *
     * @return the mapping
     */
    public static ColumnMapping qualified() {
        return QUALIFIED;
    }

    @Override
    public Fragment expression(QueryAttribute attribute, SqlContext context) {
        // ⚠️ `source`, not `name` — a query may write `issue.assignee` for a column the table calls
        // `assignee_id`. The schema recorded both; nothing here has to guess the translation.
        return Fragment.of(context.column(attribute.source()));
    }
}
