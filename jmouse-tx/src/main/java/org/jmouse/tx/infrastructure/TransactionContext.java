package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.TransactionStatus;

/**
 * Holds transactional state for the current execution scope.
 */
public interface TransactionContext {

    TransactionStatus getStatus();

    boolean isRollbackOnly();

    void setRollbackOnly();

}
