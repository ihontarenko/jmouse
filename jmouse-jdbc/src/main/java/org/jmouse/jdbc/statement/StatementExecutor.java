package org.jmouse.jdbc.statement;

import java.sql.SQLException;
import java.sql.Statement;

@FunctionalInterface
public interface StatementExecutor<S extends Statement, R> {
    R execute(S statement) throws SQLException;
}
