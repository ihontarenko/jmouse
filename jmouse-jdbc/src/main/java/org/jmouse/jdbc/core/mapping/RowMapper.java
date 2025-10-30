package org.jmouse.jdbc.core.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 🧭 Type-safe row mapper.
 */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet resultSet, int rowNumber) throws SQLException;
}