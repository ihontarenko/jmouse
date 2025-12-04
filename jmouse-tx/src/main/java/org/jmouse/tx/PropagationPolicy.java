package org.jmouse.tx;

/**
 * Transaction propagation behavior. 🧭
 */
public enum PropagationPolicy {

    /** Join current transaction or create a new one if none exists. */
    REQUIRED,

    /** Always create a new independent transaction, suspending the current one. */
    REQUIRES_NEW,

    /** Must run inside an existing transaction. */
    MANDATORY,

    /** Must NOT run inside a transaction. */
    NEVER,

    /** Join if exists; otherwise run non-transactional. */
    SUPPORTS,

    /** Nested transaction (savepoint-based). */
    NESTED,
}
