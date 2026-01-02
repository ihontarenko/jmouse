package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.PaginationDialect;

public final class OracleOffsetFetchPaginationDialect implements PaginationDialect {

    @Override
    public String apply(String sql, long offset, int limit) {
        return "%s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY".formatted(sql, offset, limit);
    }

}
