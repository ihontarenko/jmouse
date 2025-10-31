package org.jmouse.jdbc.tx;

import org.jmouse.jdbc.ConnectionContext;
import org.jmouse.tx.support.AbstractTransactionManager;
import org.jmouse.tx.TransactionDefinition;
import org.jmouse.tx.TransactionSession;
import org.jmouse.tx.support.JdbcTransactionObjectSupport;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


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
    protected void doCommit(TransactionSession status) {
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
    protected void doRollback(TransactionSession status) {
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

    private class JdbcTransactionObject extends JdbcTransactionObjectSupport {

        public JdbcTransactionObject(ConnectionContext connectionContext) {
            super(connectionContext);
        }

    }
}
