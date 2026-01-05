package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementBinder {

    void bind(PreparedStatement statement) throws SQLException;

    StatementBinder NOOP = noop();

    static StatementBinder noop() {
        return new StatementBinder() {
            @Override
            public void bind(PreparedStatement statement) throws SQLException {}
            @Override
            public String toString() {
                return "NOOP";
            }
        };
    }

}