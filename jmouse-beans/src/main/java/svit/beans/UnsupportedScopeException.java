package svit.beans;

import org.jmouse.core.reflection.Reflections;

/**
 * Exception thrown when an unsupported {@link Scope} is encountered in a bean context or container.
 * <p>
 * This exception is typically used to indicate that the current implementation
 * does not support the specified scope.
 * </p>
 * @see Scope
 */
public class UnsupportedScopeException extends RuntimeException {

    /**
     * Constructs a new exception with a detailed message including the unsupported scope and the caller's class.
     *
     * @param scope  the unsupported {@link Scope}.
     * @param caller the class where the exception occurred.
     */
    public UnsupportedScopeException(Scope scope, Class<?> caller) {
        this("Unsupported bean scope '%s' detected in context '%s'."
                     .formatted(scope.name(), Reflections.getShortName(caller)));
    }

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the detail message.
     */
    public UnsupportedScopeException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public UnsupportedScopeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause the cause of the exception.
     */
    public UnsupportedScopeException(Throwable cause) {
        super(cause);
    }
}
