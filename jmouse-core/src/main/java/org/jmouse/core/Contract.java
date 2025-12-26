package org.jmouse.core;

import java.util.function.Supplier;

/**
 * Centralized contract enforcement utility.
 * <p>
 * Provides a set of static methods to validate method arguments,
 * object state, invariants, and usage constraints.
 * </p>
 * <p>
 * Intended as a lightweight alternative to {@code Objects.requireNonNull}
 * and Spring's {@code Assert}, with explicit semantic categories.
 * </p>
 */
public final class Contract {

    private Contract() {
    }

    /**
     * Validate a required argument.
     *
     * @param value the argument value
     * @param name  the argument name
     * @param <T>   the value type
     * @return the validated value
     * @throws IllegalArgumentException if the value is {@code null}
     */
    public static <T> T argument(T value, String name) {
        return argument(value, ()
                -> new IllegalArgumentException("Required argument '" + name + "' must be non-null"));
    }

    /**
     * Validate a required argument using a custom exception supplier.
     *
     * @param value              the argument value
     * @param exceptionSupplier  exception supplier invoked on violation
     * @param <T>                the value type
     * @return the validated value
     * @throws RuntimeException if the value is {@code null}
     */
    public static <T> T argument(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    /**
     * Require that the given condition holds for method arguments.
     *
     * @param condition the condition to check
     * @param message   the exception message
     * @throws IllegalArgumentException if the condition is {@code false}
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Require that the given condition holds for method arguments.
     *
     * @param condition           the condition to check
     * @param exceptionSupplier   exception supplier invoked on violation
     * @throws RuntimeException if the condition is {@code false}
     */
    public static void require(boolean condition, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Validate that the given value represents a valid object state.
     *
     * @param value the state value
     * @param name  the state name
     * @param <T>   the value type
     * @return the validated value
     * @throws IllegalStateException if the value is {@code null}
     */
    public static <T> T state(T value, String name) {
        state(value != null, ()
                -> new IllegalStateException("Illegal state of '" + name + "'. Must be non-null"));
        return value;
    }

    /**
     * Validate that the given condition represents a valid object state.
     *
     * @param condition the condition to check
     * @param message   the exception message
     * @throws IllegalStateException if the condition is {@code false}
     */
    public static void state(boolean condition, String message) {
        state(condition, () -> new IllegalStateException(message));
    }

    /**
     * Validate that the given condition represents a valid object state.
     *
     * @param condition           the condition to check
     * @param exceptionSupplier   exception supplier invoked on violation
     * @throws RuntimeException if the condition is {@code false}
     */
    public static void state(boolean condition, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Validate that the given value is non-null.
     *
     * @param value   the value to check
     * @param message contextual description
     * @param <T>     the value type
     * @return the validated value
     * @throws NullPointerException if the value is {@code null}
     */
    public static <T> T nonNull(T value, String message) {
        return nonNull(value, () -> new NullPointerException(
                "Required value must be non-null: '" + message + "'."));
    }

    /**
     * Validate that the given value is non-null using a custom exception.
     *
     * @param value              the value to check
     * @param exceptionSupplier  exception supplier invoked on violation
     * @param <T>                the value type
     * @return the validated value
     * @throws RuntimeException if the value is {@code null}
     */
    public static <T> T nonNull(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    /**
     * Assert a system invariant.
     *
     * @param condition the invariant condition
     * @param message   description of the invariant
     * @throws IllegalStateException if the invariant is violated
     */
    public static void invariant(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Invariant violation: " + message);
        }
    }

    /**
     * Signal an unsupported operation.
     *
     * @param message the exception message
     * @throws UnsupportedOperationException always
     */
    public static void unsupported(String message) {
        throw new UnsupportedOperationException(message);
    }

    /**
     * Signal a forbidden operation or access violation.
     *
     * @param message the exception message
     * @throws SecurityException always
     */
    public static void forbidden(String message) {
        throw new SecurityException(message);
    }

}
