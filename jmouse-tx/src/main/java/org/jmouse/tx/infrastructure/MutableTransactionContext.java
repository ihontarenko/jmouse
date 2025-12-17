package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.TransactionSession;
import org.jmouse.tx.core.TransactionStatus;

import java.util.Optional;

public final class MutableTransactionContext implements TransactionContext {

    private final TransactionStatus  status;
    private final TransactionSession session;
    private       Object             savepoint;

    public MutableTransactionContext(TransactionStatus status, TransactionSession session) {
        this.status = status;
        this.session = session;
    }

    @Override
    public TransactionStatus getStatus() {
        return status;
    }

    @Override
    public TransactionSession getSession() {
        return session;
    }

    @Override
    public Optional<Object> getSavepoint() {
        return Optional.ofNullable(savepoint);
    }

    public void setSavepoint(Object savepoint) {
        this.savepoint = savepoint;
    }

    public void clearSavepoint() {
        this.savepoint = null;
    }

    @Override
    public boolean isRollbackOnly() {
        return status.isRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        status.setRollbackOnly();
    }
}

