package org.jmouse.core;

import java.util.function.Supplier;

public final class Contract {

    private Contract() {
    }

    public static <T> T argument(T value, String name) {
        return argument(value, ()
                -> new IllegalArgumentException("Required argument '" + name + "' must be non-null"));
    }

    public static <T> T argument(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void require(boolean condition, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    public static <T> T state(T value, String name) {
        state(value != null, ()
                -> new IllegalStateException("Illegal state of '" + name + "'. Must be non-null"));
        return value;
    }

    public static void state(boolean condition, String message) {
        state(condition, () -> new IllegalStateException(message));
    }

    public static void state(boolean condition, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    public static <T> T nonNull(T value, String message) {
        return nonNull(value, () -> new NullPointerException("Required value must be non-null: '" + message + "'."));
    }

    public static <T> T nonNull(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public static void invariant(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Invariant violation: " + message);
        }
    }

    public static void unsupported(String message) {
        throw new UnsupportedOperationException(message);
    }

    public static void forbidden(String message) {
        throw new SecurityException(message);
    }

}
