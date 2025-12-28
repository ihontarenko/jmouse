package org.jmouse.transaction.infrastructure;

/**
 * Describes transaction participation semantics.
 */
public enum TransactionParticipation {

    NEW,

    EXISTING,

    SUSPENDED,

    NONE

}
