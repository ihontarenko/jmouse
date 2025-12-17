package org.jmouse.tx.core;

/**
 * Represents a low-level transactional session bound to a resource.
 */
public interface TransactionSession extends AutoCloseable {

    void begin();

    void commit();

    void rollback();

    boolean isActive();

    @Override
    void close();
    
}