package org.jmouse.core;

import java.util.List;

/**
 * Utility class for common exception handling and validation checks.
 * <p>
 * This class provides static methods to enforce validation constraints
 * and throw exceptions when conditions are not met.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Exceptions.thrownIfFalse(user.isActive(), "User must be active");
 * Exceptions.throwIfNull(config, "Configuration must be provided");
 * Exceptions.throwIfOutOfRange(index, 0, 100, "Index is out of bounds");
 * }</pre>
 *
 * @author JMouse
 */
public final class Exceptions {

    private Exceptions() {
    }

    public static Throwable unwrapIf(Throwable exception, Class<? extends Throwable>... types) {
        List<Class<? extends Throwable>> exceptionTypes = List.of(types);
        Throwable                        throwable      = exception;

        while (exceptionTypes.contains(throwable.getClass())) {
            throwable = throwable.getCause();
        }

        return throwable;
    }

}
