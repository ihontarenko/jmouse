package org.jmouse.transaction;

/**
 * Describes transactional requirements.
 */
public interface TransactionDefinition {

    TransactionPropagation getPropagation();

    TransactionIsolation getIsolation();

    int getTimeoutSeconds();

    boolean isReadOnly();

    String getName();

}
