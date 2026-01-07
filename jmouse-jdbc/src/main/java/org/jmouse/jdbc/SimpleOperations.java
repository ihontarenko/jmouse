package org.jmouse.jdbc;

import org.jmouse.jdbc.mapping.*;
import org.jmouse.jdbc.statement.*;

import java.sql.ResultSet;
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
 *     <li>Allow optional execution hooks via {@link StatementHandler} 📎</li>
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
 * <h3>Using statement hooks</h3>
 * <pre>{@code
 * StatementHandler timing = stmt -> {
 *     long started = System.nanoTime();
 *     // ... store metadata in your sink ...
 * };
 *
 * List<User> users = jdbc.query(
 *     "select * from users",
 *     StatementBinder.NOOP,
 *     StatementConfigurer.fetchSize(500),
 *     timing,
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
     * @param binder     parameter binder (may be {@link StatementBinder#noop()})
     * @param configurer statement configuration (timeouts, fetch size, etc.)
     * @param handler    statement lifecycle hook (may be {@link StatementHandler#noop()})
     * @param mapper     row mapper
     * @param <T>        mapped element type
     * @return optional mapped result, empty if no rows were returned
     * @throws SQLException if JDBC access fails
     */
    <T> Optional<T> querySingle(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}.
     */
    default <T> Optional<T> querySingle(
            String sql,
            StatementBinder binder,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException {
        return querySingle(sql, binder, StatementConfigurer.NOOP, handler, mapper);
    }

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default <T> Optional<T> querySingle(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            RowMapper<T> mapper
    ) throws SQLException {
        return querySingle(sql, binder, configurer, StatementHandler.noop(), mapper);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()} and {@link StatementHandler#noop()()}.
     */
    default <T> Optional<T> querySingle(
            String sql,
            StatementBinder binder,
            RowMapper<T> mapper
    ) throws SQLException {
        return querySingle(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop(), mapper);
    }

    /**
     * Convenience overload: uses {@link StatementBinder#noop()}, {@link StatementConfigurer#noop()},
     * and {@link StatementHandler#noop()}.
     */
    default <T> Optional<T> querySingle(
            String sql,
            RowMapper<T> mapper
    ) throws SQLException {
        return querySingle(sql, StatementBinder.NOOP, StatementConfigurer.NOOP, StatementHandler.noop(), mapper);
    }

    /**
     * Executes a query expected to return <b>exactly one</b> row.
     * <p>
     * Implementations should fail if zero or more than one row is returned.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param handler    statement lifecycle hook
     * @param mapper     row mapper
     * @param <T>        mapped element type
     * @return mapped result
     * @throws SQLException if JDBC access fails
     */
    <T> T queryOne(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default <T> T queryOne(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            RowMapper<T> mapper
    ) throws SQLException {
        return queryOne(sql, binder, configurer, StatementHandler.noop(), mapper);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}.
     */
    default <T> T queryOne(
            String sql,
            StatementBinder binder,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException {
        return queryOne(sql, binder, StatementConfigurer.NOOP, handler, mapper);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()} and {@link StatementHandler#noop()}.
     */
    default <T> T queryOne(
            String sql,
            StatementBinder binder,
            RowMapper<T> mapper
    ) throws SQLException {
        return queryOne(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop(), mapper);
    }

    /**
     * Convenience overload: uses {@link StatementBinder#noop()}, {@link StatementConfigurer#noop()},
     * and {@link StatementHandler#noop()}.
     */
    default <T> T queryOne(
            String sql,
            RowMapper<T> mapper
    ) throws SQLException {
        return queryOne(sql, StatementBinder.NOOP, StatementConfigurer.NOOP, StatementHandler.noop(), mapper);
    }

    /**
     * Executes a query expected to return multiple rows.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param handler    statement lifecycle hook
     * @param mapper     row mapper
     * @param <T>        mapped element type
     * @return list of mapped results (possibly empty)
     * @throws SQLException if JDBC access fails
     */
    <T> List<T> query(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default <T> List<T> query(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            RowMapper<T> mapper
    ) throws SQLException {
        return query(sql, binder, configurer, StatementHandler.noop(), mapper);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}.
     */
    default <T> List<T> query(
            String sql,
            StatementBinder binder,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException {
        return query(sql, binder, StatementConfigurer.NOOP, handler, mapper);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()} and {@link StatementHandler#noop()}.
     */
    default <T> List<T> query(
            String sql,
            StatementBinder binder,
            RowMapper<T> mapper
    ) throws SQLException {
        return query(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop(), mapper);
    }

    /**
     * Convenience overload: uses {@link StatementBinder#noop()}, {@link StatementConfigurer#noop()},
     * and {@link StatementHandler#noop()}.
     */
    default <T> List<T> query(
            String sql,
            RowMapper<T> mapper
    ) throws SQLException {
        return query(sql, StatementBinder.NOOP, StatementConfigurer.NOOP, StatementHandler.noop(), mapper);
    }

    /**
     * Executes a query using a low-level {@link ResultSetExtractor}.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param handler    statement lifecycle hook
     * @param extractor  result set extractor
     * @param <T>        result type
     * @return extracted result
     * @throws SQLException if JDBC access fails
     */
    <T> T query(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            ResultSetExtractor<T> extractor
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default <T> T query(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return query(sql, binder, configurer, StatementHandler.noop(), extractor);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}.
     */
    default <T> T query(
            String sql,
            StatementBinder binder,
            StatementHandler<ResultSet> handler,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return query(sql, binder, StatementConfigurer.NOOP, handler, extractor);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()} and {@link StatementHandler#noop()}.
     */
    default <T> T query(
            String sql,
            StatementBinder binder,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return query(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop(), extractor);
    }

    /**
     * Convenience overload: uses {@link StatementBinder#noop()}, {@link StatementConfigurer#noop()},
     * and {@link StatementHandler#noop()}.
     */
    default <T> T query(
            String sql,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return query(sql, StatementBinder.NOOP, StatementConfigurer.NOOP, StatementHandler.noop(), extractor);
    }

    /**
     * Executes an update operation (INSERT / UPDATE / DELETE).
     *
     * @param sql        SQL statement to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param handler    statement lifecycle hook
     * @return update count
     * @throws SQLException if JDBC access fails
     */
    int update(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<Integer> handler
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default int update(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer
    ) throws SQLException {
        return update(sql, binder, configurer, StatementHandler.noop());
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}.
     */
    default int update(
            String sql,
            StatementBinder binder,
            StatementHandler<Integer> handler
    ) throws SQLException {
        return update(sql, binder, StatementConfigurer.NOOP, handler);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()} and {@link StatementHandler#noop()}.
     */
    default int update(
            String sql,
            StatementBinder binder
    ) throws SQLException {
        return update(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop());
    }

    /**
     * Convenience overload: uses {@link StatementBinder#noop()}, {@link StatementConfigurer#noop()},
     * and {@link StatementHandler#noop()}.
     */
    default int update(String sql) throws SQLException {
        return update(sql, StatementBinder.NOOP, StatementConfigurer.NOOP, StatementHandler.noop());
    }

    /**
     * Executes a batch update.
     *
     * @param sql        SQL statement to execute
     * @param binders    parameter binders applied per batch item
     * @param configurer statement configuration
     * @param handler    statement lifecycle hook
     * @return array of update counts (driver-dependent semantics)
     * @throws SQLException if JDBC access fails
     */
    int[] batchUpdate(
            String sql,
            List<? extends StatementBinder> binders,
            StatementConfigurer configurer,
            StatementHandler<int[]> handler
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default int[] batchUpdate(
            String sql,
            List<? extends StatementBinder> binders,
            StatementConfigurer configurer
    ) throws SQLException {
        return batchUpdate(sql, binders, configurer, StatementHandler.noop());
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}.
     */
    default int[] batchUpdate(
            String sql,
            List<? extends StatementBinder> binders,
            StatementHandler<int[]> handler
    ) throws SQLException {
        return batchUpdate(sql, binders, StatementConfigurer.NOOP, handler);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()} and {@link StatementHandler#noop()}.
     */
    default int[] batchUpdate(
            String sql,
            List<? extends StatementBinder> binders
    ) throws SQLException {
        return batchUpdate(sql, binders, StatementConfigurer.NOOP, StatementHandler.noop());
    }

    /**
     * Executes an update returning generated keys.
     *
     * @param sql        SQL statement to execute
     * @param binder     parameter binder
     * @param configurer statement configuration (may enable key retrieval)
     * @param handler    statement lifecycle hook
     * @param extractor  generated key extractor
     * @param <K>        key/result type
     * @return extracted key value
     * @throws SQLException if JDBC access fails
     */
    <K> K update(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<K> handler,
            KeyExtractor<K> extractor
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default <K> K update(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return update(sql, binder, configurer, StatementHandler.noop(), extractor);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}.
     */
    default <K> K update(
            String sql,
            StatementBinder binder,
            StatementHandler<K> handler,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return update(sql, binder, StatementConfigurer.NOOP, handler, extractor);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()} and {@link StatementHandler#noop()}.
     */
    default <K> K update(
            String sql,
            StatementBinder binder,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return update(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop(), extractor);
    }

    /**
     * Executes a stored procedure or database function call.
     *
     * @param sql        call SQL (e.g. {@code "{call my_proc(?)}"})
     * @param binder     callable statement binder (IN / OUT parameters)
     * @param configurer statement configuration
     * @param handler    statement lifecycle hook
     * @param callback   callable callback
     * @param <T>        result type
     * @return callback result
     * @throws SQLException if JDBC access fails
     */
    <T> T call(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<T> handler,
            CallableCallback<T> callback
    ) throws SQLException;

    /**
     * Convenience overload: uses {@link StatementHandler#noop()}.
     */
    default <T> T call(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            CallableCallback<T> callback
    ) throws SQLException {
        return call(sql, binder, configurer, StatementHandler.noop(), callback);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}.
     */
    default <T> T call(
            String sql,
            CallableStatementBinder binder,
            StatementHandler<T> handler,
            CallableCallback<T> callback
    ) throws SQLException {
        return call(sql, binder, StatementConfigurer.NOOP, handler, callback);
    }

    /**
     * Convenience overload: uses {@link StatementConfigurer#noop()}, {@link StatementHandler#noop()}.
     */
    default <T> T call(
            String sql,
            CallableStatementBinder binder,
            CallableCallback<T> callback
    ) throws SQLException {
        return call(sql, binder, StatementConfigurer.NOOP, StatementHandler.noop(), callback);
    }

}
