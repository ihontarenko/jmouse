package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.TransactionStatus;

public class TransactionStatusSupport implements TransactionStatus {

    private final boolean isNew;
    private       boolean rollbackOnly;
    private       boolean completed;

    public TransactionStatusSupport(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }
}
