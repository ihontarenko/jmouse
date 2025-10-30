package org.jmouse.tx;

public interface TransactionSynchronization {

    default void beforeCommit(boolean readOnly) {}

    default void beforeCompletion() {}

    default void afterCommit() {}

    /**
     * @param status one of STATUS_COMMITTED / STATUS_ROLLED_BACK / STATUS_UNKNOWN
     */
    default void afterCompletion(int status) {}

}
