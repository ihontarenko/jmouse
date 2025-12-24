package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class ColumnRowMapper<T> implements RowMapper<T> {

    private final int             index;
    private final ColumnMapper<T> mapper;

    public ColumnRowMapper(int index, ColumnMapper<T> mapper) {
        this.index = index;
        this.mapper = mapper;
    }

    @Override
    public T map(ResultSet resultSet) throws SQLException {
        return mapper.map(resultSet, index);
    }

    @Override
    public T map(RowView view) throws SQLException {
        // ????
        return mapper.map(view, index);
    }
}
