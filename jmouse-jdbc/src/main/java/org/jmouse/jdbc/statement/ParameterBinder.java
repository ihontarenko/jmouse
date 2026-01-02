package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface ParameterBinder<T> {
    void bind(PreparedStatement statement, int index, T value) throws SQLException;
}