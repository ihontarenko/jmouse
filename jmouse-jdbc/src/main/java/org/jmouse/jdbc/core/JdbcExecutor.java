package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

    /**
     * Execute batch update using same SQL, multiple binders.
     */
    int[] executeBatch(String sql, List<? extends PreparedStatementBinder> binders) throws SQLException;

    /**
     * Execute update and extract generated key(s) from stmt.getGeneratedKeys().
     */
    <K> K executeUpdateWithKey(
            String sql,
            PreparedStatementBinder binder,
            KeyExtractor<K> extractor
    ) throws SQLException;

}