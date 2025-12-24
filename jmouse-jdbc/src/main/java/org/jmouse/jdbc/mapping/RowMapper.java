package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<T> {

    default T map(ResultSet resultSet) throws SQLException {
        return map(RowView.of(resultSet));
    }

    T map(RowView view) throws SQLException;

}