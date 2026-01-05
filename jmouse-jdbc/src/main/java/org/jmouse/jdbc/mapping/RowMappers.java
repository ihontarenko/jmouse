package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;

public final class RowMappers {

    private RowMappers() {}

    public static RowMapper<String> stringColumn(int index) {
        return new ColumnRowMapper<>(index, ResultSet::getString);
    }

    public static RowMapper<Long> longColumn(int index) {
        return new ColumnRowMapper<>(index, (resultSet, i)
                -> resultSet.wasNull() ? null : resultSet.getLong(i));
    }

    public static RowMapper<Integer> integerColumn(int index) {
        return new ColumnRowMapper<>(index, (resultSet, i)
                -> resultSet.wasNull() ? null : resultSet.getInt(i));
    }

    public static RowMapper<Boolean> booleanColumn(int index) {
        return new ColumnRowMapper<>(index, (resultSet, i)
                -> resultSet.wasNull() ? null : resultSet.getBoolean(i));
    }

}
