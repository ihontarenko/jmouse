package org.jmouse.tx;

public interface TransactionSession extends SavepointManager {

    boolean isNew();

    boolean isCompleted();

    boolean isNested();

    boolean isRollbackOnly();

    Object getSuspended();

    Object getTransaction();

    void markRollbackOnly();

    void markCompleted();

    void markNew();

    Object getSavepoint();

    void setSavepoint(Object savepoint);

    default boolean hasSavepoint() {
        return getSavepoint() != null;
    }

    default SavepointManager getSavepointManager() {
        Object transaction = getTransaction();
        if (!(transaction instanceof SavepointManager savepointManager)) {
            throw new NestedUnsupportedTransactionException(
                    "TRANSACTION OBJECT '" + getTransaction() + "' DOES UNSUPPORTED NESTED TRANSACTIONS.");
        }
        return savepointManager;
    }

}
