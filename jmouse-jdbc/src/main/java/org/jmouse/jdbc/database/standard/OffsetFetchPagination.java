package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.PaginationStrategy;
import org.jmouse.jdbc.query.OffsetLimit;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class OffsetFetchPagination implements PaginationStrategy {

    @Override
    public String apply(String sql, OffsetLimit page) {
        return "%s OFFSET ? ROWS FETCH NEXT ? ROWS ONLY".formatted(sql);
    }

    @Override
    public PaginationBind bind(OffsetLimit offsetLimit) {
        return new PaginationBind() {

            @Override
            public void bind(PreparedStatement statement, int startIndex) throws SQLException {
                statement.setLong(startIndex, offsetLimit.offset());
                statement.setInt(startIndex + 1, offsetLimit.limit());
            }

            @Override
            public int countOfParameters() {
                return 2;
            }

        };
    }
}
