package org.jmouse.query.sql;

import org.jmouse.query.el.node.JoinClauseNode;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.sql.mapping.Join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Several structures in one query, composed into the single source everything downstream expects.
 *
 * <h2>⚠️ A composition, not a second compiler</h2>
 *
 * <p>⚠️ It composes, so it nests: the result of one composition is the outer side of the next, and a view
 * saying {@code join:} three times is three calls rather than a compiler that counts sources. A later join
 * may therefore hang off an earlier one, and reads through it correctly, because the far side of every
 * condition is resolved through whatever the outer side turned out to be.</p>
 *
 * <p>A view joining two structures does not need the compiler to learn about two sources. It needs one
 * source whose schema is the union of both and whose mapping knows which half each attribute came from —
 * and the machinery for "another table with its own alias" already exists, because a mapping's own
 * {@code join:} has used it since the beginning.</p>
 *
 * <p>So this is an {@link AttributeMapping} that dispatches, and nothing in {@link SqlContext},
 * {@link SqlCompiler} or {@link ViewCompiler} changes. That is the difference between adding a feature and
 * adding a second route through the compiler.</p>
 *
 * <h2>⚠️ What the joined side may be, and what it may not</h2>
 *
 * <p>An attribute of the joined structure is read from <strong>its own table's column</strong>. An
 * attribute that itself lives in a bag, behind another join, or in a collection is
 * <strong>refused by name</strong>: it would need its own alias space inside a table that is already
 * aliased, and quietly rendering it against the wrong alias is the kind of wrong answer nobody would look
 * for.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JoinedStructures {

    private JoinedStructures() {
    }

    /**
     * One source that answers for both.
     *
     * @param outer  what the view is {@code from:}
     * @param inner  the structure being joined in
     * @param clause the condition — two attributes and an equality
     * @return a source whose schema and mapping cover both
     */
    public static QuerySource compose(QuerySource outer, QuerySource inner, JoinClauseNode clause) {
        QueryAttribute innerKey = require(inner, clause.getLeft(), clause);
        QueryAttribute outerKey = require(outer, clause.getRight(), clause);

        requireColumn(innerKey, inner, "joined on");
        requireColumn(outerKey, outer, "joined on");

        return new QuerySource(outer.target(), union(outer.schema(), inner.schema()),
                dispatch(outer, inner, innerKey, outerKey), outer.membership());
    }

    /**
     * ⚠️ The join is registered the first time an attribute of the inner structure is asked for, and only
     * then. A view that joins a structure and never reads it produces no join — which is not an
     * optimisation but the same rule a mapping's own join follows: a table nobody reads is a table nobody
     * pays for.
     */
    private static AttributeMapping dispatch(
            QuerySource outer, QuerySource inner, QueryAttribute innerKey, QueryAttribute outerKey) {

        return (attribute, context) -> {
            if (inner.schema().attribute(attribute.name()).isEmpty()) {
                return outer.mapping().expression(attribute, context);
            }

            requireColumn(attribute, inner, "read through a join between structures");

            // ⚠️ The far side is resolved THROUGH the outer mapping rather than as a column of the root
            // table. A second join may hang off the first — `join: category on category.key ==
            // person.category` — and there the far side lives at the first join's alias. Asking the
            // mapping registers that join first and answers with its alias; writing `context.column(…)`
            // instead would name the root table and compare two columns that have nothing to do with
            // each other, which is a wrong answer rather than a failure.
            String far = qualified(outer, outerKey, context);

            // ⚠️ Keyed on the joined SOURCE, so every attribute out of it shares one join and two different
            // structures never share an alias.
            String alias = context.alias("structure:" + inner.name());

            context.join(alias, Join.left(inner.target().table(), alias)
                    .on(alias, innerKey.source()).equalTo(far)
                    .toFragment(context.dialect()));

            return Fragment.of(context.column(alias, attribute.source()));
        };
    }

    /**
     * The far side of a join condition, as the outer source itself reads it.
     *
     * <p>⚠️ Refused when that expression binds a value. A join condition is written as two names and an
     * equality precisely so a backend can promise to honour it; an attribute whose SQL carries a
     * parameter has nowhere to put it here, and rendering the text without the value would produce a
     * statement whose {@code ?} count no longer matches what is bound.</p>
     */
    private static String qualified(QuerySource outer, QueryAttribute key, SqlContext context) {
        Fragment fragment = outer.mapping().expression(key, context);

        if (!fragment.parameters().isEmpty()) {
            throw new SqlCompileException(
                    ("'%s' is joined on, and it reads as an expression that binds a value in '%s'; "
                     + "a join is written as two plain columns and an equality").formatted(
                            key.name(), outer.name()));
        }

        return fragment.sql();
    }

    private static QueryAttribute require(QuerySource source, String name, JoinClauseNode clause) {
        return source.schema().attribute(name).orElseThrow(() -> new SqlCompileException(
                ("the join to '%s' reads '%s', and '%s' does not declare it; it declares %s").formatted(
                        clause.getStructure(), name, source.name(), names(source.schema()))));
    }

    /**
     * ⚠️ Refused by name rather than rendered against whichever alias happened to be current. A bag or a
     * nested join on the joined side needs its own alias space inside a table that is already aliased, and
     * there is no correct answer to give without one.
     */
    private static void requireColumn(QueryAttribute attribute, QuerySource source, String what) {
        if (attribute.access() == QueryAttribute.Access.COLUMN) {
            return;
        }

        throw new SqlCompileException(
                ("'%s' is %s, and an attribute %s has to be a column of its own table; "
                 + "this one is read '%s' in '%s'").formatted(
                        attribute.name(), what, what, attribute.access().name().toLowerCase(), source.name()));
    }

    private static QuerySchema union(QuerySchema outer, QuerySchema inner) {
        Map<String, QueryAttribute> attributes = new LinkedHashMap<>();

        outer.attributes().forEach(attribute -> attributes.put(attribute.name(), attribute));

        // ⚠️ The inner side does NOT overwrite the outer one. Attribute names carry their structure's
        // prefix, so a collision means two structures of the same name — and silently preferring one of
        // them would answer a question nobody asked.
        inner.attributes().forEach(attribute -> attributes.putIfAbsent(attribute.name(), attribute));

        return new QuerySchema() {

            @Override
            public Optional<QueryAttribute> attribute(String name) {
                return Optional.ofNullable(attributes.get(name));
            }

            @Override
            public Collection<QueryAttribute> attributes() {
                return attributes.values();
            }
        };
    }

    private static String names(QuerySchema schema) {
        Collection<String> names = new ArrayList<>();

        schema.attributes().forEach(attribute -> names.add(attribute.name()));

        return names.isEmpty() ? "nothing" : String.join(", ", names);
    }
}
