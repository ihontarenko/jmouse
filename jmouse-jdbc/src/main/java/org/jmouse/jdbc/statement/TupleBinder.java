package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface TupleBinder<T> {
    void bind(PreparedStatement statement, T value) throws SQLException;
}