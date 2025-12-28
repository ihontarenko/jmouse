package org.jmouse.jdbc.transaction;

import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.exception.JdbcAccessException;
import org.jmouse.transaction.SavepointSupport;
import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionIsolation;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public final class JdbcTransactionSession implements TransactionSession, SavepointSupport {

    private static final Logger                LOGGER = LoggerFactory.getLogger(JdbcTransactionSession.class);
    private final        ConnectionProvider    provider;
    private              TransactionDefinition definition;
    private              ConnectionState       previousState;
    private              Connection            connection;

    public JdbcTransactionSession(ConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void configure(TransactionDefinition definition) {
        this.definition = definition;
    }

    @Override
    public void begin() {
        try {
            connection = provider.getConnection();
            previousState = ConnectionState.capture(connection);

            if (definition != null) {
                applyReadOnly(definition);
                applyIsolation(definition);
            }

            connection.setAutoCommit(false);

        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }

        TransactionContextAccessSupport.bindResource(
                JdbcResourceHolder.class,
                new JdbcResourceHolder(connection)
        );
    }

    @Override
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    @Override
    public boolean isActive() {
        return connection != null;
    }

    @Override
    public void close() {
        Connection connection = this.connection;
        if (connection != null) {
            try {
                if (previousState != null) {
                    previousState.restore(connection);
                } else {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                LOGGER.error("Error restoring connection state!", e);
            } finally {
                try {
                    TransactionContextAccessSupport.unbindResource(JdbcResourceHolder.class);
                } finally {
                    provider.release(connection);
                    this.connection = null;
                    this.definition = null;
                    this.previousState = null;
                }
            }
        }
    }

    @Override
    public Object createSavepoint() {
        try {
            return connection.setSavepoint();
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    @Override
    public void rollbackToSavepoint(Object savepoint) {
        try {
            connection.rollback((java.sql.Savepoint) savepoint);
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    @Override
    public void releaseSavepoint(Object savepoint) {
        try {
            connection.releaseSavepoint((java.sql.Savepoint) savepoint);
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    private void applyReadOnly(TransactionDefinition definition) throws SQLException {
        boolean target = definition.isReadOnly();
        if (previousState.readOnly() != target) {
            connection.setReadOnly(target);
        }
    }

    private void applyIsolation(TransactionDefinition definition) throws SQLException {
        TransactionIsolation value = definition.getIsolation();
        if (value != null && value != TransactionIsolation.DEFAULT) {
            int isolation = IsolationMapping.toJDBCIsolation(value);
            if (previousState.isolation() != isolation) {
                connection.setTransactionIsolation(isolation);
            }
        }
    }

}


