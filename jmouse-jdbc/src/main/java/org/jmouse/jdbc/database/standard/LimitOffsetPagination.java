package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.PaginationStrategy;
import org.jmouse.jdbc.query.OffsetLimit;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class LimitOffsetPagination implements PaginationStrategy {

    @Override
    public String apply(String sql, OffsetLimit page) {
        return "%s LIMIT ? OFFSET ?".formatted(sql);
    }

    @Override
    public PaginationBind bind(OffsetLimit offsetLimit) {
        return new PaginationBind() {

            @Override
            public void bind(PreparedStatement ps, int startIndex) throws SQLException {
                ps.setInt(startIndex, offsetLimit.limit());
                ps.setLong(startIndex + 1, offsetLimit.offset());
            }

            @Override
            public int countOfParameters() {
                return 2;
            }

        };
    }
}