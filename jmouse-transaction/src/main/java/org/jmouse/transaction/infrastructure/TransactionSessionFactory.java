package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;

/**
 * Factory responsible for creating transactional sessions.
 */
public interface TransactionSessionFactory {
    TransactionSession openSession(TransactionDefinition definition);

    /**
     * Whether this manager may join a session already bound by another manager.
     * Resource-specific factories override this to prevent accidental cross-resource participation.
     */
    default boolean supportsExistingSession(TransactionSession session) {
        return true;
    }
}
