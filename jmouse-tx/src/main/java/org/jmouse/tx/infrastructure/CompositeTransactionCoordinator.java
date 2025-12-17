package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.TransactionDefinition;
import org.jmouse.tx.core.TransactionManager;
import org.jmouse.tx.core.TransactionStatus;

import java.util.List;

public class CompositeTransactionCoordinator
        implements TransactionManager {

    private final List<TransactionManager> delegates;

    public CompositeTransactionCoordinator(
            List<TransactionManager> delegates
    ) {
        this.delegates = delegates;
    }

    @Override
    public TransactionStatus begin(TransactionDefinition definition) {
        TransactionStatus lastStatus = null;
        for (TransactionManager manager : delegates) {
            lastStatus = manager.begin(definition);
        }
        return lastStatus;
    }

    @Override
    public void commit(TransactionStatus status) {
        for (TransactionManager manager : delegates) {
            manager.commit(status);
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        for (TransactionManager manager : delegates) {
            manager.rollback(status);
        }
    }
}
