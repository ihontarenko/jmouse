package org.jmouse.jdbc.transaction;

import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.core.exception.JdbcAccessException;
import org.jmouse.tx.core.TransactionSession;
import org.jmouse.tx.infrastructure.support.TransactionContextAccessSupport;

import java.sql.Connection;
import java.sql.SQLException;

public final class JdbcTransactionSession implements TransactionSession {

    private final ConnectionProvider provider;
    private Connection connection;

    public JdbcTransactionSession(ConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void begin() {
        try {
            connection = provider.getConnection();
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
        return false;
    }

    @Override
    public void close() {
        TransactionContextAccessSupport.unbindResource(JdbcResourceHolder.class);
        provider.release(connection);
    }
}


