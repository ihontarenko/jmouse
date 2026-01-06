package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * PreparedStatement binder that knows how many JDBC parameters it binds.
 */
public interface CountedBinder {

    void bind(PreparedStatement statement) throws SQLException;

    int countOfParameters();

    static CountedBinder of(StatementBinder binder, int countOfParameters) {
        return new CountedBinder() {

            @Override
            public void bind(PreparedStatement statement) throws SQLException {
                binder.bind(statement);
            }

            @Override
            public int countOfParameters() {
                return countOfParameters;
            }

        };
    }
}
