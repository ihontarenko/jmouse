package org.jmouse.storage.exception;

/**
 * 💥 Base of every failure the storage layer reports.
 *
 * <p>Storage owns its own hierarchy rather than borrowing an application's, so the library stays
 * usable from a jMouse context, a Spring context and a plain {@code main} method alike. Callers
 * that only want "something went wrong with the bytes" can catch this one type.</p>
 */
public class StorageException extends RuntimeException {

    /**
     * 🏗️ Create a failure with a message.
     *
     * @param message what went wrong
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * 🏗️ Create a failure with a message and an underlying cause.
     *
     * @param message what went wrong
     * @param cause   the original failure
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
