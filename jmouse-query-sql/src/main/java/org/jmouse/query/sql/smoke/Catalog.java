package org.jmouse.query.sql.smoke;

import org.jmouse.jdbc.dialect.MySqlDialect;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.schema.QueryType;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.QueryTarget;
import org.jmouse.query.sql.mapping.AttributeMappings;
import org.jmouse.query.sql.mapping.BagMapping;
import org.jmouse.query.sql.mapping.BagTable;
import org.jmouse.query.sql.mapping.ColumnMapping;

import static org.jmouse.query.schema.QueryAttribute.Access.BAG;
import static org.jmouse.query.schema.QueryAttribute.Access.COLUMN;

/**
 * The two real products, configured the way a product configures itself.
 *
 * <p>⚠️ <strong>Everything below is a product's declaration, and none of it is SQL.</strong> Four names
 * for the bag, a target per source, and an attribute list saying what a query may write and what the
 * store calls it. That is the whole of what jMQ asks of a product.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Catalog {

    /**
     * ⚠️ Innoventa's bag is <strong>two hops</strong>: {@code field_entries.field_id} points at
     * {@code fields.id}, so the key column holds an ID rather than a name. Nothing in the library
     * changes for that — each attribute's {@code source} carries the id.
     */
    public static final BagTable TEXT_BAG =
            new BagTable("field_entries", "form_entry_id", "field_id", "text_value");

    /**
     * ⚠️ The same table again, reading a different column. Innoventa keeps {@code resistance} as
     * {@code "3300|mΩ"} in text and {@code 3300} in numeric, so which column a query should read is a
     * product's decision and the library never has to know.
     */
    public static final BagTable NUMERIC_BAG =
            new BagTable("field_entries", "form_entry_id", "field_id", "numeric_value");

    public static final QuerySchema INVENTORY = Smokes.schema(
            new QueryAttribute("entry[name]", "f-component-name", QueryType.TEXT, BAG),
            new QueryAttribute("entry[quantity]", "f-quantity", QueryType.UNKNOWN, BAG),
            new QueryAttribute("entry[resistance]", "f-resistance", QueryType.UNKNOWN, BAG),
            new QueryAttribute("entry[price]", "f-approximate-price", QueryType.UNKNOWN, BAG),
            new QueryAttribute("created", "created_at", QueryType.TEMPORAL, COLUMN));

    public static final QuerySchema EQUIPMENT = Smokes.schema(
            new QueryAttribute("asset[manufacturer]", "field-asset-manufacturer", QueryType.TEXT, BAG),
            new QueryAttribute("asset[model]", "field-asset-model", QueryType.TEXT, BAG),
            new QueryAttribute("asset[serial]", "field-asset-serial-number", QueryType.TEXT, BAG),
            new QueryAttribute("asset[inventory]", "field-asset-inventory-number", QueryType.TEXT, BAG));

    /** ⚠️ A query writes {@code issue.points}; the table calls it {@code story_points}. */
    public static final QuerySchema ISSUES = Smokes.schema(
            new QueryAttribute("issue.key", "issue_key", QueryType.TEXT, COLUMN),
            new QueryAttribute("issue.summary", "summary", QueryType.TEXT, COLUMN),
            new QueryAttribute("issue.points", "story_points", QueryType.NUMBER, COLUMN),
            new QueryAttribute("issue.status", "status_id", QueryType.TEXT, COLUMN),
            new QueryAttribute("issue.type", "issue_type_id", QueryType.TEXT, COLUMN),
            new QueryAttribute("issue.assignee", "assignee_member_id", QueryType.TEXT, COLUMN),
            new QueryAttribute("issue.parent", "parent_id", QueryType.TEXT, COLUMN),
            new QueryAttribute("issue.opened", "created_at", QueryType.TEMPORAL, COLUMN));

    private Catalog() {
    }

    public static QueryEngine innoventa() {
        return QueryEngine.with(new MySqlDialect())
                .source("inventory", new QueryTarget("inventory", "form_entries", "e", "id"), INVENTORY,
                        AttributeMappings.byAccess(ColumnMapping.qualified(), BagMapping.of(TEXT_BAG)))
                .source("numbers", new QueryTarget("numbers", "form_entries", "e", "id"), INVENTORY,
                        AttributeMappings.byAccess(ColumnMapping.qualified(), BagMapping.of(NUMERIC_BAG)))
                .source("assets", new QueryTarget("assets", "form_entries", "e", "id"), EQUIPMENT,
                        AttributeMappings.byAccess(ColumnMapping.qualified(), BagMapping.of(TEXT_BAG)))
                .build();
    }

    public static QueryEngine tessera() {
        return QueryEngine.with(new MySqlDialect())
                .source("issues", new QueryTarget("issues", "issues", "i", "id"), ISSUES,
                        AttributeMappings.columnsOnly())
                .build();
    }
}
