package org.jmouse.query.sql;

import org.jmouse.query.schema.QuerySchema;

/**
 * One thing a query can be written against: where the rows are, what may be asked of them, and how to
 * reach it.
 *
 * <p>The three travel together because they are only meaningful together — a schema describing
 * attributes of a table nobody named, or a mapping for a schema that declares nothing, is a
 * misconfiguration that can only be discovered at compile time. Bundled, they are configured once and
 * wrong together or right together.</p>
 *
 * <p>⚠️ <strong>A record rather than something the engine hides.</strong> A product that wants to build
 * one and compile against it directly — a one-off report, a test, a screen with its own narrowed schema
 * — can, without registering anything. The registry in {@link QueryEngine} is a convenience over this,
 * not a gate in front of it.</p>
 *
 * @param target the table the rows come from
 * @param schema what may be filtered, sorted and returned
 * @param mapping how each attribute becomes SQL
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record QuerySource(QueryTarget target, QuerySchema schema, AttributeMapping mapping,
                          MembershipMapping membership) {

    /**
     * A source with no collections.
     *
     * <p>⚠️ The membership mapping is <strong>refusing</strong> rather than absent, so a schema declaring
     * a collection while the mapping knows of none is told so by name — instead of the question quietly
     * reaching a mapping that answers something else.</p>
     */
    public QuerySource(QueryTarget target, QuerySchema schema, AttributeMapping mapping) {
        this(target, schema, mapping, refusing());
    }

    /** What a document writes after {@code on}. */
    public String name() {
        return target.name();
    }

    private static MembershipMapping refusing() {
        return (attribute, question, items, context) -> {
            throw new SqlCompileException(
                    ("'%s' is declared as a collection, and this source says nothing about where its "
                     + "items are kept").formatted(attribute.name()));
        };
    }
}
