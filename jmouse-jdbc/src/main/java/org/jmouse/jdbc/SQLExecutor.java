package org.jmouse.jdbc;

import java.sql.*;
import java.util.List;

import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.*;

/**
 * Default {@link JdbcExecutor} implementation performing direct JDBC execution.
 * <p>
 * {@code SQLExecutor} is the "raw" executor that:
 * <ul>
 *     <li>obtains a {@link Connection} from a {@link ConnectionProvider}</li>
 *     <li>creates {@link PreparedStatement}/{@link CallableStatement}</li>
 *     <li>binds parameters via {@link StatementBinder}/{@link CallableStatementBinder}</li>
 *     <li>applies statement configuration via {@link StatementConfigurer}</li>
 *     <li>executes JDBC operations via {@link StatementCallback}/{@link KeyUpdateCallback}/{@link CallableCallback}</li>
 *     <li>ensures resources are closed and connections are released</li>
 * </ul>
 *
 * <h3>Execution lifecycle</h3>
 * <pre>{@code
 * Connection c = provider.getConnection();
 * try {
 *   PreparedStatement ps = c.prepareStatement(sql);
 *   binder.bind(ps);
 *   configurer.configure(ps);
 *   // execute via callback and/or extract results
 * } finally {
 *   close(ResultSet/Statement);
 *   provider.release(c);
 * }
 * }</pre>
 *
 * <p>
 * ⚠️ Transaction participation is determined by the configured {@link ConnectionProvider}.
 * For example, a transaction-aware provider may return a thread-bound connection.
 *
 * @author jMouse
 */
public final class SQLExecutor implements JdbcExecutor {

    /**
     * Provides JDBC connections (raw, pooled, or transaction-aware).
     */
    private final ConnectionProvider connectionProvider;

    /**
     * Creates a new {@code SQLExecutor}.
     *
     * @param connectionProvider connection provider used for all executions
     */
    public SQLExecutor(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Executes a query-like statement returning a {@link ResultSet}, then maps it using a {@link ResultSetExtractor}.
     *
     * @param sql        SQL to execute
     * @param binder     binds statement parameters
     * @param configurer configures statement options
     * @param callback   executes the statement and returns a {@link ResultSet}
     * @param extractor  maps the {@link ResultSet}
     * @param <T>        result type
     * @return extracted result
     * @throws SQLException if JDBC access fails
     */
    @Override
    public <T> T execute(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {
        Connection        connection = connectionProvider.getConnection();
        PreparedStatement statement  = null;
        ResultSet         resultSet  = null;

        try {
            statement = connection.prepareStatement(sql);

            binder.bind(statement);
            configurer.configure(statement);

            resultSet = handler.handle(statement, callback::doStatementExecute);
            return extractor.extract(resultSet);

        } finally {
            JdbcSupport.closeQuietly(resultSet, statement);
            connectionProvider.release(connection);
        }
    }

    /**
     * Executes an update-like statement (INSERT/UPDATE/DELETE) via a {@link StatementCallback}.
     *
     * @param sql        SQL to execute
     * @param binder     binds statement parameters
     * @param configurer configures statement options
     * @param callback   executes the statement and returns update count
     * @return update count
     * @throws SQLException if JDBC access fails
     */
    @Override
    public int executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            StatementCallback<Integer> callback
    ) throws SQLException {
        Connection connection = connectionProvider.getConnection();
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement(sql);

            binder.bind(statement);
            configurer.configure(statement);

            return handler.handle(statement, callback::doStatementExecute);
        } finally {
            JdbcSupport.closeQuietly(statement);
            connectionProvider.release(connection);
        }
    }

    /**
     * Executes a batch update.
     * <p>
     * If {@code binders} is empty, this method returns an empty array and does not access the database.
     *
     * @param sql        SQL to execute
     * @param binders    per-batch binders (nullable items are ignored)
     * @param configurer configures statement options
     * @param callback   executes the statement and returns batch results
     * @return batch update counts
     * @throws SQLException if JDBC access fails
     */
    @Override
    public int[] executeBatch(
            String sql,
            List<? extends StatementBinder> binders,
            StatementConfigurer configurer,
            StatementHandler handler,
            StatementCallback<int[]> callback
    ) throws SQLException {
        if (binders.isEmpty()) {
            return new int[0];
        }

        Connection        connection = connectionProvider.getConnection();
        PreparedStatement statement  = null;

        try {
            statement = connection.prepareStatement(sql);
            configurer.configure(statement);

            for (StatementBinder binder : binders) {
                if (binder != null) {
                    binder.bind(statement);
                }
                statement.addBatch();
            }

            return handler.handle(statement, callback::doStatementExecute);
        } finally {
            JdbcSupport.closeQuietly(statement);
            connectionProvider.release(connection);
        }
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
    @Override
    public <K> K executeUpdate(
            String sql,
            StatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            KeyUpdateCallback<K> callback
    ) throws SQLException {
        Connection connection = connectionProvider.getConnection();

        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            configurer.configure(statement);

            return handler.handle(statement, s -> {
                s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) {
                    return callback.doStatementExecute(s, keys);
                }
            });
        } finally {
            connectionProvider.release(connection);
        }
    }

    /**
     * Executes a callable statement (stored procedure / function call).
     *
     * @param sql        call SQL
     * @param binder     binds callable parameters (IN / OUT)
     * @param configurer configures statement options
     * @param callback   executes the call and maps its result
     * @param <T>        result type
     * @return callback result
     * @throws SQLException if JDBC access fails
     */
    @Override
    public <T> T executeCall(
            String sql,
            CallableStatementBinder binder,
            StatementConfigurer configurer,
            StatementHandler handler,
            CallableCallback<T> callback
    ) throws SQLException {
        Connection connection = connectionProvider.getConnection();
        CallableStatement statement = null;

        try {
            statement = connection.prepareCall(sql);
            binder.bind(statement);
            configurer.configure(statement);
            return handler.handle(statement, callback::doInCallable);
        } finally {
            JdbcSupport.closeQuietly(statement);
            connectionProvider.release(connection);
        }
    }

}
