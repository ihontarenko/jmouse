package org.jmouse.jdbc;

import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface JdbcExecutor {

    <T> T execute(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException;

    default <T> T execute(
            String sql,
            PreparedStatementBinder binder,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, binder, StatementConfigurer.NOOP, callback, extractor);
    }

    default <T> T execute(
            String sql,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, PreparedStatementBinder.NOOP, StatementConfigurer.NOOP, callback, extractor);
    }

    int executeUpdate(
            String sql
    ) throws SQLException;

    int executeUpdate(
            String sql,
            PreparedStatementBinder binder
    ) throws SQLException;

    int[] executeBatch(
            String sql,
            List<? extends PreparedStatementBinder> binders
    ) throws SQLException;

    <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            KeyExtractor<K> extractor
    ) throws SQLException;

    <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            CallableCallback<T> callback
    ) throws SQLException;

}