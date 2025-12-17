package org.jmouse.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jmouse.jdbc.JdbcSupport;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
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

        Connection connection = connectionProvider.getConnection();
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connection.prepareStatement(sql);
            binder.bind(statement);

            resultSet = callback.doWithStatement(statement);
            return extractor.extract(resultSet);

        } finally {
            JdbcSupport.closeQuietly(resultSet);
            JdbcSupport.closeQuietly(statement);
            connectionProvider.release(connection);
        }
    }


}
