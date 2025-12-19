package org.jmouse.tx.infrastructure;

/**
 * Strategy for binding transaction context to an execution scope.
 */
public interface TransactionContextHolder {

    TransactionContext getContext();

    void bindContext(TransactionContext context);

    TransactionContext unbindContext();

    <T> void bindResource(Class<T> key, T resource);

    <T> T getResource(Class<T> key);

    <T> T unbindResource(Class<T> key);

    boolean hasResource(Class<?> key);

    void clear();

}
