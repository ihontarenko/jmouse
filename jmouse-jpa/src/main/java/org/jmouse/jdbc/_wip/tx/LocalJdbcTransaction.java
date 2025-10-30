package org.jmouse.jdbc._wip.tx;

import org.jmouse.jdbc.ConnectionBinding;
import org.jmouse.jdbc.errors.JdbcException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * 🔁 Local (resource) transaction over a single {@link Connection}.
 * Respects {@link ConnectionBinding} if already bound.
 */
public final class LocalJdbcTransaction implements JdbcTransaction {
    private final ConnectionBinding    binding;
    private final Supplier<Connection> supplier;

    private Connection owned; // only if we created it
    private boolean active;

    public LocalJdbcTransaction(ConnectionBinding binding, java.util.function.Supplier<Connection> supplier) {
        this.binding = binding;
        this.supplier = supplier;
    }

    @Override public void begin() {
        if (active) return;
        try {
            Connection connection = binding.currentConnection();
            if (connection == null) {
                connection = supplier.get();
                ThreadLocalConnectionBinding.autoCommitDisable(connection);
                binding.bind(connection);
                owned = connection;
            } else {
                ThreadLocalConnectionBinding.autoCommitDisable(connection);
            }
            active = true;
        } catch (SQLException e) {
            throw new JdbcException("Begin tx failed", e);
        }
    }

    @Override public void commit() {
        if (!active) return;
        try (Connection connection = binding.currentConnection();) {
            if (connection != null) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new JdbcException("Commit failed", e);
        } finally {
            cleanup();
        }
    }

    @Override public void rollback() {
        if (!active) return;
        try (Connection connection = binding.currentConnection()) {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
            throw new JdbcException("Rollback failed", e);
        } finally {
            cleanup();
        }
    }

    @Override public boolean isActive() { return active; }

    private void cleanup() {
        active = false;
        try {
            if (owned != null) {
                try { owned.setAutoCommit(true); } catch (SQLException ignore) {}
                try { owned.close(); } catch (SQLException ignore) {}
            }
        } finally {
            binding.unbind();
            owned = null;
        }
    }
}
