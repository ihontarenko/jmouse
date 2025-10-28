package org.jmouse.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * 🔍 Convenience wrapper over {@link ResultSet}.
 */
public final class Row {

    private final ResultSet resultSet;
    private final int       rowNumber;

    public Row(ResultSet resultSet, int rowNumber) {
        this.resultSet = resultSet;
        this.rowNumber = rowNumber;
    }

    public int rowNumber() {
        return rowNumber;
    }

    public <T> T get(int columnIndex, ColumnMapper<T> columnMapper) throws SQLException {
        return columnMapper.get(resultSet, columnIndex);
    }

    public String get(String label) throws SQLException {
        return resultSet.getString(label);
    }

    public Object getObject(int columnIndex) throws SQLException {
        return get(columnIndex, ColumnMapper.OBJECT);
    }

    public int columns() throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();

        if (metadata == null) {
            return 0;
        }

        return metadata.getColumnCount();
    }

}