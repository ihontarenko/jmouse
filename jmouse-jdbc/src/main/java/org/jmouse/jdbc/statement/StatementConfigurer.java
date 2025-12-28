package org.jmouse.jdbc.statement;

import java.sql.SQLException;
import java.sql.Statement;

@FunctionalInterface
public interface StatementConfigurer {

    void configure(Statement statement) throws SQLException;

    StatementConfigurer NOOP = statement -> { };

    static StatementConfigurer chain(StatementConfigurer a, StatementConfigurer b) {
        if (a == null || a == NOOP) return (b != null) ? b : NOOP;
        if (b == null || b == NOOP) return a;
        return statement -> {
            a.configure(statement);
            b.configure(statement);
        };
    }

}