package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<T> {

    default T map(ResultSet resultSet) throws SQLException {
        return map(ResultSetRowMetadata.of(resultSet));
    }

    T map(ResultSetRowMetadata view) throws SQLException;

}