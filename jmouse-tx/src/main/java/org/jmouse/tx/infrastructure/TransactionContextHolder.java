package org.jmouse.tx.infrastructure;

/**
 * Strategy for binding transaction context to an execution scope.
 */
public interface TransactionContextHolder {

    TransactionContext getCurrent();

    void bind(TransactionContext context);

    void clear();

}
