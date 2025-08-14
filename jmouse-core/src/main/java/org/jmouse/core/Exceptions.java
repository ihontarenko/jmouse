package org.jmouse.core;

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

    private Exceptions() { }

    /**
     * Throws an {@link IllegalArgumentException} if the given condition is false.
     *
     * @param condition the condition to check
     * @param message   the error message if the condition is false
     * @throws IllegalArgumentException if {@code condition} is false
     */
    public static void thrownIfFalse(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException("Assertion failed: " + message);
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given condition is true.
     *
     * @param condition the condition to check
     * @param message   the error message if the condition is true
     * @throws IllegalArgumentException if {@code condition} is true
     */
    public static void thrownIfTrue(boolean condition, String message) {
        thrownIfFalse(!condition, message);
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given object is null.
     *
     * @param object  the object to check
     * @param message the error message if the object is null
     * @throws IllegalArgumentException if {@code object} is null
     */
    public static void throwIfNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException("Must be non-NULL: " + message);
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given index is out of range.
     *
     * @param index   the index to check
     * @param min     the minimum allowed value (inclusive)
     * @param max     the maximum allowed value (inclusive)
     * @param message the error message if the index is out of range
     * @throws IllegalArgumentException if {@code index} is not between {@code min} and {@code max}
     */
    public static void throwIfOutOfRange(int index, int min, int max, String message) {
        if (index < min || index > max) {
            throw new IllegalArgumentException("Must be between %d and %d inclusive: %s"
                                                       .formatted(min, max, message));
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the index is out of range for the given array.
     *
     * @param array   the array to check against
     * @param index   the index to check
     * @param message the error message if the index is out of range
     * @throws IllegalArgumentException if {@code index} is not within the array bounds
     * @throws IllegalArgumentException if {@code array} is null
     */
    public static void throwIfOutOfRange(Object[] array, int index, String message) {
        throwIfNull(array, message);
        throwIfOutOfRange(index, 0, array.length, message);
    }

}
