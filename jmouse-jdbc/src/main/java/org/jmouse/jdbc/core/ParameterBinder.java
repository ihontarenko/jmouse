package org.jmouse.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 🔗 Binds parameters to a {@link PreparedStatement}.
 */
@FunctionalInterface
public interface ParameterBinder {

    static ParameterBinder of(Object... arguments) {
        return preparedStatement -> {
            for (int i = 0; i < arguments.length; i++) {
                preparedStatement.setObject(i + 1, arguments[i]);
            }
        };
    }

    void bind(PreparedStatement preparedStatement) throws SQLException;
}