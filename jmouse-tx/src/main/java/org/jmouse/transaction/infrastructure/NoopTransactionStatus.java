package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.TransactionStatus;

public final class NoopTransactionStatus implements TransactionStatus {

    public static final NoopTransactionStatus INSTANCE = new NoopTransactionStatus();

    private NoopTransactionStatus() {
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public boolean isRollbackOnly() {
        return false;
    }

    @Override
    public void markRollbackOnly() {

    }

    @Override
    public boolean isCompleted() {
        return false;
    }

    @Override
    public void markCompleted() {

    }
}
