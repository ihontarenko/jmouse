package org.jmouse.transaction;

/**
 * Represents the runtime status of an active transaction.
 */
public interface TransactionStatus {

    boolean isNew();

    boolean isRollbackOnly();

    void markRollbackOnly();

    boolean isCompleted();

    void markCompleted();

}
