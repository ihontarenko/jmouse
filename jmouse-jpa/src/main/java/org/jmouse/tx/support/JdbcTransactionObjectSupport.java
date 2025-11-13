package org.jmouse.tx.support;

import org.jmouse.jdbc.ConnectionContext;
import org.jmouse.tx.SavepointManager;

public class JdbcTransactionObjectSupport implements SavepointManager {

    private final ConnectionContext connectionContext;

    public JdbcTransactionObjectSupport(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object createSavepoint() {
        return null;
    }

    @Override
    public void releaseSavepoint(Object savepoint) {

    }

    @Override
    public void rollbackSavepoint(Object savepoint) {

    }

}
