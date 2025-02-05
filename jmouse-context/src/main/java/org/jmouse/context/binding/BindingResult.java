package org.jmouse.context.binding;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@code BindingResult} class represents the result of a binding operation.
 * <p>
 * Similar to {@link java.util.Optional}, but specifically focused on storing a value
 * that may result from a binding process. Provides methods to retrieve the value
 * with exception handling, default values, or consumer execution.
 * </p>
 *
 * @param <T> the type of the stored value
 */
public final class BindingResult<T> {

    private final T value;

    private BindingResult(T value) {
        this.value = value;
    }

    /**
     * Creates a {@code BindingResult} instance with the given value.
     *
     * @param value the value to be stored
     * @param <T>   the type of the value
     * @return a new {@code BindingResult} instance
     */
    public static <T> BindingResult<T> of(T value) {
        return new BindingResult<>(value);
    }

    /**
     * Creates an empty {@code BindingResult}.
     *
     * @param <T> the type of the value
     * @return an empty binding result
     */
    public static <T> BindingResult<T> empty() {
        return of(null);
    }

    /**
     * Returns the stored value or throws an exception if the value is absent.
     *
     * @param exceptionSupplier a supplier providing the exception to throw if the value is absent
     * @return the stored value
     * @throws Error if the value is absent
     */
    public T getValue(Supplier<? extends Error> exceptionSupplier) {
        if (!isPresent()) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    /**
     * Returns the stored value or a default value if the value is absent.
     *
     * @param defaultValue the default value to return if the stored value is absent
     * @return the stored value or {@code defaultValue} if absent
     */
    public T getValue(T defaultValue) {
        return isPresent() ? value : defaultValue;
    }

    /**
     * Returns the stored value (which may be {@code null}).
     *
     * @return the stored value
     */
    public T getValue() {
        return value;
    }

    /**
     * Checks if the stored value is absent.
     *
     * @return {@code true} if the stored value is {@code null}, otherwise {@code false}
     */
    public boolean isEmpty() {
        return value == null;
    }

    /**
     * Checks if the stored value is present.
     *
     * @return {@code true} if the stored value is not {@code null}, otherwise {@code false}
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * Performs the given action if the value is present.
     *
     * @param consumer the action to perform on the value
     */
    public void ifPresent(Consumer<T> consumer) {
        if (isPresent()) {
            consumer.accept(value);
        }
    }

    @Override
    public String toString() {
        return "Bound: [value=%s]".formatted(value);
    }
}
