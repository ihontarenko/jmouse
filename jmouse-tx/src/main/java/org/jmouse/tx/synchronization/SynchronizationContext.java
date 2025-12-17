package org.jmouse.tx.synchronization;

/**
 * Holds synchronizations bound to a transaction scope.
 */
public interface SynchronizationContext {

    SynchronizationRegistry getRegistry();

}