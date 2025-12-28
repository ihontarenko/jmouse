package org.jmouse.jdbc;

import java.sql.*;
import java.util.List;

import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.*;

public final class SQLExecutor implements JdbcExecutor {

    private final ConnectionProvider connectionProvider;

    public SQLExecutor(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

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
