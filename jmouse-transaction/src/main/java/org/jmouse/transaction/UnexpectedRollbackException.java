package org.jmouse.transaction;

/** The requested commit completed by rolling back a rollback-only transaction. */
public final class UnexpectedRollbackException extends RuntimeException {

    public UnexpectedRollbackException() {
        super("Transaction was marked rollback-only and could not commit");
    }
}
