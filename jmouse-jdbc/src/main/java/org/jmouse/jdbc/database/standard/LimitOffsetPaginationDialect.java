package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.PaginationDialect;

public final class LimitOffsetPaginationDialect implements PaginationDialect {

    @Override
    public String apply(String sql, long offset, int limit) {
        return "%s LIMIT %d OFFSET %d".formatted(sql, limit, offset);
    }

}
