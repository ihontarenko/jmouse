package org.jmouse.jdbc.core;

import org.jmouse.jdbc.core.mapping.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 🎯 Maps first column to a single scalar value.
 */
public final class SingleColumnRowMapper<T> implements RowMapper<T> {

    @Override
    @SuppressWarnings("unchecked")
    public T map(ResultSet resultSet, int rowNumber) throws SQLException {
        return (T) resultSet.getObject(1);
    }

}