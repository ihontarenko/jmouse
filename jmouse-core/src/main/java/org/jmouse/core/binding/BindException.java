package org.jmouse.core.binding;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.TypedValue;

/**
 * Exception thrown when a binding operation fails.
 */
public class BindException extends RuntimeException {

    /**
     * Constructs a {@link BindException} with a specified message.
     */
    public BindException(String message) {
        super(message);
    }

    /**
     * Constructs a {@link BindException} with a specified message and cause.
     */
    public BindException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@link BindException} for a failed binding operation,
     * indicating the property path and the expected type.
     */
    public BindException(PropertyPath path, TypedValue<?> bindable, Throwable cause) {
        super(getExceptionMessage(path, bindable), cause);
    }

    /**
     * Generates a detailed exception message for a binding failure.
     */
    public static String getExceptionMessage(PropertyPath path, TypedValue<?> bindable) {
        return "Binding failure: '%s' could not be assigned from '%s'.".formatted(bindable.getType(), path);
    }
}
