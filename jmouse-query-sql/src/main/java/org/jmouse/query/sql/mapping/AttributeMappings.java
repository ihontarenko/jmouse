package org.jmouse.query.sql.mapping;

import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.sql.AttributeMapping;
import org.jmouse.query.sql.SqlCompileException;

/**
 * Puts the ready-made mappings together, so that a product declares names and writes no SQL.
 *
 * <pre>{@code
 * AttributeMapping mapping = AttributeMappings.byAccess(
 *         ColumnMapping.qualified(),
 *         BagMapping.of(new BagTable("field_entries", "entry_id", "field", "text_value")));
 * }</pre>
 *
 * <p>⚠️ That is the whole of what a product with a bag store has to say. Four names, as data. The join
 * shape, the aliasing, the quoting and the binding all live where they can be got right once.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class AttributeMappings {

    private AttributeMappings() {
    }

    /**
     * Chooses a mapping by how the schema said an attribute is reached.
     *
     * @param column what to do with a real column
     * @param bag    what to do with a value in a bag
     * @return a mapping covering both
     */
    public static AttributeMapping byAccess(AttributeMapping column, AttributeMapping bag) {
        return byAccess(column, bag, refusing("a joined"));
    }

    /**
     * Chooses a mapping by how the schema said an attribute is reached, joins included.
     *
     * @param column what to do with a real column
     * @param bag    what to do with a value in a bag
     * @param joined what to do with a column of a row one hop away
     * @return a mapping covering all three
     */
    public static AttributeMapping byAccess(
            AttributeMapping column, AttributeMapping bag, AttributeMapping joined) {

        return (attribute, context) -> switch (attribute.access()) {
            case COLUMN -> column.expression(attribute, context);
            case BAG -> bag.expression(attribute, context);
            case JOINED -> joined.expression(attribute, context);

            // ⚠️ Not a gap. A collection has no expression at all — it is many rows — and is reached
            // through MembershipMapping instead. Anything arriving here has been used as a value, which
            // the checker refuses; this is the second wall, for a caller that skipped the first.
            case COLLECTION -> throw new SqlCompileException(
                    ("'%s' holds many values per row and cannot be read as one; ask it "
                     + "'is hasAny([…])', 'is hasAll([…])' or 'is hasNone([…])'").formatted(attribute.name()));
        };
    }

    /**
     * A product whose data is all real columns.
     *
     * @return a mapping that refuses a bag attribute by name
     */
    public static AttributeMapping columnsOnly() {
        return byAccess(ColumnMapping.qualified(), refusing("a bag"), refusing("a joined"));
    }

    /**
     * ⚠️ Refuses rather than returning something plausible.
     *
     * <p>A schema that declares an attribute reached one way while the mapping only knows the other is a
     * misconfiguration, and the honest answer names it. Returning a bare column for a bag attribute
     * would produce a statement that runs and reads the wrong thing.</p>
     *
     * <p>⚠️ Public because a <em>caller</em> assembling the three branches needs it too, and the
     * alternative it replaced was passing {@link #columnsOnly()} in as the bag branch — which refuses
     * correctly, by going round the switch a second time, and reads like a mistake somebody will one day
     * "fix" into a real mapping.</p>
     *
     * @param what how the attribute is reached, in the words the message should use — {@code "a bag"}
     * @return a mapping that refuses every attribute, naming it
     */
    public static AttributeMapping refusing(String what) {
        return (attribute, context) -> {
            throw new SqlCompileException(
                    ("'%s' is declared as %s attribute, and this mapping does not know how to reach one")
                            .formatted(attribute.name(), what));
        };
    }
}
