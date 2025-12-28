package org.jmouse.transaction;

/**
 * Represents a low-level transactional session bound to a resource.
 */
public interface TransactionSession extends AutoCloseable {

    default void configure(TransactionDefinition definition) { }

    void begin();

    void commit();

    void rollback();

    boolean isActive();

    @Override
    void close();

}