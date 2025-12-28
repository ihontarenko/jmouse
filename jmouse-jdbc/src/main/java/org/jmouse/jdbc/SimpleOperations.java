package org.jmouse.jdbc;

import org.jmouse.jdbc.mapping.*;
import org.jmouse.jdbc.statement.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * High-level JDBC operations facade.
 * <p>
 * {@code SimpleOperations} defines a compact, user-facing API for the most common
 * JDBC use cases, while delegating the actual execution mechanics to a lower-level
 * {@link JdbcExecutor}.
 *
 * <h3>Design principles</h3>
 * <ul>
 *     <li>Expose only a few <b>wide</b> (fully-configurable) abstract methods</li>
 *     <li>Provide ergonomic default overloads for common cases</li>
 *     <li>Keep the API declarative and mapper-oriented</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * Optional<User> user = jdbc.querySingle(
 *     "select * from users where id = ?",
 *     ps -> ps.setLong(1, 10L),
 *     (row, i) -> new User(row.getLong("id"), row.getString("name"))
 * );
 *
 * List<User> users = jdbc.query(
 *     "select * from users",
 *     (row, i) -> new User(row.getLong("id"), row.getString("name"))
 * );
 * }</pre>
 *
 * @author jMouse
 */
public interface SimpleOperations {

    /**
     * Executes a query expected to return <b>at most one</b> row.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder (may be {@link PreparedStatementBinder#NOOP})
     * @param configurer statement configuration (timeouts, fetch size, etc.)
     * @param mapper     row mapper
     * @param <T>        mapped element type
     * @return optional mapped result, empty if no rows were returned
     * @throws SQLException if JDBC access fails
     */
    <T> Optional<T> querySingle(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            RowMapper<T> mapper
    ) throws SQLException;

    /**
     * Convenience overload: no parameters, no statement configuration.
     */
    default <T> Optional<T> querySingle(String sql, RowMapper<T> mapper) throws SQLException {
        return querySingle(sql, PreparedStatementBinder.NOOP, StatementConfigurer.NOOP, mapper);
    }

    /**
     * Convenience overload: parameter binding without statement configuration.
     */
    default <T> Optional<T> querySingle(String sql, PreparedStatementBinder binder, RowMapper<T> mapper)
            throws SQLException {
        return querySingle(sql, binder, StatementConfigurer.NOOP, mapper);
    }

    /**
     * Executes a query expected to return <b>exactly one</b> row.
     * <p>
     * Implementations should fail if zero or more than one row is returned.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param mapper     row mapper
     * @param <T>        mapped element type
     * @return mapped result
     * @throws SQLException if JDBC access fails
     */
    <T> T queryOne(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            RowMapper<T> mapper
    ) throws SQLException;

    /**
     * Convenience overload: no parameters, no statement configuration.
     */
    default <T> T queryOne(String sql, RowMapper<T> mapper) throws SQLException {
        return queryOne(sql, PreparedStatementBinder.NOOP, StatementConfigurer.NOOP, mapper);
    }

    /**
     * Convenience overload: parameter binding without statement configuration.
     */
    default <T> T queryOne(String sql, PreparedStatementBinder binder, RowMapper<T> mapper) throws SQLException {
        return queryOne(sql, binder, StatementConfigurer.NOOP, mapper);
    }

    /**
     * Executes a query expected to return multiple rows.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param mapper     row mapper
     * @param <T>        mapped element type
     * @return list of mapped results (possibly empty)
     * @throws SQLException if JDBC access fails
     */
    <T> List<T> query(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            RowMapper<T> mapper
    ) throws SQLException;

    /**
     * Convenience overload: no parameters, no statement configuration.
     */
    default <T> List<T> query(String sql, RowMapper<T> mapper) throws SQLException {
        return query(sql, PreparedStatementBinder.NOOP, StatementConfigurer.NOOP, mapper);
    }

    /**
     * Convenience overload: parameter binding without statement configuration.
     */
    default <T> List<T> query(String sql, PreparedStatementBinder binder, RowMapper<T> mapper) throws SQLException {
        return query(sql, binder, StatementConfigurer.NOOP, mapper);
    }

    /**
     * Executes a query using a low-level {@link ResultSetExtractor}.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param extractor result set extractor
     * @param <T>        result type
     * @return extracted result
     * @throws SQLException if JDBC access fails
     */
    <T> T query(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            ResultSetExtractor<T> extractor
    ) throws SQLException;

    /**
     * Convenience overload: no parameters, no statement configuration.
     */
    default <T> T query(String sql, ResultSetExtractor<T> extractor) throws SQLException {
        return query(sql, PreparedStatementBinder.NOOP, StatementConfigurer.NOOP, extractor);
    }

    /**
     * Convenience overload: parameter binding without statement configuration.
     */
    default <T> T query(String sql, PreparedStatementBinder binder, ResultSetExtractor<T> extractor)
            throws SQLException {
        return query(sql, binder, StatementConfigurer.NOOP, extractor);
    }

    /**
     * Executes an update operation (INSERT / UPDATE / DELETE).
     *
     * @param sql        SQL statement to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @return update count
     * @throws SQLException if JDBC access fails
     */
    int update(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer
    ) throws SQLException;

    /**
     * Convenience overload: no parameters, no statement configuration.
     */
    default int update(String sql) throws SQLException {
        return update(sql, PreparedStatementBinder.NOOP, StatementConfigurer.NOOP);
    }

    /**
     * Convenience overload: parameter binding without statement configuration.
     */
    default int update(String sql, PreparedStatementBinder binder) throws SQLException {
        return update(sql, binder, StatementConfigurer.NOOP);
    }

    /**
     * Executes a batch update.
     *
     * @param sql        SQL statement to execute
     * @param binders    parameter binders applied per batch item
     * @param configurer statement configuration
     * @return array of update counts (driver-dependent semantics)
     * @throws SQLException if JDBC access fails
     */
    int[] batchUpdate(
            String sql,
            List<? extends PreparedStatementBinder> binders,
            StatementConfigurer configurer
    ) throws SQLException;

    /**
     * Convenience overload: no statement configuration.
     */
    default int[] batchUpdate(String sql, List<? extends PreparedStatementBinder> binders) throws SQLException {
        return batchUpdate(sql, binders, StatementConfigurer.NOOP);
    }

    /**
     * Executes an update returning generated keys.
     *
     * @param sql        SQL statement to execute
     * @param binder     parameter binder
     * @param configurer statement configuration (may enable key retrieval)
     * @param extractor generated key extractor
     * @param <K>        key/result type
     * @return extracted key value
     * @throws SQLException if JDBC access fails
     */
    <K> K update(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            KeyExtractor<K> extractor
    ) throws SQLException;

    /**
     * Convenience overload: parameter binding without statement configuration.
     */
    default <K> K update(String sql, PreparedStatementBinder binder, KeyExtractor<K> extractor) throws SQLException {
        return update(sql, binder, StatementConfigurer.NOOP, extractor);
    }

    /**
     * Executes a stored procedure or database function call.
     *
     * @param sql        call SQL (e.g. {@code "{call my_proc(?)}"})
     * @param binder     callable statement binder (IN / OUT parameters)
     * @param configurer statement configuration
     * @param callback   callable callback
     * @param <T>        result type
     * @return callback result
     * @throws SQLException if JDBC access fails
     */
    <T> T call(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            CallableCallback<T> callback
    ) throws SQLException;

    /**
     * Convenience overload: no parameters, no statement configuration.
     */
    default <T> T call(String sql, CallableCallback<T> callback) throws SQLException {
        return call(sql, CallableStatementBinder.NOOP, StatementConfigurer.NOOP, callback);
    }

    /**
     * Convenience overload: callable binder without statement configuration.
     */
    default <T> T call(String sql, CallableStatementBinder binder, CallableCallback<T> callback) throws SQLException {
        return call(sql, binder, StatementConfigurer.NOOP, callback);
    }
}
