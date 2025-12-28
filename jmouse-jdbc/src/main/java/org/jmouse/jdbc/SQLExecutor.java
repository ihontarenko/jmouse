package org.jmouse.jdbc;

import java.sql.*;
import java.util.List;

import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.*;

/**
 * Default {@link JdbcExecutor} implementation performing direct JDBC execution.
 * <p>
 * {@code SQLExecutor} is the "raw" executor that:
 * <ul>
 *     <li>obtains a {@link Connection} from a {@link ConnectionProvider}</li>
 *     <li>creates {@link PreparedStatement}/{@link CallableStatement}</li>
 *     <li>binds parameters via {@link PreparedStatementBinder}/{@link CallableStatementBinder}</li>
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
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
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

            resultSet = callback.doStatementExecute(statement);
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
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            StatementCallback<Integer> callback
    ) throws SQLException {
        Connection connection = connectionProvider.getConnection();
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement(sql);

            binder.bind(statement);
            configurer.configure(statement);

            return callback.doStatementExecute(statement);

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
            List<? extends PreparedStatementBinder> binders,
            StatementConfigurer configurer,
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

            for (PreparedStatementBinder binder : binders) {
                if (binder != null) {
                    binder.bind(statement);
                }
                statement.addBatch();
            }

            return callback.doStatementExecute(statement);
        } finally {
            JdbcSupport.closeQuietly(statement);
            connectionProvider.release(connection);
        }
    }

    /**
     * Executes an update and extracts generated keys using a {@link KeyExtractor}.
     * <p>
     * Uses {@link PreparedStatement#RETURN_GENERATED_KEYS}.
     *
     * @param sql       SQL to execute
     * @param binder    binds statement parameters
     * @param extractor extracts generated keys from the {@link ResultSet} returned by {@link PreparedStatement#getGeneratedKeys()}
     * @param <K>       key/result type
     * @return extracted key result
     * @throws SQLException if JDBC access fails
     */
    @Override
    public <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            KeyExtractor<K> extractor
    ) throws SQLException {
        Connection connection = connectionProvider.getConnection();

        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                return extractor.extract(keys);
            }
        } finally {
            connectionProvider.release(connection);
        }
    }

    /**
     * Executes an update that returns generated keys, delegating key processing to a {@link KeyUpdateCallback}.
     * <p>
     * Uses {@link PreparedStatement#RETURN_GENERATED_KEYS}.
     *
     * @param sql        SQL to execute
     * @param binder     binds statement parameters
     * @param configurer configures statement options
     * @param callback   processes the executed statement and its generated keys
     * @param <K>        key/result type
     * @return callback result
     * @throws SQLException if JDBC access fails
     */
    @Override
    public <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            StatementConfigurer configurer,
            KeyUpdateCallback<K> callback
    ) throws SQLException {
        Connection        connection = connectionProvider.getConnection();
        PreparedStatement statement  = null;

        try {
            statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

            binder.bind(statement);
            configurer.configure(statement);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                return callback.doStatementExecute(statement, keys);
            }
        } finally {
            JdbcSupport.closeQuietly(statement);
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
            CallableCallback<T> callback
    ) throws SQLException {
        Connection connection = connectionProvider.getConnection();
        CallableStatement statement = null;

        try {
            statement = connection.prepareCall(sql);
            binder.bind(statement);
            configurer.configure(statement);
            return callback.doInCallable(statement);
        } finally {
            JdbcSupport.closeQuietly(statement);
            connectionProvider.release(connection);
        }
    }

}
