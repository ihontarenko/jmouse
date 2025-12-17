package org.jmouse.jdbc.tx;

import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.tx.core.TransactionSession;
import org.jmouse.tx.infrastructure.support.TransactionContextAccessSupport;

import java.sql.Connection;

public final class JdbcTransactionSession
        implements TransactionSession {

    private final ConnectionProvider provider;
    private Connection connection;

    public JdbcTransactionSession(ConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void begin() {
        connection = provider.getConnection();
        connection.setAutoCommit(false);

        TransactionContextAccessSupport.bindResource(
                JdbcResourceHolder.class,
                new JdbcResourceHolder(connection)
        );
    }

    @Override
    public void commit() {
        connection.commit();
    }

    @Override
    public void rollback() {
        connection.rollback();
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


