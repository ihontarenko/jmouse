package org.jmouse.jdbc.statement;

import java.sql.CallableStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface CallableStatementBinder {

    CallableStatementBinder NOOP = statement -> {};

    void bind(CallableStatement statement) throws SQLException;

}