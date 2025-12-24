package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ColumnMapper<T> {
    T map(ResultSet resultSet, int index) throws SQLException;
}