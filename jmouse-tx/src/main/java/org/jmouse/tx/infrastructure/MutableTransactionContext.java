package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.TransactionDefinition;
import org.jmouse.tx.core.TransactionSession;
import org.jmouse.tx.core.TransactionStatus;

import java.util.Optional;

public class MutableTransactionContext implements TransactionContext {

    private final TransactionDefinition definition;
    private final TransactionStatus     status;
    private final TransactionSession    session;

    private Object savepoint;

    public MutableTransactionContext(
            TransactionDefinition definition,
            TransactionStatus status,
            TransactionSession session
    ) {
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

}
