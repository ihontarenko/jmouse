package org.jmouse.jdbc.mapping;

import org.jmouse.core.Contract;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final public class RowView implements RowAccess {

    private final ResultSet            resultSet;
    private final Map<String, Integer> index;

    private RowView(ResultSet resultSet, Map<String, Integer> index) {
        this.resultSet = resultSet;
        this.index = index;
    }

    public static RowView of(ResultSet resultSet) throws SQLException {
        ResultSetMetaData    metaData = resultSet.getMetaData();
        int                  count    = metaData.getColumnCount();
        Map<String, Integer> mapping  = new HashMap<>(count * 2);

        for (int index = 1; index <= count; index++) {
            Optional<String> columnName  = Optional.ofNullable(metaData.getColumnName(index));
            Optional<String> columnLabel = Optional.ofNullable(metaData.getColumnLabel(index));
            int[]            cache       = new int[count];
            columnName.ifPresent(n -> mapping.put(n, cache[0]));
            columnLabel.ifPresent(n -> mapping.putIfAbsent(n, cache[0]));
        }

        return new RowView(resultSet, mapping);
    }

    @Override
    public int indexOf(String column) {
        Contract.state(index.containsKey(column), () -> new IllegalArgumentException("Unknown column '" + column + "'"));
        return index.get(column);
    }

    @Override
    public String getString(int index) throws SQLException {
        return resultSet.getString(index);
    }

    @Override
    public Long getLong(int index) throws SQLException {
        long value = resultSet.getLong(index);

        if (resultSet.wasNull()) {
            return null;
        }

        return value;
    }

    @Override
    public Integer getInteger(int index) throws SQLException {
        int value = resultSet.getInt(index);

        if (resultSet.wasNull()) {
            return null;
        }

        return value;
    }

    @Override
    public Boolean getBoolean(int index) throws SQLException {
        boolean value = resultSet.getBoolean(index);

        if (resultSet.wasNull()) {
            return null;
        }

        return value;
    }

    @Override
    public Object getObject(int index) throws SQLException {
        return resultSet.getObject(index);
    }

    @Override
    public <T> T getObject(int index, Class<T> type) throws SQLException {
        return resultSet.getObject(index, type);
    }

}
