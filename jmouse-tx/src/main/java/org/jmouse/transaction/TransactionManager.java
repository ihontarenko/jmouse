package org.jmouse.transaction;

/**
 * Central transaction manager abstraction.
 */
public interface TransactionManager {

    TransactionStatus begin(TransactionDefinition definition);

    void commit(TransactionStatus status);

    void rollback(TransactionStatus status);

}