package org.jmouse.query.sql.mapping;

import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.MembershipMapping;
import org.jmouse.query.sql.SqlCompileException;
import org.jmouse.query.sql.SqlContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Answers the three questions a collection can be asked, as {@code EXISTS}.
 *
 * <pre>{@code
 * CollectionMapping.of(new CollectionTable("issue_labels", "issue_id", "label"));
 * // issue.labels is hasAny(['regression', 'flaky'])
 * }</pre>
 *
 * <h2>⚠️ A correlated subquery, never a join</h2>
 *
 * <p>{@code EXISTS (SELECT 1 FROM issue_labels x WHERE x.issue_id = i.id AND x.label IN (?, ?))}. A join
 * would answer the same question and return the row <strong>once per matching label</strong> — so a list
 * gains duplicates, a count is wrong, and paging silently loses rows off the end of a page. None of that
 * raises anything.</p>
 *
 * <h2>⚠️ {@code hasAll} counts DISTINCT, and that is not a detail</h2>
 *
 * <p>Asking for all of {@code ['a','b']} where a row carries {@code a} twice would otherwise count two
 * and answer yes. Distinct makes the count mean <em>how many of the asked-for values are present</em>,
 * which is the question.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class CollectionMapping implements MembershipMapping {

    private final Map<String, CollectionTable> tables = new LinkedHashMap<>();

    public CollectionMapping(List<CollectionTable> tables) {
        tables.forEach(table -> this.tables.put(table.table(), table));
    }

    public static CollectionMapping of(CollectionTable... tables) {
        return new CollectionMapping(List.of(tables));
    }

    @Override
    public Fragment membership(
            QueryAttribute attribute, Question question, List<Fragment> items, SqlContext context) {

        CollectionTable table = tables.get(attribute.source());

        if (table == null) {
            throw new SqlCompileException(
                    ("'%s' is declared as a collection kept in '%s', and this source declares no such "
                     + "collection; it has %s").formatted(attribute.name(), attribute.source(),
                            tables.isEmpty() ? "none" : String.join(", ", tables.keySet())));
        }

        if (items.isEmpty()) {
            throw new SqlCompileException(
                    "'%s' was asked about nothing at all; name at least one value".formatted(attribute.name()));
        }

        // ⚠️ Its own alias, from the same counter every join uses, so a query asking about two
        // collections — or the same one twice — cannot end up correlating the wrong subquery.
        String       alias      = context.alias("collection:" + attribute.name() + ":" + question);
        List<Object> parameters = new ArrayList<>();
        List<String> holes      = new ArrayList<>();

        for (Fragment item : items) {
            holes.add(item.sql());
            parameters.addAll(item.parameters());
        }

        String inList = "%s IN (%s)".formatted(
                context.column(alias, table.valueColumn()), String.join(", ", holes));

        String correlation = "%s = %s".formatted(
                context.column(alias, table.foreignKey()), context.rootKey());

        String from = "%s %s".formatted(context.dialect().quote(table.table()), alias);

        String written = switch (question) {
            case ANY -> "EXISTS (SELECT 1 FROM %s WHERE %s AND %s)".formatted(from, correlation, inList);
            case NONE -> "NOT EXISTS (SELECT 1 FROM %s WHERE %s AND %s)".formatted(from, correlation, inList);
            case ALL -> "(SELECT COUNT(DISTINCT %s) FROM %s WHERE %s AND %s) = %d".formatted(
                    context.column(alias, table.valueColumn()), from, correlation, inList, items.size());
        };

        return new Fragment(written, parameters);
    }
}
