package org.jmouse.web.context;

/**
 * A custom exception for web-related HTTP errors.
 * <p>
 * Example usage:
 * <pre>{@code
 * throw new WebHttpException("404 Not Found");
 * }</pre>
 */
public class WebContextException extends RuntimeException {

    /**
     * Constructs a new {@code WebHttpException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public WebContextException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code WebHttpException} with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     */
    public WebContextException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code WebHttpException} with the specified cause.
     *
     * @param cause the cause of this exception.
     */
    public WebContextException(Throwable cause) {
        super(cause);
    }
}
