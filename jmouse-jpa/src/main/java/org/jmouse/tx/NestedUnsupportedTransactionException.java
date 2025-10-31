package org.jmouse.tx;

public class NestedUnsupportedTransactionException extends UnsupportedTransactionException {
    public NestedUnsupportedTransactionException(String message) {
        super(message);
    }

    public NestedUnsupportedTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
