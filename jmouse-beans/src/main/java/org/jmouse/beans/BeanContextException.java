package org.jmouse.beans;

/**
 * An exception indicating errors or abnormal conditions encountered within a structured context.
 */
public class BeanContextException extends RuntimeException {

    /**
     * Constructs a new {@code BeanContextException} with no detail message or cause.
     */
    public BeanContextException() {
        super();
    }

    /**
     * Constructs a new {@code BeanContextException} with the specified detail message.
     *
     * @param message the detail message
     */
    public BeanContextException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code BeanContextException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public BeanContextException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code BeanContextException} with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public BeanContextException(Throwable cause) {
        super(cause);
    }
}
