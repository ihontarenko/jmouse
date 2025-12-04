package org.jmouse.tx;

public interface TransactionSynchronization {

    default void beforeCommit() {
    }

    default void afterCommit() {
    }

    default void beforeCompletion() {
    }

    default void afterCompletion() {
    }

}
