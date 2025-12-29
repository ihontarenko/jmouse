package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ListResultSetExtractor<T> implements ResultSetExtractor<List<T>> {

    private final RowMapper<T> mapper;

    public ListResultSetExtractor(RowMapper<T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<T> extract(ResultSet resultSet) throws SQLException {
        List<T> collection = new ArrayList<>();
        int     rowIndex   = 1;

        while (resultSet.next()) {
            collection.add(mapper.map(resultSet, rowIndex++));
        }

        return collection;
    }
}
