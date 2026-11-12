package org.jmouse.jdbc.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.exception.JdbcAccessException;
import org.jmouse.jdbc.transaction.ConnectionCustomizer.RestoreAction;
import org.jmouse.transaction.SavepointSupport;
import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;

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
 * This session is not thread-safe. It is intended to be used within a single
 * transaction boundary and a single thread.
 *
 * @author jMouse
 */
public final class JdbcTransactionSession implements TransactionSession, SavepointSupport {

    /**
     * Provider for obtaining and releasing JDBC connections.
     */
    private final ConnectionProvider provider;
    private final TransactionContextHolder contexts;

    /**
     * Applies transactional settings (isolation, readOnly, etc.) and provides a restore action.
     */
    private final ConnectionCustomizer connectionCustomizer;

    /**
     * Action used to restore connection state after the transaction completes.
     */
    private RestoreAction restoreAction = RestoreAction.noop();

    /**
     * The active transactional connection (non-null while active).
     */
    private Connection connection;
    private ConnectionState originalState;
    private boolean started;
    private boolean completed;
    private boolean resourceBound;
    private JdbcResourceHolder boundResource;

    /**
     * Definition captured by {@link #configure(TransactionDefinition)} and applied on {@link #begin()}.
     */
    private TransactionDefinition pendingDefinition;

    /**
     * Creates a new {@code JdbcTransactionSession}.
     *
     * @param provider             connection valueProvider
     * @param connectionCustomizer customizer responsible for applying/restoring connection settings
     */
    public JdbcTransactionSession(ConnectionProvider provider, ConnectionCustomizer connectionCustomizer) {
        this(provider, connectionCustomizer, TransactionContextAccessSupport.current());
    }

    public JdbcTransactionSession(
        ConnectionProvider provider,
        ConnectionCustomizer connectionCustomizer,
        TransactionContextHolder contexts
    ) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.connectionCustomizer = Objects.requireNonNull(connectionCustomizer, "connectionCustomizer");
        this.contexts = Objects.requireNonNull(contexts, "contexts");
    }

    /**
     * Captures the {@link TransactionDefinition} to be applied when the session begins.
     * <p>
     * This method typically runs before {@link #begin()} (e.g., by a coordinator).
     *
     * @param definition transaction definition to apply
     */
    private void requireActiveTransaction() {
        if (!started || connection == null || completed) {
            throw new IllegalStateException("No active transaction in this session");
        }
    }

    boolean belongsTo(Object resourceKey) {
        return provider.getResourceKey() == resourceKey;
    }

    @Override
    public void configure(TransactionDefinition definition) {
        if (connection != null) {
            throw new IllegalStateException("Cannot configure an active transaction session");
        }
        this.pendingDefinition = Objects.requireNonNull(definition, "definition");
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
        if (connection != null) {
            throw new IllegalStateException("Transaction session is already active");
        }
        try {
            connection = provider.getConnection();
            originalState = ConnectionState.capture(connection);
            if (pendingDefinition != null) {
                restoreAction = connectionCustomizer.apply(connection, pendingDefinition);
            }
            connection.setAutoCommit(false);
            started = true;
            completed = false;
            boundResource = new JdbcResourceHolder(connection, provider.getResourceKey());
            contexts.bindResource(JdbcResourceHolder.class, boundResource);
            resourceBound = true;
        } catch (SQLException error) {
            JdbcAccessException failure = new JdbcAccessException(error);
            cleanupFailedBegin(failure);
            throw failure;
        } catch (RuntimeException | Error failure) {
            cleanupFailedBegin(failure);
            throw failure;
        }
    }

    private void cleanupFailedBegin(Throwable failure) {
        try {
            close();
        } catch (RuntimeException | Error cleanupFailure) {
            if (failure != cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }

    /**
     * Commits the current transaction.
     *
     * @throws JdbcAccessException if commit fails
     */
    @Override
    public void commit() {
        requireActiveTransaction();
        try {
            connection.commit();
            completed = true;
            boundResource.markCompleted();
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
        requireActiveTransaction();
        try {
            connection.rollback();
            completed = true;
            boundResource.markCompleted();
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
     *     <li>Roll back unfinished work and restore the original connection state</li>
     *     <li>Unbind {@link JdbcResourceHolder} from the TX context</li>
     *     <li>Release the connection via {@link ConnectionProvider}</li>
     *     <li>Reset internal state</li>
     * </ol>
     *
     * <p>
     * Restoration failures are reported after the connection has been released.
     */
    @Override
    public void close() {
        if (connection == null) {
            return;
        }
        Throwable failure = null;
        boolean safeToRestore = true;
        if (started && !completed) {
            try {
                connection.rollback();
                completed = true;
            } catch (SQLException | RuntimeException | Error exception) {
                failure = translate(exception);
                safeToRestore = false;
            }
        }
        // Restoring auto-commit may commit pending work. Never do it after failed rollback.
        if (safeToRestore) {
            try {
                restoreAction.restore();
            } catch (SQLException | RuntimeException | Error exception) {
                failure = append(failure, translate(exception));
            }
            if (originalState != null) {
                try {
                    originalState.restore(connection);
                } catch (SQLException | RuntimeException | Error exception) {
                    failure = append(failure, translate(exception));
                }
            }
        }
        try {
            if (resourceBound) {
                contexts.unbindResource(JdbcResourceHolder.class);
            }
        } catch (RuntimeException | Error exception) {
            failure = append(failure, exception);
        }
        try {
            provider.release(connection);
        } catch (RuntimeException | Error exception) {
            failure = append(failure, exception);
        } finally {
            connection = null;
            originalState = null;
            pendingDefinition = null;
            restoreAction = RestoreAction.noop();
            started = false;
            completed = false;
            resourceBound = false;
            boundResource = null;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
    }

    private static Throwable translate(Throwable failure) {
        return failure instanceof SQLException sqlFailure ? new JdbcAccessException(sqlFailure) : failure;
    }

    private static Throwable append(Throwable first, Throwable next) {
        if (first == null) {
            return next;
        }
        if (first != next) {
            first.addSuppressed(next);
        }
        return first;
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
