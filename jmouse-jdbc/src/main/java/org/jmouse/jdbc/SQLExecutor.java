package org.jmouse.jdbc;

import java.sql.*;
import java.util.List;
import java.util.Objects;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.intercept.QueryTimeoutSupport;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.*;
import org.jmouse.jdbc.transaction.JdbcResourceHolder;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;

/**
 * Direct JDBC execution with deterministic resource ownership.
 * Statements/results close before the provider releases the connection; cleanup failures are suppressed
 * on the original failure by try-with-resources. Transaction-bound connections are retained by the provider.
 */
public final class SQLExecutor implements JdbcExecutor {

    private final ConnectionProvider connectionProvider;
    private final TransactionContextHolder contexts;

    public SQLExecutor(ConnectionProvider connectionProvider) {
        this(connectionProvider, TransactionContextAccessSupport.current());
    }

    public SQLExecutor(ConnectionProvider connectionProvider, TransactionContextHolder contexts) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.contexts = Objects.requireNonNull(contexts, "contexts");
    }

    @Override
    public <T> T execute(
        String sql,
        StatementBinder binder,
        StatementConfigurer configurer,
        StatementHandler<ResultSet> handler,
        StatementCallback<ResultSet> callback,
        ResultSetExtractor<T> extractor
    ) throws SQLException {
        try (
            ConnectionLease lease = acquire();
            PreparedStatement statement = lease.connection().prepareStatement(sql)
        ) {
            binder.bind(statement);
            configure(statement, configurer);
            try (ResultSet rows = handler.handle(statement, callback::doStatementExecute)) {
                return extractor.extract(rows);
            }
        }
    }

    @Override
    public int executeUpdate(
        String sql,
        StatementBinder binder,
        StatementConfigurer configurer,
        StatementHandler<Integer> handler,
        StatementCallback<Integer> callback
    ) throws SQLException {
        try (
            ConnectionLease lease = acquire();
            PreparedStatement statement = lease.connection().prepareStatement(sql)
        ) {
            binder.bind(statement);
            configure(statement, configurer);
            return handler.handle(statement, callback::doStatementExecute);
        }
    }

    @Override
    public int[] executeBatch(
        String sql,
        List<? extends StatementBinder> binders,
        StatementConfigurer configurer,
        StatementHandler<int[]> handler,
        StatementCallback<int[]> callback
    ) throws SQLException {
        Objects.requireNonNull(binders, "binders");
        if (binders.stream().noneMatch(Objects::nonNull)) {
            return new int[0];
        }
        try (
            ConnectionLease lease = acquire();
            PreparedStatement statement = lease.connection().prepareStatement(sql)
        ) {
            configure(statement, configurer);
            for (StatementBinder binder : binders) {
                if (binder == null) {
                    continue;
                }
                statement.clearParameters();
                binder.bind(statement);
                statement.addBatch();
            }
            return handler.handle(statement, callback::doStatementExecute);
        }
    }

    @Override
    public <K> K executeUpdate(
        String sql,
        StatementBinder binder,
        StatementConfigurer configurer,
        StatementHandler<K> handler,
        KeyUpdateCallback<K> callback
    ) throws SQLException {
        try (
            ConnectionLease lease = acquire();
            PreparedStatement statement = lease
                .connection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            binder.bind(statement);
            configure(statement, configurer);
            return handler.handle(statement, prepared -> {
                prepared.executeUpdate();
                try (ResultSet keys = prepared.getGeneratedKeys()) {
                    return callback.doStatementExecute(prepared, keys);
                }
            });
        }
    }

    @Override
    public <T> T executeCall(
        String sql,
        CallableStatementBinder binder,
        StatementConfigurer configurer,
        StatementHandler<T> handler,
        CallableCallback<T> callback
    ) throws SQLException {
        try (
            ConnectionLease lease = acquire();
            CallableStatement statement = lease.connection().prepareCall(sql)
        ) {
            binder.bind(statement);
            configure(statement, configurer);
            return handler.handle(statement, callback::doInCallable);
        }
    }

    private ConnectionLease acquire() throws SQLException {
        return new ConnectionLease(
            Objects.requireNonNull(connectionProvider.getConnection(), "ConnectionProvider returned null"),
            connectionProvider
        );
    }

    private void configure(Statement statement, StatementConfigurer configurer) throws SQLException {
        configurer.configure(statement);
        JdbcResourceHolder holder = contexts.getResource(JdbcResourceHolder.class);
        if (holder != null && holder.belongsTo(connectionProvider.getResourceKey())) {
            QueryTimeoutSupport.applyIfPresent(statement, contexts.getContext());
        }
    }

    /** A lease releases through its strategy, never by directly closing a transaction-owned connection. */
    private record ConnectionLease(
        Connection connection,
        ConnectionProvider provider
    ) implements AutoCloseable {
        @Override
        public void close() {
            provider.release(connection);
        }
    }
}
