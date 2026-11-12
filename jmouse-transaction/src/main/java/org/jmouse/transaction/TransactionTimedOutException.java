package org.jmouse.transaction;

/** The transaction deadline elapsed before commit. */
public final class TransactionTimedOutException extends RuntimeException {

    public TransactionTimedOutException() {
        super("Transaction deadline elapsed before commit");
    }
}
