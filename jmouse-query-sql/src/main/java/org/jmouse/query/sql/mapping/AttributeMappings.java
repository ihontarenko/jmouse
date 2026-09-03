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
     * Chooses a mapping by how the schema said an attribute is reached, over declared joins.
     *
     * <h2>⚠️ The join mapping is built HERE, and it has to be</h2>
     *
     * <p>A join declared {@code through} an attribute ({@link JoinedTable#through}) has to compile that
     * attribute to know what its equality points at — and the attribute may be reached any way at all, so
     * what compiles it is this same dispatcher. Handing a finished {@code JoinMapping} in would mean
     * building the thing it needs before it exists, so the tables are taken as data and the knot is tied
     * in one place instead of at every call site.</p>
     *
     * <p>⚠️ A declaration whose joins point through each other in a circle recurses until the stack ends.
     * That is a mistake in the document rather than a state to recover from, and it is not guarded here —
     * worth knowing before writing the second {@code through} in one source.</p>
     *
     * @param column what to do with a real column
     * @param bag    what to do with a value in a bag
     * @param joins  the tables one hop away, and what points at each
     * @return a mapping covering all three
     */
    public static AttributeMapping byAccess(
            AttributeMapping column, AttributeMapping bag, java.util.List<JoinedTable> joins) {

        return new AttributeMapping() {

            /* ⚠️ `this::expression` and not a captured local: the pointer is compiled by the dispatcher
               that is being built, and a lambda cannot name itself. */
            private final AttributeMapping joined = new JoinMapping(joins, this::expression);

            @Override
            public org.jmouse.query.sql.Fragment expression(
                    QueryAttribute attribute, org.jmouse.query.sql.SqlContext context) {

                return switch (attribute.access()) {
                    case COLUMN -> column.expression(attribute, context);
                    case BAG -> bag.expression(attribute, context);
                    case JOINED -> joined.expression(attribute, context);
                    case COLLECTION -> throw collectionRefusal(attribute);
                };
            }
        };
    }

    /**
     * Chooses a mapping by how the schema said an attribute is reached, joins included.
     *
     * <p>⚠️ Prefer {@link #byAccess(AttributeMapping, AttributeMapping, java.util.List)} where the joins
     * are declared data. This overload takes a finished join mapping, so a join {@code through} an
     * attribute reaches only whatever pointer that mapping was given — which is nothing, unless the
     * caller tied the knot itself.</p>
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

            case COLLECTION -> throw collectionRefusal(attribute);
        };
    }

    /**
     * ⚠️ Not a gap. A collection has no expression at all — it is many rows — and is reached through
     * {@code MembershipMapping} instead. Anything arriving here has been used as a value, which the
     * checker refuses; this is the second wall, for a caller that skipped the first.
     */
    private static SqlCompileException collectionRefusal(QueryAttribute attribute) {
        return new SqlCompileException(
                ("'%s' holds many values per row and cannot be read as one; ask it "
                 + "'is hasAny([…])', 'is hasAll([…])' or 'is hasNone([…])'").formatted(attribute.name()));
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
    /**
     * A mapping that already exists, plus some more tables one hop away.
     *
     * <h2>⚠️ Composed, because a mapping cannot be taken apart</h2>
     *
     * <p>An {@link AttributeMapping} is a function: which table a bag lives in and which columns link it
     * are closed over inside a lambda, and nothing recovers them. So a product that loaded a source from
     * a file and then learns of one more join — because what it points through is a field somebody made
     * on a screen, and no file written in advance could name it — cannot rebuild the mapping. It wraps
     * it.</p>
     *
     * <p>⚠️ <strong>Only the named tables are taken.</strong> Every other attribute, joined ones
     * included, goes to the mapping underneath, so this adds and never shadows — a wrapper that answered
     * for everything would be a second implementation of the thing it wraps, agreeing until the day the
     * original learns something.</p>
     *
     * <p>⚠️ The extra joins may point {@code through} an attribute the WRAPPED mapping reaches — a bag
     * row, typically, which is the whole reason this exists — so the pointer is compiled by the composite
     * rather than by either half alone.</p>
     *
     * @param declared what the source already knows how to read
     * @param extra    the tables it does not, and what points at each
     * @return a mapping answering for both
     */
    public static AttributeMapping alsoJoining(AttributeMapping declared, java.util.List<JoinedTable> extra) {
        if (extra.isEmpty()) {
            return declared;
        }

        return new AttributeMapping() {

            private final java.util.Set<String> tables = extra.stream()
                    .map(JoinedTable::table)
                    .collect(java.util.stream.Collectors.toSet());

            private final AttributeMapping joined = new JoinMapping(extra, this::expression);

            @Override
            public org.jmouse.query.sql.Fragment expression(
                    QueryAttribute attribute, org.jmouse.query.sql.SqlContext context) {

                return names(attribute) ? joined.expression(attribute, context)
                                        : declared.expression(attribute, context);
            }

            /* The attribute's source carries its table — `form_entries.form_id` — and the last dot is
               what tells one table from another. Same rule JoinMapping reads it by. */
            private boolean names(QueryAttribute attribute) {
                if (attribute.access() != QueryAttribute.Access.JOINED || attribute.source() == null) {
                    return false;
                }

                int dot = attribute.source().lastIndexOf('.');

                return dot > 0 && tables.contains(attribute.source().substring(0, dot));
            }
        };
    }

    public static AttributeMapping refusing(String what) {
        return (attribute, context) -> {
            throw new SqlCompileException(
                    ("'%s' is declared as %s attribute, and this mapping does not know how to reach one")
                            .formatted(attribute.name(), what));
        };
    }
}
