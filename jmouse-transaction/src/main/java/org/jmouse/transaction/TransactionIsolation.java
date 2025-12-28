package org.jmouse.transaction;

/**
 * Defines isolation level semantics for transactional execution.
 */
public enum TransactionIsolation {

    DEFAULT,

    READ_UNCOMMITTED,

    READ_COMMITTED,

    REPEATABLE_READ,

    SERIALIZABLE

}