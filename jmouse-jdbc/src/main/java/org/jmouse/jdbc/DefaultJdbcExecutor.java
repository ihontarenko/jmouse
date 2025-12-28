package org.jmouse.jdbc;

import java.sql.*;
import java.util.List;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.CallableCallback;
import org.jmouse.jdbc.statement.CallableStatementBinder;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;

public final class DefaultJdbcExecutor implements JdbcExecutor {

    private final ConnectionProvider connectionProvider;

    public DefaultJdbcExecutor(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public <T> T execute(
            String sql,
            StatementCallback<ResultSet> statementCallback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {

        Connection        connection = connectionProvider.getConnection();
        PreparedStatement statement  = null;
        ResultSet         resultSet  = null;

        try {
            statement = connection.prepareStatement(sql);
            resultSet = statementCallback.doWithStatement(statement);
            return extractor.extract(resultSet);

        } finally {
            JdbcSupport.closeQuietly(resultSet);
            JdbcSupport.closeQuietly(statement);
            connectionProvider.release(connection);
        }
    }

    @Override
    public <T> T execute(
            String sql,
            PreparedStatementBinder binder,
            StatementCallback<ResultSet> callback,
            ResultSetExtractor<T> extractor
    ) throws SQLException {

        Connection        connection = connectionProvider.getConnection();
        PreparedStatement statement  = null;
        ResultSet         resultSet  = null;

        try {
            statement = connection.prepareStatement(sql);
            binder.bind(statement);

            resultSet = callback.doWithStatement(statement);
            return extractor.extract(resultSet);

        } finally {
            JdbcSupport.closeQuietly(resultSet, statement);
            connectionProvider.release(connection);
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return executeUpdate(sql, stmt -> {});
    }

    @Override
    public int executeUpdate(String sql, PreparedStatementBinder binder) throws SQLException {
        Connection connection = connectionProvider.getConnection();
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement(sql);
            binder.bind(statement);
            return statement.executeUpdate();
        } finally {
            JdbcSupport.closeQuietly(statement);
            connectionProvider.release(connection);
        }
    }

    @Override
    public int[] executeBatch(String sql, List<? extends PreparedStatementBinder> binders) throws SQLException {
        Contract.nonNull(sql, "sql");
        Contract.nonNull(binders, "binders");

        if (binders.isEmpty()) {
            return new int[0];
        }

        Connection connection = connectionProvider.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            for (PreparedStatementBinder binder : binders) {
                if (binder != null) {
                    binder.bind(statement);
                }
                statement.addBatch();
            }

            return statement.executeBatch();
        } finally {
            connectionProvider.release(connection);
        }
    }

    @Override
    public <K> K executeUpdate(
            String sql,
            PreparedStatementBinder binder,
            KeyExtractor<K> extractor
    ) throws SQLException {
        Contract.nonNull(extractor, "extractor");
        Contract.nonNull(sql, "sql");
        Contract.nonNull(binder, "binder");

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
    public <T> T executeCall(String sql, CallableStatementBinder binder, CallableCallback<T> callback) throws SQLException {
        Connection connection = connectionProvider.getConnection();
        try (CallableStatement statement = connection.prepareCall(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            return callback.doInCallable(statement);
        } finally {
            connectionProvider.release(connection);
        }
    }

}
