package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.TransactionDefinition;
import org.jmouse.tx.core.TransactionSession;
import org.jmouse.tx.core.TransactionStatus;

import java.util.Optional;

/**
 * Holds transactional state for the current execution scope.
 */
public interface TransactionContext {

    TransactionDefinition getDefinition();

    TransactionStatus getStatus();

    TransactionSession getSession();

    Optional<Object> getSavepoint();

}
