package org.jmouse.jdbc;

import org.jmouse.jdbc.mapping.*;
import org.jmouse.jdbc.statement.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link SimpleOperations} built on top of {@link JdbcExecutor}.
 * <p>
 * {@code SimpleTemplate} represents the <b>high-level JDBC template layer</b> in jMouse.
 * It translates user-oriented operations (query / update / call) into
 * executor-level invocations with the appropriate callbacks and extractors.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Compose {@link RowMapper} into {@link ResultSetExtractor} variants</li>
 *     <li>Select correct {@link StatementCallback} for each operation</li>
 *     <li>Delegate all low-level JDBC mechanics to {@link JdbcExecutor}</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *     <li>This class is <b>stateless</b> and thread-safe</li>
 *     <li>Transaction handling is external and executor-driven</li>
 *     <li>Intended as the primary user-facing JDBC entry point</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * SimpleOperations jdbc = new SimpleTemplate(executor);
 *
 * Optional<User> user = jdbc.querySingle(
 *     "select * from users where id = ?",
 *     ps -> ps.setLong(1, 10L),
 *     (row, i) -> new User(row.getLong("id"), row.getString("name"))
 * );
 * }</pre>
 *
 * @author jMouse
 */
public class SimpleTemplate implements SimpleOperations {

    /**
     * Backing executor responsible for actual JDBC interaction.
     */
    private final JdbcExecutor executor;

    /**
     * Creates a new {@code SimpleTemplate} delegating to the given {@link JdbcExecutor}.
     *
     * @param executor JDBC executor to use
     */
    public SimpleTemplate(JdbcExecutor executor) {
        this.executor = executor;
    }

    /**
     * Executes a query expected to return <b>at most one</b> row.
     * <p>
     * Internally uses {@link StrictSingleResultSetExtractor} to enforce result size.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param mapper     row mapper
     * @param <T>        mapped element type
     * @return optional mapped result
     * @throws SQLException if JDBC access fails or more than one row is returned
     */
    @Override
    public <T> Optional<T> querySingle(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException {
        return executor.execute(
                sql, binder, configurer, handler,
                StatementCallback.QUERY,
                new StrictSingleResultSetExtractor<>(mapper, sql)
        );
    }

    /**
     * Executes a query expected to return <b>exactly one</b> row.
     *
     * @param sql        SQL query to execute
     * @param binder     parameter binder
     * @param configurer statement configuration
     * @param mapper     row mapper
     * @param <T>        mapped element type
     * @return mapped result
     * @throws SQLException if JDBC access fails or result size is not exactly one
     */
    @Override
    public <T> T queryOne(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException {
        return query(sql, binder, configurer, handler, new SingleResultSetExtractor<>(mapper));
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
    @Override
    public <T> List<T> query(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            RowMapper<T> mapper
    ) throws SQLException {
        return query(sql, binder, configurer, handler, new ListResultSetExtractor<>(mapper));
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
    @Override
    public <T> T query(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<ResultSet> handler,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        return executor.execute(sql, binder, configurer, handler, StatementCallback.QUERY, extractor);
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
    @Override
    public int update(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<Integer> handler
    ) throws SQLException {
        return executor.executeUpdate(sql, binder, configurer, handler, StatementCallback.UPDATE);
    }

    /**
     * Executes a batch update operation.
     *
     * @param sql        SQL statement to execute
     * @param binders    parameter binders applied per batch item
     * @param configurer statement configuration
     * @return array of update counts (driver-dependent semantics)
     * @throws SQLException if JDBC access fails
     */
    @Override
    public int[] batchUpdate(
            String sql,
            List<? extends StatementBinder> binders,
            StatementConfigurer configurer,
            StatementHandler<int[]> handler
    ) throws SQLException {
        return executor.executeBatch(sql, binders, configurer, handler, StatementCallback.BATCH);
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
    @Override
    public <K> K update(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<K> handler,
            KeyExtractor<K> extractor
    ) throws SQLException {
        return executor.executeUpdate(
                sql, binder, configurer, handler, (statement, keys) -> extractor.extract(keys)
        );
    }

    /**
     * Executes a stored procedure or database function call.
     *
     * @param sql        call SQL
     * @param binder     callable statement binder
     * @param configurer statement configuration
     * @param callback   callable callback
     * @param <T>        result type
     * @return callback result
     * @throws SQLException if JDBC access fails
     */
    @Override
    public <T> T call(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler<T> handler,
            CallableCallback<T> callback
    ) throws SQLException {
        return executor.executeCall(sql, binder, configurer, handler, callback);
    }

    /**
     * Exposes the underlying {@link JdbcExecutor}.
     * <p>
     * Intended for subclasses and advanced customization.
     *
     * @return backing JDBC executor
     */
    protected JdbcExecutor executor() {
        return executor;
    }
}
