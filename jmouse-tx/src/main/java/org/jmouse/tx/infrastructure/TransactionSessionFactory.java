package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.TransactionDefinition;
import org.jmouse.tx.core.TransactionSession;

/**
 * Factory responsible for creating transactional sessions.
 */
public interface TransactionSessionFactory {

    TransactionSession openSession(TransactionDefinition definition);

}