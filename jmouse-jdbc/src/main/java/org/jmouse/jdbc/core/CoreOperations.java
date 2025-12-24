package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.PreparedStatementBinder;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CoreOperations {

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

}
