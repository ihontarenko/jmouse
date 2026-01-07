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
 *     <li>invoking execution hooks via {@link StatementHandler} (optional)</li>
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
 *     StatementConfigurer.NOOP,
 *     StatementHandler.NOOP,
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
 *     ps -> { ps.setString(1, "Alice"); ps.setLong(2, 10L); },
 *     StatementConfigurer.NOOP,
 *     StatementHandler.NOOP
 * );
 * }</pre>
 *
 * <h3>Generated keys example</h3>
 * <pre>{@code
 * Long id = executor.executeUpdate(
 *     "insert into users(name) values(?)",
 *     ps -> ps.setString(1, "Bob"),
 *     StatementConfigurer.NOOP,
 *     StatementHandler.NOOP,
 *     (stmt, keys) -> keys.next() ? keys.getLong(1) : null
 * );
 * }</pre>
 *
 * @author jMouse
 */
public interface JdbcExecutor {

    /**
     * Executes a SQL query using a prepared statement and returns a mapped result.
     *
     * <p>
     * This is the <b>widest</b> query signature that exposes all extension points:
     * binding, configuration, execution hooks, the actual execution callback,
     * and the result extractor.
     *
     * @param sql        SQL to execute
     * @param binder     binds statement parameters (may be {@link StatementBinder#NOOP})
     * @param configurer configures statement options (may be {@link StatementConfigurer#NOOP})
     * @param handler    execution hook for statement lifecycle/metadata (may be {@link StatementHandler#NOOP})
     * @param callback   performs the JDBC operation and returns a {@link ResultSet}
     * @param extractor  maps the {@link ResultSet} to the target result
     * @param <T>        result type
     * @return extracted result
     * @throws SQLException if JDBC access fails
     */
    <T> T execute(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#NOOP_QUERY}.
     *
     * @param sql        SQL to execute
     * @param binder     binds statement parameters
     * @param configurer configures statement options
     * @param callback   performs the JDBC operation and returns a {@link ResultSet}
     * @param extractor  maps the {@link ResultSet}
     * @param <T>        result type
     * @return extracted result
     * @throws SQLException if JDBC access fails
     */
    default <T> T execute(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, binder, configurer, StatementHandler.NOOP_QUERY, callback, extractor);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP} and {@link StatementHandler#NOOP_QUERY}.
     *
     * @param sql       SQL to execute
     * @param binder    binds statement parameters
     * @param callback  performs the JDBC operation and returns a {@link ResultSet}
     * @param extractor maps the {@link ResultSet}
     * @param <T>       result type
     * @return extracted result
     * @throws SQLException if JDBC access fails
     */
    default <T> T execute(
            String sql,
            StatementBinder binder,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, binder, StatementConfigurer.NOOP, StatementHandler.NOOP_QUERY, callback, extractor);
    }

    /**
     * Convenience overload: uses {@link StatementBinder#NOOP}, {@link StatementConfigurer#NOOP}
     * and {@link StatementHandler#NOOP_QUERY}.
     *
     * @param sql       SQL to execute
     * @param callback  performs the JDBC operation and returns a {@link ResultSet}
     * @param extractor maps the {@link ResultSet}
     * @param <T>       result type
     * @return extracted result
     * @throws SQLException if JDBC access fails
     */
    default <T> T execute(
            String sql,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return execute(sql, StatementBinder.NOOP, StatementConfigurer.NOOP, StatementHandler.NOOP_QUERY, callback, extractor);
    }

    /**
     * Executes a SQL update (INSERT/UPDATE/DELETE) using a prepared statement.
     * <p>
     * This is the <b>widest</b> update signature and includes {@link StatementHandler}
     * as an execution hook.
     *
     * @param sql        SQL to execute
     * @param binder     binds statement parameters
     * @param configurer configures statement options
     * @param handler    execution hook for statement lifecycle/metadata
     * @param callback   performs the JDBC operation and returns update count
     * @return update count
     * @throws SQLException if JDBC access fails
     */
    int executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<Integer> handler,
            StatementCallback<Integer> callback
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#NOOP_QUERY}.
     */
    default int executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementCallback<Integer> callback
    )
            throws SQLException {
        return executeUpdate(sql, binder, configurer, StatementHandler.NOOP_UPDATE, callback);
    }

    /**
     * Convenience overload: uses {@link StatementCallback#UPDATE} and {@link StatementHandler#NOOP_UPDATE}.
     */
    default int executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer
    )
            throws SQLException {
        return executeUpdate(sql, binder, configurer, StatementHandler.NOOP_UPDATE, StatementCallback.UPDATE);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP}, {@link StatementHandler#NOOP_UPDATE},
     * and {@link StatementCallback#UPDATE}.
     */
    default int executeUpdate(
            String sql,
            StatementBinder binder
    )
            throws SQLException {
        return executeUpdate(sql, binder, StatementConfigurer.NOOP, StatementHandler.NOOP_UPDATE, StatementCallback.UPDATE);
    }

    /**
     * Convenience overload: executes update without parameters or configuration.
     */
    default int executeUpdate(String sql) throws SQLException {
        return executeUpdate(
                sql,
                StatementBinder.NOOP,
                StatementConfigurer.NOOP,
                StatementHandler.NOOP_UPDATE,
                StatementCallback.UPDATE
        );
    }

    /**
     * Executes a batch operation using a prepared statement.
     * <p>
     * This is the <b>widest</b> batch signature and includes {@link StatementHandler}
     * as an execution hook.
     *
     * @param sql        SQL to execute
     * @param binders    parameter binders applied per batch item
     * @param configurer statement configuration
     * @param handler    execution hook for statement lifecycle/metadata
     * @param callback   performs the JDBC operation and returns update counts
     * @return array of update counts (driver-dependent semantics)
     * @throws SQLException if JDBC access fails
     */
    int[] executeBatch(
            String sql,
            List<? extends StatementBinder> binders,
            StatementConfigurer configurer,
            StatementHandler<int[]> handler,
            StatementCallback<int[]> callback
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementCallback#BATCH}.
     */
    default int[] executeBatch(
            String sql,
            List<? extends StatementBinder> binders,
            StatementConfigurer configurer,
            StatementHandler<int[]> handler
    ) throws SQLException {
        return executeBatch(sql, binders, configurer, handler, StatementCallback.BATCH);
    }

    /**
     * Convenience overload: uses {@link StatementHandler#NOOP_BATCH} and {@link StatementCallback#BATCH}.
     */
    default int[] executeBatch(
            String sql,
            List<? extends StatementBinder> binders,
            StatementConfigurer configurer
    ) throws SQLException {
        return executeBatch(sql, binders, configurer, StatementHandler.NOOP_BATCH, StatementCallback.BATCH);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP}, {@link StatementHandler#NOOP_BATCH},
     * and {@link StatementCallback#BATCH}.
     */
    default int[] executeBatch(
            String sql,
            List<? extends StatementBinder> binders
    ) throws SQLException {
        return executeBatch(sql, binders, StatementConfigurer.NOOP, StatementHandler.NOOP_BATCH, StatementCallback.BATCH);
    }

    /**
     * Executes an update returning generated keys, delegating extraction to a callback.
     * <p>
     * This is the <b>widest</b> signature for key-returning updates and exposes a
     * {@link StatementHandler} hook.
     *
     * @param sql        SQL to execute (typically INSERT)
     * @param binder     binds statement parameters
     * @param configurer statement configuration (may enable key retrieval)
     * @param handler    execution hook for statement lifecycle/metadata
     * @param callback   invoked with the executed statement and its generated keys
     * @param <K>        key/result type
     * @return extracted keys (or any other callback-defined result)
     * @throws SQLException if JDBC access fails
     */
    <K> K executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<K> handler,
            KeyUpdateCallback<K> callback
    ) throws SQLException;

    /**
     * Convenience overload: extracts generated keys using a {@link KeyExtractor}.
     */
    default <K> K executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<K> handler,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return executeUpdate(sql, binder, configurer, handler,
                             (statement, keys) -> extractor.extract(keys));
    }

    /**
     * Convenience overload: extracts generated keys using a {@link KeyExtractor}.
     * Uses {@link StatementHandler#noop()}.
     */
    default <K> K executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return executeUpdate(sql, binder, configurer, StatementHandler.noop(),
                             (statement, keys) -> extractor.extract(keys));
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP} and {@link StatementHandler#noop()}.
     */
    default <K> K executeUpdate(
            String sql,
            StatementBinder binder,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return executeUpdate(
                sql, binder,
                StatementConfigurer.NOOP,
                StatementHandler.noop(),
                (statement, keys) -> extractor.extract(keys)
        );
    }

    /**
     * Executes a stored procedure / function call using a callable statement.
     * <p>
     * This is the <b>widest</b> call signature and includes {@link StatementHandler}
     * as an execution hook.
     *
     * @param sql        call SQL (e.g. {@code "{call proc(?, ?)}"})
     * @param binder     binds IN/OUT parameters
     * @param configurer statement configuration
     * @param handler    execution hook for statement lifecycle/metadata
     * @param callback   performs the call and returns an arbitrary result
     * @param <T>        result type
     * @return callback result
     * @throws SQLException if JDBC access fails
     */
    <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<T> handler,
            CallableCallback<T> callback
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            CallableCallback<T> callback
    ) throws SQLException {
        return executeCall(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop(), callback);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#NOOP} and {@link StatementHandler#noop()}.
     */
    default <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            CallableCallback<T> callback
    ) throws SQLException {
        return executeCall(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop(), callback);
    }

}
