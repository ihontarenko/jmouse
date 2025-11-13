package org.jmouse.jdbc.tx;

import org.jmouse.tx.AbstractTransactionManager;
import org.jmouse.tx.TransactionDefinition;
import org.jmouse.tx.TransactionStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;

/**
 * 💾 Plain JDBC transaction manager (single DataSource) with savepoint support.
 *
 * <p>One JDBC Connection per thread. No ConnectionHolder abstraction (yet),
 * але семантика дуже близька до Spring's DataSourceTransactionManager.</p>
 */
public class JdbcTransactionManager extends AbstractTransactionManager {

    private final DataSource dataSource;

    /**
     * One tx-object per thread.
     */
    private final ThreadLocal<JdbcTransactionObject> current = new ThreadLocal<>();

    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected Object doGetTransaction() {
        JdbcTransactionObject existing = current.get();
        return (existing != null ? existing : new JdbcTransactionObject());
    }

    @Override
    protected boolean isExisting(Object txObject) {
        JdbcTransactionObject jdbc = (JdbcTransactionObject) txObject;
        return jdbc.connection != null;
    }

    @Override
    protected void doBegin(Object txObject, TransactionDefinition definition) {
        JdbcTransactionObject jdbc = (JdbcTransactionObject) txObject;
        try {
            Connection con = dataSource.getConnection();
            con.setAutoCommit(false);

            // isolation?
            if (definition.getIsolation() != TransactionDefinition.ISOLATION_DEFAULT) {
                con.setTransactionIsolation(definition.getIsolation());
            }

            // read-only?
            if (definition.isReadOnly()) {
                con.setReadOnly(true);
            }

            jdbc.connection = con;
            current.set(jdbc);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot open JDBC transaction", e);
        }
    }

    @Override
    protected void doCommit(TransactionStatus status) {
        JdbcTransactionObject jdbc = obtainCurrent();
        if (jdbc.connection == null) {
            return;
        }
        try {
            jdbc.connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("JDBC commit failed", e);
        } finally {
            closeAndClear(jdbc);
        }
    }

    @Override
    protected void doRollback(TransactionStatus status) {
        JdbcTransactionObject jdbc = obtainCurrent();
        if (jdbc.connection == null) {
            return;
        }
        try {
            jdbc.connection.rollback();
        } catch (SQLException e) {
            throw new IllegalStateException("JDBC rollback failed", e);
        } finally {
            closeAndClear(jdbc);
        }
    }

    @Override
    protected Object doSuspend(Object currentTx) {
        // For JDBC it's basically “detach from thread”
        JdbcTransactionObject jdbc = (JdbcTransactionObject) currentTx;
        current.remove();
        return jdbc;
    }

    @Override
    protected void doResume(Object currentTx, Object suspended) {
        current.set((JdbcTransactionObject) suspended);
    }

    // ⭐ savepoints
    @Override
    protected Object createSavepoint(Object resource) {
        JdbcTransactionObject jdbc = (JdbcTransactionObject) resource;
        if (jdbc.connection == null) {
            throw new IllegalStateException("No JDBC connection for savepoint");
        }
        try {
            return jdbc.connection.setSavepoint();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create JDBC savepoint", e);
        }
    }

    @Override
    protected void rollbackSavepoint(Object resource, Object savepoint) {
        JdbcTransactionObject jdbc = (JdbcTransactionObject) resource;
        try {
            jdbc.connection.rollback((Savepoint) savepoint);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot rollback to JDBC savepoint", e);
        }
    }

    @Override
    protected void releaseSavepoint(Object resource, Object savepoint) {
        JdbcTransactionObject jdbc = (JdbcTransactionObject) resource;
        try {
            jdbc.connection.releaseSavepoint((Savepoint) savepoint);
        } catch (SQLException e) {
            // часто можна ігнорувати
        }
    }

    private JdbcTransactionObject obtainCurrent() {
        JdbcTransactionObject jdbc = current.get();
        if (jdbc == null) {
            jdbc = new JdbcTransactionObject();
            current.set(jdbc);
        }
        return jdbc;
    }

    private void closeAndClear(JdbcTransactionObject jdbc) {
        try {
            jdbc.connection.setAutoCommit(true);
            jdbc.connection.close();
        } catch (SQLException ignored) {
        } finally {
            jdbc.connection = null;
            current.remove();
        }
    }

    /**
     * 📦 Our minimal tx object (similar to Spring's DataSourceTransactionObject).
     */
    private static class JdbcTransactionObject {
        Connection connection;
    }
}
