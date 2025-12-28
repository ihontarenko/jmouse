package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.TransactionStatus;

import java.util.Optional;

public final class MutableTransactionContext implements TransactionContext {

    private final TransactionDefinition definition;
    private final TransactionStatus     status;
    private final TransactionSession    session;
    private       Object                savepoint;

    public MutableTransactionContext(TransactionDefinition definition, TransactionStatus status, TransactionSession session) {
        this.definition = definition;
        this.status = status;
        this.session = session;
    }

    @Override
    public TransactionDefinition getDefinition() {
        return definition;
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
        status.markRollbackOnly();
    }
}

