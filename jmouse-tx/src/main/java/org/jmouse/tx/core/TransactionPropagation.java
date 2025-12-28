package org.jmouse.tx.core;

/**
 * Defines how transactional boundaries propagate when a transactional
 * operation is invoked.
 *
 * <p>
 * Propagation rules determine whether an existing transaction is reused,
 * suspended, or rejected when entering a transactional scope.
 *
 * <h3>Propagation semantics</h3>
 * <ul>
 *     <li>{@link #REQUIRED} — join the current transaction or create a new one</li>
 *     <li>{@link #REQUIRES_NEW} — suspend the current transaction and start a new one</li>
 *     <li>{@link #SUPPORTS} — participate in a transaction if one exists</li>
 *     <li>{@link #NOT_SUPPORTED} — suspend any existing transaction and execute non-transactionally</li>
 *     <li>{@link #MANDATORY} — require an existing transaction, otherwise fail</li>
 *     <li>{@link #NEVER} — forbid execution within a transaction</li>
 *     <li>{@link #NESTED} — execute within a nested transaction (typically using savepoints)</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * @Transactional(propagation = TransactionPropagation.REQUIRES_NEW)
 * public void audit() {
 *     // executes in an independent transaction
 * }
 * }</pre>
 *
 * @author jMouse
 */
public enum TransactionPropagation {
    REQUIRED,
    REQUIRES_NEW,
    SUPPORTS,
    NOT_SUPPORTED,
    MANDATORY,
    NEVER,
    NESTED
}
