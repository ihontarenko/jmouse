package org.jmouse.jdbc.mapping;

import java.sql.SQLException;

public interface RowAccess {

    int indexOf(String column);

    default String getString(String column) throws SQLException {
        return getString(indexOf(column));
    }

    default Long getLong(String column) throws SQLException {
        return getLong(indexOf(column));
    };

    default Integer getInteger(String column) throws SQLException {
        return getInteger(indexOf(column));
    }

    default Boolean getBoolean(String column) throws SQLException {
        return getBoolean(indexOf(column));
    }

    default Object getObject(String column) throws SQLException {
        return getObject(indexOf(column));
    }

    default <T> T getObject(String column, Class<T> type) throws SQLException {
        return getObject(indexOf(column), type);
    }

    String getString(int index) throws SQLException;

    Long getLong(int index) throws SQLException;

    Integer getInteger(int index) throws SQLException;

    Boolean getBoolean(int index) throws SQLException;

    Object getObject(int index) throws SQLException;

    <T> T getObject(int index, Class<T> type) throws SQLException;

}
