package org.jmouse.scheduler.spi;

/** A {@link TriggerStore} could not read or write — the database is down, a row is malformed. */
public class TriggerStoreException extends RuntimeException {

    /**
     * @param message what was being done
     * @param cause   the underlying failure
     */
    public TriggerStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
