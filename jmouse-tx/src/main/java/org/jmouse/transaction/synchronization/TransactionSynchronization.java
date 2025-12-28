package org.jmouse.transaction.synchronization;

/**
 * Callback interface for transaction lifecycle events.
 */
public interface TransactionSynchronization {

    default void beforeBegin() {
    }

    default void beforeCommit() {
    }

    default void afterCommit() {
    }

    default void afterRollback() {
    }

    default void afterCompletion(CompletionStatus status) {
    }

    enum CompletionStatus {
        COMMITTED,
        ROLLED_BACK
    }

}

