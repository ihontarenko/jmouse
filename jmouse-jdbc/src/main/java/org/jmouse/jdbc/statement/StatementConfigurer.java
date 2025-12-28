package org.jmouse.jdbc.statement;

import java.sql.SQLException;
import java.sql.Statement;

@FunctionalInterface
public interface StatementConfigurer {

    void configure(Statement statement) throws SQLException;

    StatementConfigurer NOOP = noop();

    static StatementConfigurer noop() {
        return new StatementConfigurer() {
            @Override
            public void configure(Statement statement) throws SQLException {}
            @Override
            public String toString() {
                return "NOOP";
            }
        };
    }

    static StatementConfigurer chain(StatementConfigurer a, StatementConfigurer b) {
        if (a == null || a == NOOP) return (b != null) ? b : NOOP;
        if (b == null || b == NOOP) return a;
        return statement -> {
            a.configure(statement);
            b.configure(statement);
        };
    }

}