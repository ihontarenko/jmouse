package org.jmouse.transaction.synchronization;

import java.util.List;

/**
 * Registry for transaction synchronizations.
 */
public interface SynchronizationRegistry {

    void register(TransactionSynchronization synchronization);

    List<TransactionSynchronization> getSynchronizations();

    void clear();

}
