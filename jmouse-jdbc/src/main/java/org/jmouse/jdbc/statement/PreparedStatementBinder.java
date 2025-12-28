package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface PreparedStatementBinder {

    void bind(PreparedStatement statement) throws SQLException;

    PreparedStatementBinder NOOP = noop();

    static PreparedStatementBinder noop() {
        return new PreparedStatementBinder() {
            @Override
            public void bind(PreparedStatement statement) throws SQLException {}
            @Override
            public String toString() {
                return "NOOP";
            }
        };
    }

}