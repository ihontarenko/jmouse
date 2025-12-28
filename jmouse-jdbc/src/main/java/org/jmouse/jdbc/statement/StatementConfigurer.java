package org.jmouse.jdbc.statement;

import java.sql.SQLException;
import java.sql.Statement;

@FunctionalInterface
public interface StatementConfigurer {
    void configure(Statement statement) throws SQLException;
    StatementConfigurer NOOP = statement -> { };
}