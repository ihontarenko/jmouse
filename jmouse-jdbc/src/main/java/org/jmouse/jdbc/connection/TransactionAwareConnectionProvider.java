package org.jmouse.jdbc.connection;

import org.jmouse.jdbc.transaction.JdbcResourceHolder;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Reuses only the transaction connection belonging to this provider's resource. */
public final class TransactionAwareConnectionProvider implements ConnectionProvider {
    private final ConnectionProvider delegate;

    public TransactionAwareConnectionProvider(ConnectionProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public Object getResourceKey() {
        return delegate.getResourceKey();
    }

    @Override
    public Connection getConnection() throws SQLException {
        JdbcResourceHolder holder = TransactionContextAccessSupport.getResource(JdbcResourceHolder.class);
        if (holder != null && holder.belongsTo(getResourceKey())) {
            return holder.getConnection();
        }
        return delegate.getConnection();
    }

    @Override
    public void release(Connection connection) {
        JdbcResourceHolder holder = TransactionContextAccessSupport.getResource(JdbcResourceHolder.class);
        if (holder == null || holder.getConnection() != connection) {
            delegate.release(connection);
        }
    }
}
