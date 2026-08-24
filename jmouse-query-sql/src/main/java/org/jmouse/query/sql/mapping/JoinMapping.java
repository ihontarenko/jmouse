package org.jmouse.query.sql.mapping;

import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.sql.AttributeMapping;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.SqlCompileException;
import org.jmouse.query.sql.SqlContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An attribute that is a column of a row one hop away — {@code issue.status.category}.
 *
 * <p>The product declares the tables it may reach and what points at them; an attribute then names one
 * of their columns as {@code table.column}:</p>
 *
 * <pre>{@code
 * JoinMapping.of(new JoinedTable("statuses", "status_id", "id"));
 * // attribute issue.status.category from statuses.category text in join
 * }</pre>
 *
 * <h2>⚠️ One alias per TABLE — the opposite of a bag's rule</h2>
 *
 * <p>{@code issue.status.name} and {@code issue.status.category} are two columns of the <strong>same
 * row</strong>, so they share one join. A bag's two attributes are two different rows and must not. Both
 * rules exist for the same reason — a join that asks one row to be two things returns nothing — and they
 * point in opposite directions, which is precisely why the two are separate declarations rather than one
 * clever mechanism.</p>
 *
 * <h2>⚠️ The attribute's source carries the table, and it must</h2>
 *
 * <p>{@code statuses.category}, not {@code category}. A source may reach several tables, and the last
 * dot is what tells them apart — recovering it from the <em>written</em> path ({@code issue.status.…})
 * would make the language's spelling load-bearing for the mapping, which is the mistake the schema's two
 * names exist to prevent.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JoinMapping implements AttributeMapping {

    private final Map<String, JoinedTable> tables = new LinkedHashMap<>();

    public JoinMapping(List<JoinedTable> tables) {
        tables.forEach(table -> this.tables.put(table.table(), table));
    }

    public static JoinMapping of(JoinedTable... tables) {
        return new JoinMapping(List.of(tables));
    }

    @Override
    public Fragment expression(QueryAttribute attribute, SqlContext context) {
        String source = attribute.source();
        int    dot    = source.lastIndexOf('.');

        if (dot < 1 || dot == source.length() - 1) {
            throw new SqlCompileException(
                    ("'%s' is reached through a join, so what the store calls it has to say which table: "
                     + "'statuses.category', not '%s'").formatted(attribute.name(), source));
        }

        String      table  = source.substring(0, dot);
        String      column = source.substring(dot + 1);
        JoinedTable joined = tables.get(table);

        if (joined == null) {
            throw new SqlCompileException(
                    ("'%s' reads a column of '%s', and this source declares no join to it; it reaches %s")
                            .formatted(attribute.name(), table,
                                    tables.isEmpty() ? "nothing" : String.join(", ", tables.keySet())));
        }

        // ⚠️ Keyed on the TABLE, so every attribute out of it shares one join. See the class note.
        String alias = context.alias(joined.table());

        context.join(alias, Join.left(joined.table(), alias)
                .on(alias, joined.foreignColumn()).equalTo(context.column(joined.localColumn()))
                .toFragment(context.dialect()));

        return Fragment.of(context.column(alias, column));
    }
}
