package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;

/**
 * Factory responsible for creating transactional sessions.
 */
public interface TransactionSessionFactory {

    TransactionSession openSession(TransactionDefinition definition);

}