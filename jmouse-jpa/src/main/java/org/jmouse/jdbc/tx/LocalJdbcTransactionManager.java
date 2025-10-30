package org.jmouse.jdbc.tx;

import org.jmouse.tx.AbstractTransactionManager;
import org.jmouse.tx.JtaResourceContext;
import org.jmouse.tx.TransactionDefinition;
import org.jmouse.tx.TransactionStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Savepoint;
import java.util.Objects;

/**
 * 💾 Local (single-DataSource) transaction manager.
 * No XA, but supports propagation + nested (via savepoints).
 */
public final class LocalJdbcTransactionManager extends AbstractTransactionManager {

    private final DataSource dataSource;

    public LocalJdbcTransactionManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    protected Object doBegin(TransactionDefinition def) {
        try {
            Connection c = dataSource.getConnection();
            c.setAutoCommit(false);

            if (def.getIsolation() != TransactionDefinition.ISOLATION_DEFAULT) {
                c.setTransactionIsolation(toJdbcIsolation(def.getIsolation()));
            }

            JtaResourceContext.put(dataSource, c);
            return c;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot start transaction", e);
        }
    }

    @Override
    protected void doCommit(TransactionStatus status) {
        Connection c = (Connection) status.getResource();
        try {
            c.commit();
        } catch (Exception e) {
            throw new IllegalStateException("Commit failed", e);
        } finally {
            cleanup(c);
        }
    }

    @Override
    protected void doRollback(TransactionStatus status) {
        Connection c = (Connection) status.getResource();
        try {
            c.rollback();
        } catch (Exception e) {
            throw new IllegalStateException("Rollback failed", e);
        } finally {
            cleanup(c);
        }
    }

    @Override
    protected Object suspend(TransactionStatus current) {
        // just unbind connection from thread
        Connection c = (Connection) current.getResource();
        JtaResourceContext.unbind(dataSource);
        return c;
    }

    @Override
    protected void resume(Object suspended) {
        if (suspended instanceof Connection c) {
            JtaResourceContext.put(dataSource, c);
        }
    }

    @Override
    protected Object createSavepoint(Object resource) {
        try {
            return ((Connection) resource).setSavepoint();
        } catch (Exception e) {
            throw new IllegalStateException("Savepoint not supported", e);
        }
    }

    @Override
    protected void rollbackToSavepoint(Object resource, Object savepoint) {
        try {
            ((Connection) resource).rollback((Savepoint) savepoint);
        } catch (Exception e) {
            throw new IllegalStateException("Rollback to savepoint failed", e);
        }
    }

    private void cleanup(Connection c) {
        try { c.setAutoCommit(true); } catch (Exception ignore) {}
        JtaResourceContext.unbind(dataSource);
        try { c.close(); } catch (Exception ignore) {}
    }

    private int toJdbcIsolation(int iso) {
        return switch (iso) {
            case TransactionDefinition.ISOLATION_READ_UNCOMMITTED -> Connection.TRANSACTION_READ_UNCOMMITTED;
            case TransactionDefinition.ISOLATION_READ_COMMITTED -> Connection.TRANSACTION_READ_COMMITTED;
            case TransactionDefinition.ISOLATION_REPEATABLE_READ -> Connection.TRANSACTION_REPEATABLE_READ;
            case TransactionDefinition.ISOLATION_SERIALIZABLE -> Connection.TRANSACTION_SERIALIZABLE;
            default -> Connection.TRANSACTION_NONE;
        };
    }
}
