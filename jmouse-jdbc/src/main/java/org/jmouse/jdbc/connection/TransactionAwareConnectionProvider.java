package org.jmouse.jdbc.connection;

import org.jmouse.jdbc.transaction.JdbcResourceHolder;
import org.jmouse.tx.infrastructure.support.TransactionContextAccessSupport;

import java.sql.Connection;
import java.sql.SQLException;

public final class TransactionAwareConnectionProvider implements ConnectionProvider {

    private final ConnectionProvider delegate;

    public TransactionAwareConnectionProvider(ConnectionProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        JdbcResourceHolder holder =
                TransactionContextAccessSupport.getResource(JdbcResourceHolder.class);

        if (holder != null) {
            return holder.getConnection();
        }

        return delegate.getConnection();
    }

    @Override
    public void release(Connection connection) {
        if (!TransactionContextAccessSupport.hasResource(JdbcResourceHolder.class)) {
            delegate.release(connection);
        }
    }
}
