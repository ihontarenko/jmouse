package org.jmouse.tx.support;

import org.jmouse.tx.TransactionException;
import org.jmouse.tx.TransactionSession;

abstract public class AbstractTransactionExecution implements TransactionSession {

    private boolean rollbackOnly;
    private boolean nested;
    private boolean isNew;
    private boolean completed;
    private Object  savepoint;
    private Object  transaction;
    private Object  suspended;

    public AbstractTransactionExecution() {
        markNew();
    }

    @Override
    public Object createSavepoint() {
        Object savepoint = getSavepointManager().createSavepoint();
        setSavepoint(savepoint);
        return savepoint;
    }

    @Override
    public void releaseSavepoint(Object savepoint) {
        getSavepointManager().releaseSavepoint(savepoint);
    }

    public void releaseSavepoint() {
        Object savepoint = getSavepoint();
        if (savepoint == null) {
            throw new TransactionException(
                    "RELEASE SAVEPOINT FAILURE. NO ATTACHED SAVEPOINT");
        }
        releaseSavepoint(savepoint);
        setSavepoint(null);
    }

    @Override
    public void rollbackSavepoint(Object savepoint) {
        getSavepointManager().rollbackSavepoint(savepoint);
    }

    public void rollbackSavepoint() {
        Object savepoint = getSavepoint();
        if (savepoint == null) {
            throw new TransactionException(
                    "ROLLBACK SAVEPOINT FAILURE. NO ATTACHED SAVEPOINT");
        }
        rollbackSavepoint(savepoint);
        releaseSavepoint(savepoint);
    }

    @Override
    public void setSavepoint(Object savepoint) {
        this.savepoint = savepoint;
    }

    @Override
    public Object getSavepoint() {
        return savepoint;
    }

    @Override
    public Object getTransaction() {
        return transaction;
    }

    @Override
    public Object getSuspended() {
        return suspended;
    }

    @Override
    public boolean isNested() {
        return nested;
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public void markCompleted() {
        completed = true;
    }

    @Override
    public void markNew() {
        isNew = true;
    }

    @Override
    public void markRollbackOnly() {
        rollbackOnly = true;
    }

}
