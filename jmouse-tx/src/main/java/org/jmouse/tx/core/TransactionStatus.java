package org.jmouse.tx.core;

/**
 * Represents the runtime status of an active transaction.
 */
public interface TransactionStatus {

    boolean isNew();

    boolean isRollbackOnly();

    void setRollbackOnly();

    boolean isCompleted();

    void markCompleted();

}
