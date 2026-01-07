package org.jmouse.core;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Centralized contract enforcement utility.
 *
 * <p>
 * Provides a cohesive set of static methods to validate:
 * </p>
 * <ul>
 *   <li><b>Arguments</b> (preconditions) – throws {@link IllegalArgumentException}</li>
 *   <li><b>State</b> – throws {@link IllegalStateException}</li>
 *   <li><b>Nullability</b> – throws {@link NullPointerException} (when explicitly required)</li>
 *   <li><b>Invariants</b> – throws {@link IllegalStateException}</li>
 * </ul>
 *
 * <p>
 * Designed as a lightweight alternative to {@link Objects#requireNonNull(Object)}
 * and Spring's {@code Assert}, but with explicit semantic categories.
 * </p>
 */
public final class Verify {

    private Verify() {}

    // -------------------------------------------------------------------------
    // Argument (preconditions)
    // -------------------------------------------------------------------------

    /**
     * Validate a required argument (non-null).
     *
     * @param value the argument value
     * @param name  the argument name (used in the exception message)
     * @param <T>   value type
     * @return the validated value
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public static <T> T argument(T value, String name) {
        return argument(value, () ->
                new IllegalArgumentException("Required argument '" + name + "' must be non-null"));
    }

    /**
     * Validate a required argument (non-null) using a custom exception supplier.
     *
     * @param value             the argument value
     * @param exceptionSupplier exception supplier invoked on violation
     * @param <T>               value type
     * @return the validated value
     * @throws RuntimeException if {@code value} is {@code null}
     */
    public static <T> T argument(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    /**
     * Require that the given argument condition holds.
     *
     * @param condition         condition to check
     * @param messageSupplier   message supplier invoked on violation
     * @throws IllegalArgumentException if {@code condition} is {@code false}
     */
    public static void require(boolean condition, String messageSupplier) {
        require(condition, () -> new IllegalArgumentException(messageSupplier));
    }

    /**
     * Require that the given argument condition holds.
     *
     * @param condition         condition to check
     * @param exceptionSupplier exception supplier invoked on violation
     * @throws RuntimeException if {@code condition} is {@code false}
     */
    public static void require(boolean condition, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Validate that the given string argument is not {@code null} and not blank.
     *
     * @param value string argument to validate
     * @param name  argument name (used in the exception message)
     * @return the validated string
     * @throws IllegalArgumentException if {@code value} is {@code null} or blank
     */
    public static String notBlank(String value, String name) {
        require(value != null && !value.isBlank(),
                "Required argument '" + name + "' must be non-blank");
        return value;
    }

    /**
     * Validate that the given string argument is not {@code null} and not empty.
     *
     * @param value string argument to validate
     * @param name  argument name (used in the exception message)
     * @return the validated string
     * @throws IllegalArgumentException if {@code value} is {@code null} or empty
     */
    public static String notEmpty(String value, String name) {
        require(value != null && !value.isEmpty(),
                "Required argument '" + name + "' must be non-empty");
        return value;
    }

    /**
     * Validate that the given collection argument is not {@code null} and not empty.
     *
     * @param value collection argument to validate
     * @param name  argument name (used in the exception message)
     * @param <C>   collection type
     * @return the validated collection
     * @throws IllegalArgumentException if {@code value} is {@code null} or empty
     */
    public static <C extends Collection<?>> C notEmpty(C value, String name) {
        require(value != null && !value.isEmpty(),
                "Required argument '" + name + "' must be a non-empty collection");
        return value;
    }

    /**
     * Validate that the given map argument is not {@code null} and not empty.
     *
     * @param value map argument to validate
     * @param name  argument name (used in the exception message)
     * @param <M>   map type
     * @return the validated map
     * @throws IllegalArgumentException if {@code value} is {@code null} or empty
     */
    public static <M extends Map<?, ?>> M notEmpty(M value, String name) {
        require(value != null && !value.isEmpty(),
                "Required argument '" + name + "' must be a non-empty map");
        return value;
    }

    /**
     * Validate that the given number is positive (> 0).
     *
     * @param value number to validate
     * @param name  argument name (used in the exception message)
     * @return the validated value
     * @throws IllegalArgumentException if {@code value} is not positive
     */
    public static int positive(int value, String name) {
        require(value > 0, "Required argument '" + name + "' must be > 0");
        return value;
    }

    /**
     * Validate that the given number is non-negative (>= 0).
     *
     * @param value number to validate
     * @param name  argument name (used in the exception message)
     * @return the validated value
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public static int nonNegative(int value, String name) {
        require(value >= 0, "Required argument '" + name + "' must be >= 0");
        return value;
    }

    /**
     * Validate that a value is within an inclusive integer range: {@code [min..max]}.
     *
     * @param value value to validate
     * @param min   inclusive lower bound
     * @param max   inclusive upper bound
     * @param name  argument name (used in the exception message)
     * @return the validated value
     * @throws IllegalArgumentException if value is out of range
     */
    public static int inRange(int value, int min, int max, String name) {
        require(min <= max, "Invalid range for '" + name + "': min > max");
        require(value >= min && value <= max,
                "Argument '%s' must be in range [%d..%d], but was %d".formatted(name, min, max, value));
        return value;
    }

    /**
     * Validate that {@code index} is a valid element index for a structure of {@code size}.
     *
     * <p>
     * Valid index range: {@code [0..size-1]}.
     * </p>
     *
     * @param index index to validate
     * @param size  structure size
     * @param name  index name (used in the exception message)
     * @return the validated index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    public static int index(int index, int size, String name) {
        require(size >= 0, "Argument 'size' must be >= 0");
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Index '" + name + "' out of bounds: " + index + " (size=" + size + ")");
        }
        return index;
    }

    /**
     * Validate that {@code position} is a valid 1-based position for a structure of {@code size}.
     *
     * <p>
     * Valid position range: {@code [1..size]}.
     * </p>
     *
     * @param position 1-based position
     * @param size     structure size
     * @param name     position name
     * @return the validated position
     * @throws IndexOutOfBoundsException if position is out of bounds
     */
    public static int position(int position, int size, String name) {
        require(size >= 0, "Argument 'size' must be >= 0");
        if (position < 1 || position > size) {
            throw new IndexOutOfBoundsException(
                    "Position '" + name + "' out of bounds: " + position + " (size=" + size + ")");
        }
        return position;
    }

    /**
     * Validate that {@code obj} is an instance of {@code type}.
     *
     * @param obj  value to validate
     * @param type required type
     * @param name argument name (used in the exception message)
     * @param <T>  required type
     * @return {@code obj} cast to {@code T}
     * @throws IllegalArgumentException if obj is {@code null} or not an instance of type
     */
    public static <T> T instanceOf(Object obj, Class<T> type, String name) {
        argument(type, "type");
        require(type.isInstance(obj),
                "Argument '" + name + "' must be an instance of " + type.getName() +
                        (obj == null ? ", but was null" : ", but was " + obj.getClass().getName()));
        return type.cast(obj);
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Validate that the given value represents a valid object state (non-null).
     *
     * @param value state value
     * @param name  state name (used in the exception message)
     * @param <T>   value type
     * @return the validated value
     * @throws IllegalStateException if {@code value} is {@code null}
     */
    public static <T> T state(T value, String name) {
        state(value != null, () ->
                new IllegalStateException("Illegal state of '" + name + "'. Must be non-null"));
        return value;
    }

    /**
     * Validate that the given condition represents a valid object state.
     *
     * @param condition         condition to check
     * @throws IllegalStateException if {@code condition} is {@code false}
     */
    public static void state(boolean condition, String message) {
        state(condition, () -> new IllegalStateException(message));
    }

    /**
     * Validate that the given condition represents a valid object state.
     *
     * @param condition         condition to check
     * @param exceptionSupplier exception supplier invoked on violation
     * @throws RuntimeException if {@code condition} is {@code false}
     */
    public static void state(boolean condition, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    // -------------------------------------------------------------------------
    // Nullability
    // -------------------------------------------------------------------------

    /**
     * Validate that the given value is non-null.
     *
     * <p>
     * Prefer {@link #argument(Object, String)} for argument checks and
     * {@link #state(Object, String)} for state checks. This method is intended
     * for explicit nullability enforcement with {@link NullPointerException}.
     * </p>
     *
     * @param value   value to check
     * @param message contextual description
     * @param <T>     value type
     * @return the validated value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static <T> T nonNull(T value, String message) {
        return nonNull(value, () ->
                new NullPointerException("Required value must be non-null: '" + message + "'."));
    }

    /**
     * Validate that the given value is non-null using a custom exception supplier.
     *
     * @param value             value to check
     * @param exceptionSupplier exception supplier invoked on violation
     * @param <T>               value type
     * @return the validated value
     * @throws RuntimeException if {@code value} is {@code null}
     */
    public static <T> T nonNull(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // Invariants & failure helpers
    // -------------------------------------------------------------------------

    /**
     * Assert a system invariant.
     *
     * @param condition invariant condition
     * @param message   description of the invariant
     * @throws IllegalStateException if the invariant is violated
     */
    public static void invariant(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Invariant violation: " + message);
        }
    }

    /**
     * Always fail with an {@link IllegalArgumentException}.
     *
     * @param message exception message
     * @return never returns normally
     * @throws IllegalArgumentException always
     */
    public static IllegalArgumentException failArgument(String message) {
        throw new IllegalArgumentException(message);
    }

    /**
     * Always fail with an {@link IllegalStateException}.
     *
     * @param message exception message
     * @return never returns normally
     * @throws IllegalStateException always
     */
    public static IllegalStateException failState(String message) {
        throw new IllegalStateException(message);
    }

    /**
     * Signal an unsupported operation.
     *
     * @param message exception message
     * @throws UnsupportedOperationException always
     */
    public static void unsupported(String message) {
        throw new UnsupportedOperationException(message);
    }

    /**
     * Signal a forbidden operation or access violation.
     *
     * @param message exception message
     * @throws SecurityException always
     */
    public static void forbidden(String message) {
        throw new SecurityException(message);
    }
}
