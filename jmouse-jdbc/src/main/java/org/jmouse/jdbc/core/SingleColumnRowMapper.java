package org.jmouse.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 🎯 Maps first column to a single scalar value.
 */
public final class SingleColumnRowMapper<T> implements RowMapper<T> {
    private final Class<T> requiredType;

    public SingleColumnRowMapper(Class<T> requiredType) {
        this.requiredType = requiredType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T map(ResultSet resultSet, int rowNumber) throws SQLException {
        return (T) resultSet.getObject(1);
    }
}