package org.jmouse.tx.infrastructure;

/**
 * Describes transaction participation semantics.
 */
public enum TransactionParticipation {

    NEW,

    EXISTING,

    SUSPENDED,

    NONE

}
