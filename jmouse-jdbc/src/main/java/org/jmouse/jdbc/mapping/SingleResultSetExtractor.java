package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class SingleResultSetExtractor<T> implements ResultSetExtractor<T> {

    private final RowMapper<T> mapper;

    public SingleResultSetExtractor(RowMapper<T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public T extract(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return mapper.map(resultSet);
        }

        return null;
    }
}
