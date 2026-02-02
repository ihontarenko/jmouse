package org.jmouse.jdbc.transaction;

import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.exception.JdbcAccessException;
import org.jmouse.jdbc.transaction.ConnectionCustomizer.RestoreAction;
import org.jmouse.transaction.SavepointSupport;
import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC-backed {@link TransactionSession} implementation.
 * <p>
 * {@code JdbcTransactionSession} is responsible for creating and managing a single
 * transactional {@link Connection} obtained from a {@link ConnectionProvider}.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Acquire a {@link Connection} on {@link #begin()}</li>
 *     <li>Optionally apply {@link TransactionDefinition} settings via {@link ConnectionCustomizer}</li>
 *     <li>Disable auto-commit and manage {@link #commit()} / {@link #rollback()}</li>
 *     <li>Bind/unbind the connection resource into the TX context</li>
 *     <li>Restore connection state on {@link #close()}</li>
 *     <li>Provide savepoint operations via {@link SavepointSupport} (for {@code NESTED}-like semantics)</li>
 * </ul>
 *
 * <h3>Context binding</h3>
 * <pre>{@code
 * begin():
 *   Connection c = valueProvider.getConnection();
 *   bindResource(JdbcResourceHolder.class, new JdbcResourceHolder(c));
 *
 * close():
 *   unbindResource(JdbcResourceHolder.class);
 *   valueProvider.release(c);
 * }</pre>
 *
 * <p>
 * ⚠️ This session is not thread-safe. It is intended to be used within a single
 * transaction boundary and a single thread.
 *
 * @author jMouse
 */
public final class JdbcTransactionSession implements TransactionSession, SavepointSupport {

    private static final Logger                LOGGER = LoggerFactory.getLogger(JdbcTransactionSession.class);

    /**
     * Provider for obtaining and releasing JDBC connections.
     */
    private final ConnectionProvider    provider;

    /**
     * Applies transactional settings (isolation, readOnly, etc.) and provides a restore action.
     */
    private final ConnectionCustomizer  connectionCustomizer;

    /**
     * Action used to restore connection state after the transaction completes.
     */
    private       RestoreAction         restoreAction = RestoreAction.noop();

    /**
     * The active transactional connection (non-null while active).
     */
    private       Connection            connection;

    /**
     * Definition captured by {@link #configure(TransactionDefinition)} and applied on {@link #begin()}.
     */
    private       TransactionDefinition pendingDefinition;

    /**
     * Creates a new {@code JdbcTransactionSession}.
     *
     * @param provider             connection valueProvider
     * @param connectionCustomizer customizer responsible for applying/restoring connection settings
     */
    public JdbcTransactionSession(ConnectionProvider provider, ConnectionCustomizer connectionCustomizer) {
        this.provider = provider;
        this.connectionCustomizer = connectionCustomizer;
    }

    /**
     * Captures the {@link TransactionDefinition} to be applied when the session begins.
     * <p>
     * This method typically runs before {@link #begin()} (e.g., by a coordinator).
     *
     * @param definition transaction definition to apply
     */
    @Override
    public void configure(TransactionDefinition definition) {
        this.pendingDefinition = definition;
    }

    // Apply isolation/readOnly before disabling autocommit is acceptable.
    // We also capture restore action.
    /**
     * Begins the transaction by acquiring a connection and disabling auto-commit.
     * <p>
     * If a {@link TransactionDefinition} was provided via {@link #configure(TransactionDefinition)},
     * it is applied before setting auto-commit to {@code false}.
     *
     * <p>
     * The acquired connection is bound into the transaction context as a {@link JdbcResourceHolder}.
     *
     * @throws JdbcAccessException if JDBC operations fail
     */
    @Override
    public void begin() {
        try {
            connection = provider.getConnection();
            TransactionDefinition definition = pendingDefinition;
            if (definition != null) {
                restoreAction = connectionCustomizer.apply(connection, definition);
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

    /**
     * Commits the current transaction.
     *
     * @throws JdbcAccessException if commit fails
     */
    @Override
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    /**
     * Rolls back the current transaction.
     *
     * @throws JdbcAccessException if rollback fails
     */
    @Override
    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    /**
     * Indicates whether this session currently holds an active connection.
     *
     * @return {@code true} if a transaction has been started and not yet closed
     */
    @Override
    public boolean isActive() {
        return connection != null;
    }

    // restore connection settings (readOnly/isolation/autocommit)
    /**
     * Closes this session and releases associated resources.
     * <p>
     * Order of operations:
     * <ol>
     *     <li>Attempt to restore connection state via {@link RestoreAction}</li>
     *     <li>As a safety measure, force {@code autoCommit=true} if possible</li>
     *     <li>Unbind {@link JdbcResourceHolder} from the TX context</li>
     *     <li>Release the connection via {@link ConnectionProvider}</li>
     *     <li>Reset internal state</li>
     * </ol>
     *
     * <p>
     * Restoration failures are logged and do not prevent releasing the connection.
     */
    @Override
    public void close() {
        try {
            try {
                restoreAction.restore();
            } catch (SQLException restoreEx) {
                LOGGER.warn("Failed to restore Connection state.", restoreEx);
            }

            if (connection != null) {
                // ensure autocommit true as final safety (some drivers ignore restore ordering)
                try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } finally {
            TransactionContextAccessSupport.unbindResource(JdbcResourceHolder.class);
            provider.release(connection);
            connection = null;
            pendingDefinition = null;
            restoreAction = RestoreAction.noop();
        }
    }

    /**
     * Creates a JDBC savepoint for nested-transaction semantics.
     *
     * @return driver-specific savepoint object (typically {@link java.sql.Savepoint})
     * @throws JdbcAccessException if savepoint creation fails
     */
    @Override
    public Object createSavepoint() {
        try {
            return connection.setSavepoint();
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    /**
     * Rolls back the transaction to the given savepoint.
     *
     * @param savepoint savepoint returned by {@link #createSavepoint()}
     * @throws JdbcAccessException if rollback fails
     */
    @Override
    public void rollbackToSavepoint(Object savepoint) {
        try {
            connection.rollback((java.sql.Savepoint) savepoint);
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

    /**
     * Releases the given savepoint.
     *
     * @param savepoint savepoint returned by {@link #createSavepoint()}
     * @throws JdbcAccessException if release fails
     */
    @Override
    public void releaseSavepoint(Object savepoint) {
        try {
            connection.releaseSavepoint((java.sql.Savepoint) savepoint);
        } catch (SQLException e) {
            throw new JdbcAccessException(e);
        }
    }

}
