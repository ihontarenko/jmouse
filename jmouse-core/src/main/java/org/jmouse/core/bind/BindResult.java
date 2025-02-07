package org.jmouse.core.bind;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@code BindResult} class represents the result of a binding operation.
 * <p>
 * Similar to {@link java.util.Optional}, but specifically focused on storing a value
 * that may result from a binding process. Provides methods to retrieve the value
 * with exception handling, default values, or consumer execution.
 * </p>
 *
 * @param <T> the type of the stored value
 */
public final class BindResult<T> {

    private final T value;

    private BindResult(T value) {
        this.value = value;
    }

    /**
     * Creates a {@code BindResult} instance with the given value.
     *
     * @param value the value to be stored
     * @param <T>   the type of the value
     * @return a new {@code BindResult} instance
     */
    public static <T> BindResult<T> of(T value) {
        return new BindResult<>(value);
    }

    /**
     * Creates an empty {@code BindResult}.
     *
     * @param <T> the type of the value
     * @return an empty binding result
     */
    public static <T> BindResult<T> empty() {
        return of(null);
    }

    /**
     * Returns the stored value or throws an exception if the value is absent.
     *
     * @param exceptionSupplier a supplier providing the exception to throw if the value is absent
     * @return the stored value
     * @throws Error if the value is absent
     */
    public T getValue(Supplier<? extends RuntimeException> exceptionSupplier) {
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
            Objects.requireNonNullElseGet(consumer, () -> v -> {}).accept(value);
        }
    }

    /**
     * Returns the contained value if present; otherwise, returns the provided default value.
     *
     * @param defaultValue the default value to return if no value is present
     * @return the contained value if present, otherwise {@code defaultValue}
     */
    public T orElse(T defaultValue) {
        return isPresent() ? value : defaultValue;
    }

    /**
     * Returns the contained value if present; otherwise, returns the result of the provided supplier.
     *
     * @param supplier the supplier to provide a fallback value
     * @return the contained value if present, otherwise the supplied value
     */
    public T orElse(Supplier<? extends T> supplier) {
        return isPresent() ? value : Objects.requireNonNullElse(supplier, () -> null).get();
    }

    @Override
    public String toString() {
        return "Bound: [value=%s]".formatted(value);
    }
}
