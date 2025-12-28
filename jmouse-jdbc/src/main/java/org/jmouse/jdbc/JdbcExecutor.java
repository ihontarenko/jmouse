package org.jmouse.jdbc;

import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.CallableCallback;
import org.jmouse.jdbc.statement.CallableStatementBinder;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface JdbcExecutor {

    <T> T execute(String sql, StatementCallback<ResultSet> action, ResultSetExtractor<T> extractor) throws SQLException;

    <T> T execute(String sql, PreparedStatementBinder binder, StatementCallback<ResultSet> statementCallback,
                  ResultSetExtractor<T> extractor) throws SQLException;

    int executeUpdate(String sql) throws SQLException;

    int executeUpdate(String sql, PreparedStatementBinder binder) throws SQLException;

    int[] executeBatch(String sql, List<? extends PreparedStatementBinder> binders) throws SQLException;

    <K> K executeUpdate(String sql, PreparedStatementBinder binder, KeyExtractor<K> extractor) throws SQLException;

    <T> T executeCall(String sql, CallableStatementBinder binder, CallableCallback<T> callback) throws SQLException;

}