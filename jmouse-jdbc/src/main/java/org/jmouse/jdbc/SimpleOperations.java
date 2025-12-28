package org.jmouse.jdbc;

import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.CallableCallback;
import org.jmouse.jdbc.statement.CallableStatementBinder;
import org.jmouse.jdbc.statement.PreparedStatementBinder;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SimpleOperations {

    <T> Optional<T> querySingle(String sql, RowMapper<T> mapper) throws SQLException;

    <T> Optional<T> querySingle(String sql, PreparedStatementBinder binder, RowMapper<T> mapper) throws SQLException;

    <T> T queryOne(String sql, RowMapper<T> mapper) throws SQLException;

    <T> T queryOne(String sql, PreparedStatementBinder binder, RowMapper<T> mapper) throws SQLException;

    <T> List<T> query(String sql, RowMapper<T> mapper) throws SQLException;

    <T> List<T> query(String sql, PreparedStatementBinder binder, RowMapper<T> mapper) throws SQLException;

    <T> T query(String sql, ResultSetExtractor<T> extractor) throws SQLException;

    <T> T query(String sql, PreparedStatementBinder binder, ResultSetExtractor<T> extractor) throws SQLException;

    int update(String sql) throws SQLException;

    int update(String sql, PreparedStatementBinder binder) throws SQLException;

    int[] batchUpdate(String sql, List<? extends PreparedStatementBinder> binders) throws SQLException;

    <K> K update(String sql, PreparedStatementBinder binder, KeyExtractor<K> extractor) throws SQLException;

    <T> T call(String sql, CallableStatementBinder binder, CallableCallback<T> callback) throws SQLException;

    <T> T call(String sql, CallableCallback<T> callback) throws SQLException;

}
