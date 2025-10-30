package org.jmouse.jdbc.tx;

import org.jmouse.tx.*;
import org.jmouse.tx.errors.TransactionException;

import javax.sql.DataSource;
import java.sql.Connection;

public class JdbcTransactionManager extends TransactionManager.Support {

    private final DataSource dataSource;
    private final Object     resourceKey;

    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
        this.resourceKey = dataSource; // можна завернути в ключ
    }

    @Override
    public TransactionStatus begin(TransactionDefinition definition) {
        try {
            Connection connection    = dataSource.getConnection();
            boolean    auto = connection.getAutoCommit();
            connection.setAutoCommit(false);

            JtaResourceContext.bind(resourceKey, connection);

            var status = new TransactionStatus.Simple(true, connection);

            TransactionSynchronizations.beforeCommit(definition.isReadOnly());

            return status;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot start JDBC transaction", e);
        }
    }

    @Override
    protected void doCommit(TransactionStatus status) {
        if (status.getResource() instanceof Connection connection) {
            try (connection; connection) {
                connection.commit();
            } catch (Exception e) {
                throw new TransactionException("Commit failed", e);
            } finally {
                JtaResourceContext.unbind(resourceKey);
            }
        }
    }

    @Override
    protected void doRollback(TransactionStatus status) {
        if (status.getResource() instanceof Connection connection) {
            try (connection) {
                connection.rollback();
            } catch (Exception e) {
                throw new TransactionException("Rollback failed", e);
            } finally {
                JtaResourceContext.unbind(resourceKey);
            }
        }
    }

}
