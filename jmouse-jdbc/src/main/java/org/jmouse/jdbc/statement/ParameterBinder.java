package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface ParameterBinder<T> {

    void bind(PreparedStatement statement, int index, T value) throws SQLException;

    static <T> ParameterBinder<T> objectBinder() {
        return PreparedStatement::setObject;
    }

    static ParameterBinder<Long> longBinder() {
        return (statement, index, value) -> {
            if (value == null) {
                nullBinder().bind(statement, index, null);
            } else {
                statement.setLong(index, value);
            }
        };
    }

    static ParameterBinder<Integer> integerBinder() {
        return (statement, index, value) -> {
            if (value == null) {
                nullBinder().bind(statement, index, null);
            } else {
                statement.setInt(index, value);
            }
        };
    }

    static ParameterBinder<Object> nullBinder() {
        return (statement, index, value) -> statement.setObject(index, null);
    }

    static ParameterBinder<String> stringBinder() {
        return PreparedStatement::setString;
    }

}