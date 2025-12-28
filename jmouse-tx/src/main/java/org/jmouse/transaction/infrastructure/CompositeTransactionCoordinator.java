package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionManager;
import org.jmouse.transaction.TransactionStatus;

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
