package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jmouse.jdbc.core.exception.EmptyResultException;
import org.jmouse.jdbc.core.exception.NonUniqueResultException;

public final class StrictSingleResultSetExtractor<T> implements ResultSetExtractor<Optional<T>> {

    private final RowMapper<T> mapper;
    private final String sqlForMessage;

    public StrictSingleResultSetExtractor(RowMapper<T> mapper, String sqlForMessage) {
        this.mapper = mapper;
        this.sqlForMessage = sqlForMessage;
    }

    @Override
    public Optional<T> extract(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            throw new EmptyResultException("Expected 1 row but got 0 for SQL: " + sqlForMessage);
        }

        T value = mapper.map(resultSet);

        if (resultSet.next()) {
            int count = 2;
            while (resultSet.next()) {
                count++;
            }
            throw new NonUniqueResultException(count, "Expected 1 row but got " + count + " for SQL: " + sqlForMessage);
        }

        return Optional.ofNullable(value);
    }
}
