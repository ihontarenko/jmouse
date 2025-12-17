package org.jmouse.tx.core;

/**
 * Central transaction manager abstraction.
 */
public interface TransactionManager {

    TransactionStatus begin(TransactionDefinition definition);

    void commit(TransactionStatus status);

    void rollback(TransactionStatus status);

}