package org.jmouse.jdbc.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import org.jmouse.jdbc.transaction.JdbcResourceHolder;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;

/** Reuses only the transaction connection belonging to this provider's resource. */
public final class TransactionAwareConnectionProvider implements ConnectionProvider {

    private final ConnectionProvider delegate;
    private final TransactionContextHolder contexts;

    public TransactionAwareConnectionProvider(ConnectionProvider delegate) {
        this(delegate, TransactionContextAccessSupport.current());
    }

    public TransactionAwareConnectionProvider(
        ConnectionProvider delegate,
        TransactionContextHolder contexts
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.contexts = Objects.requireNonNull(contexts, "contexts");
    }

    @Override
    public Object getResourceKey() {
        return delegate.getResourceKey();
    }

    @Override
    public Connection getConnection() throws SQLException {
        JdbcResourceHolder holder = contexts.getResource(JdbcResourceHolder.class);
        if (holder != null && holder.belongsTo(getResourceKey())) {
            if (holder.isCompleted()) {
                throw new IllegalStateException(
                    "Transaction resource is complete; use REQUIRES_NEW for completion callback SQL"
                );
            }
            return holder.getConnection();
        }
        return delegate.getConnection();
    }

    @Override
    public void release(Connection connection) {
        JdbcResourceHolder holder = contexts.getResource(JdbcResourceHolder.class);
        if (holder == null || holder.getConnection() != connection) {
            delegate.release(connection);
        }
    }
}
