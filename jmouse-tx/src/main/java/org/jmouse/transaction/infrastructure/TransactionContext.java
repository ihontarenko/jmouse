package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.TransactionStatus;

import java.util.Optional;

/**
 * Holds transactional state for the current execution scope.
 */
public interface TransactionContext {

    TransactionDefinition getDefinition();

    TransactionStatus getStatus();

    TransactionSession getSession();

    Optional<Object> getSavepoint();

    boolean isRollbackOnly();

    void setRollbackOnly();
}
