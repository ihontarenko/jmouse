package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface JdbcExecutor {

    <T> T execute(
            String sql,
            StatementCallback<ResultSet> action,
            ResultSetExtractor<T> extractor
    ) throws SQLException;

    <T> T execute(
            String sql,
            PreparedStatementBinder binder,
            StatementCallback<ResultSet> statementCallback,
            ResultSetExtractor<T> extractor
    ) throws SQLException;

    int executeUpdate(String sql) throws SQLException;

    int executeUpdate(String sql, PreparedStatementBinder binder) throws SQLException;

}