package org.jmouse.tx;

public class UnsupportedTransactionException extends TransactionException {
    public UnsupportedTransactionException(String message) {
        super(message);
    }

    public UnsupportedTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
