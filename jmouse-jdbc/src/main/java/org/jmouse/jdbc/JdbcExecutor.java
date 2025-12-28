package org.jmouse.jdbc;

import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Low-level JDBC execution contract for jMouse.
 * <p>
 * {@code JdbcExecutor} is responsible for the mechanical aspects of executing SQL:
 * <ul>
 *     <li>creating JDBC statements (prepared/callable)</li>
 *     <li>binding parameters</li>
 *     <li>configuring statement options (timeouts, fetch size, etc.)</li>
 *     <li>invoking the actual JDBC operation</li>
 *     <li>delegating result processing to callbacks/extractors</li>
 * </ul>
 *
 * <p>
 * This interface is designed to be used by higher-level APIs (e.g. templates)
 * and integrates naturally with a transaction-aware {@link org.jmouse.jdbc.connection.ConnectionProvider}.
 *
 * <h3>Query example</h3>
 * <pre>{@code
 * List<User> users = executor.execute(
 *     "select id, name from users where active = ?",
 *     ps -> ps.setBoolean(1, true),
 *     StatementCallback.QUERY,
 *     rs -> {
 *         List<User> list = new ArrayList<>();
 *         while (rs.next()) {
 *             list.add(new User(rs.getLong("id"), rs.getString("name")));
 *         }
 *         return list;
 *     }
 * );
 * }</pre>
 *
 * <h3>Update example</h3>
 * <pre>{@code
 * int updated = executor.executeUpdate(
 *     "update users set name = ? where id = ?",
 *     ps -> { ps.setString(1, "Alice"); ps.setLong(2, 10L); }
 * );
 * }</pre>
 *
 * <h3>Generated keys example</h3>
 * <pre>{@code
 * Long id = executor.executeUpdate(
 *     "insert into users(name) values(?)",
 *     ps -> ps.setString(1, "Bob"),
 *     keys -> keys.next() ? keys.getLong(1) : null
 * );
 * }</pre>
 *
 * @author jMouse
 */
public interface JdbcExecutor {

    /**
     * Executes a SQL query using a prepared statement and returns a mapped result.
     *
     * @param sql        SQL to execute
     * @param binder     binds statement parameters (may be {@link PreparedStatementBinder#NOOP})
     * @param configurer configures statement options (may be {@link StatementConfigurer#NOOP})
     * @param callback   performs the JDBC operation and returns a {@link ResultSet}
     * @param extractor  maps the {@link ResultSet} to the target result
     * @param <T>        result type
     * @return extracted result
     * @throws SQLException if JDBC access fails
     */
    <T> T execute(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP}.
     */
    default <T> T execute(
            String sql,
            PreparedStatementBinder binder,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, binder, StatementConfigurer.NOOP, callback, extractor);
    }

    /**
     * Convenience overload: uses {@link PreparedStatementBinder#NOOP} and {@link StatementConfigurer#NOOP}.
     */
    default <T> T execute(
            String sql,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, PreparedStatementBinder.NOOP, StatementConfigurer.NOOP, callback, extractor);
    }

    /**
     * Executes a SQL update (INSERT/UPDATE/DELETE) using a prepared statement.
     *
     * @param sql        SQL to execute
     * @param binder     binds statement parameters
     * @param configurer configures statement options
     * @param callback   performs the JDBC operation and returns update count
     * @return update count
     * @throws SQLException if JDBC access fails
     */
    int executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            StatementCallback<Integer> callback
    ) throws SQLException;

    /**
     * Convenience overload: executes update without parameters or config.
     */
    default int executeUpdate(String sql) throws SQLException {
        return executeUpdate(sql, PreparedStatementBinder.NOOP, StatementConfigurer.NOOP, StatementCallback.UPDATE);
    }

    /**
     * Convenience overload: executes update with parameter binding only.
     */
    default int executeUpdate(String sql, PreparedStatementBinder binder) throws SQLException {
        return executeUpdate(sql, binder, StatementConfigurer.NOOP, StatementCallback.UPDATE);
    }

    /**
     * Convenience overload: executes update with parameter binding and statement configuration.
     */
    default int executeUpdate(String sql, PreparedStatementBinder binder, StatementConfigurer configurer)
            throws SQLException {
        return executeUpdate(sql, binder, configurer, StatementCallback.UPDATE);
    }

    /**
     * Executes a batch operation using a prepared statement.
     *
     * @param sql        SQL to execute
     * @param binders    parameter binders applied per batch item
     * @param configurer statement configuration
     * @param callback   performs the JDBC operation and returns update counts
     * @return array of update counts (driver-dependent semantics)
     * @throws SQLException if JDBC access fails
     */
    int[] executeBatch(
            String sql,
            List<? extends PreparedStatementBinder> binders,
            StatementConfigurer configurer,
            StatementCallback<int[]> callback
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP}.
     */
    default int[] executeBatch(String sql, List<? extends PreparedStatementBinder> binders) throws SQLException {
        return executeBatch(sql, binders, StatementConfigurer.NOOP, StatementCallback.BATCH);
    }

    /**
     * Convenience overload: uses {@link StatementCallback#BATCH}.
     */
    default int[] executeBatch(String sql, List<? extends PreparedStatementBinder> binders, StatementConfigurer configurer)
            throws SQLException {
        return executeBatch(sql, binders, configurer, StatementCallback.BATCH);
    }

    /**
     * Executes an update returning generated keys, delegating extraction to a callback.
     *
     * @param sql        SQL to execute (typically INSERT)
     * @param binder     binds statement parameters
     * @param configurer statement configuration (may enable key retrieval)
     * @param callback   invoked with the executed statement and its generated keys
     * @param <K>        key/result type
     * @return extracted keys (or any other callback-defined result)
     * @throws SQLException if JDBC access fails
     */
    <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            KeyUpdateCallback<K> callback
    ) throws SQLException;

    /**
     * Convenience overload: extracts generated keys using a {@link KeyExtractor}.
     */
    default <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return executeUpdate(sql, binder, configurer, (stmt, keys) -> extractor.extract(keys));
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP}.
     */
    default <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return executeUpdate(sql, binder, StatementConfigurer.NOOP, (statement, keys) -> extractor.extract(keys));
    }

    /**
     * Convenience overload: allows passing {@link StatementConfigurer} after the {@link KeyExtractor}.
     */
    default <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            KeyExtractor<K> extractor,
            StatementConfigurer configurer
    ) throws SQLException {
        return executeUpdate(sql, binder, configurer, (statement, keys) -> extractor.extract(keys));
    }

    /**
     * Executes a stored procedure / function call using a callable statement.
     *
     * @param sql        call SQL (e.g. {@code "{call proc(?, ?)}"})
     * @param binder     binds IN/OUT parameters
     * @param configurer statement configuration
     * @param callback   performs the call and returns an arbitrary result
     * @param <T>        result type
     * @return callback result
     * @throws SQLException if JDBC access fails
     */
    <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            CallableCallback<T> callback
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP}.
     */
    default <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            CallableCallback<T> callback
    ) throws SQLException {
        return executeCall(sql, binder, StatementConfigurer.NOOP, callback);
    }

}
