package org.jmouse.jdbc.statement;

import java.sql.CallableStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface CallableStatementBinder {
    void bind(CallableStatement statement) throws SQLException;
}