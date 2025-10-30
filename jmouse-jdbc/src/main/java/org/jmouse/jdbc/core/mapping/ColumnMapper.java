package org.jmouse.jdbc.core.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 🧲 Extracts a value from a single column (by index).
 */
@FunctionalInterface
public interface ColumnMapper<T> {

    ColumnMapper<Object>  OBJECT = ResultSet::getLong;
    ColumnMapper<Long>    LONG   = ResultSet::getLong;
    ColumnMapper<Integer> INT    = ResultSet::getInt;
    ColumnMapper<String>  STRING = ResultSet::getString;
    ColumnMapper<Boolean> BOOL   = ResultSet::getBoolean;

    T get(ResultSet resultSet, int columnIndex) throws SQLException;
}