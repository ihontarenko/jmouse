package org.jmouse.jdbc.query;

import org.jmouse.jdbc.database.SQLQuoting;

import java.util.Set;
import java.util.StringJoiner;

import static org.jmouse.core.Contract.nonNull;

public final class OrderBy {

    private OrderBy() {}

    public static String apply(String sql, Sort sort, SQLQuoting quoting, Set<String> allowedColumns) {
        nonNull(quoting, "quoting");
        nonNull(sql, "sql");
        nonNull(allowedColumns, "allowedColumns");

        if (sort == null || sort.isEmpty()) {
            return sql;
        }

        StringJoiner joiner = new StringJoiner(", ");

        for (Sort.Order order : sort.orders()) {
            String column = order.property();
            String quoted = quoting.quoteIdentifier(column);
            if (!allowedColumns.contains(column)) {
                throw new IllegalArgumentException("Sort column is not allowed: " + column);
            }
            joiner.add("%s %s".formatted(quoted, order.direction().name()));
        }

        return "%s order by %s".formatted(sql, joiner);
    }
}
