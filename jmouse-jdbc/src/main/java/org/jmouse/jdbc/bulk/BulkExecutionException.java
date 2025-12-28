package org.jmouse.jdbc.bulk;

public class BulkExecutionException extends RuntimeException {

    public BulkExecutionException(String message) {
        super(message);
    }

    public BulkExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public BulkExecutionException(Throwable cause) {
        super(cause);
    }

}
