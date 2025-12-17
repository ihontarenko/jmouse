package org.jmouse.tx.core;

/**
 * Defines how transactional boundaries propagate.
 */
public enum TransactionPropagation {

    REQUIRED,

    REQUIRES_NEW,

    SUPPORTS,

    MANDATORY,

    NEVER,

    NESTED

}
