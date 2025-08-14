package org.jmouse.core;

import java.util.function.Supplier;

/**
 * A {@link Supplier} wrapper that ensures a null-safe value retrieval.
 * <p>
 * {@code NullSafeSupplier} attempts to fetch a value from the given delegate {@code Supplier}.
 * If a {@link NullPointerException} occurs, it falls back to a predefined fallback supplier.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * NullSafeSupplier<String> safeSupplier = new NullSafeSupplier<>(
 *      () -> null, () -> "default");
 * System.out.println(safeSupplier.get()); // Prints: "default"
 * }</pre>
 *
 * @param <T> the type of the supplied value
 */
public class NullSafeSupplier<T> implements Supplier<T> {

    private final Supplier<T> delegate;
    private final Supplier<T> fallback;

    /**
     * Constructs a {@code NullSafeSupplier} with a delegate and a fallback supplier.
     *
     * @param delegate the primary supplier
     * @param fallback the fallback supplier in case of a {@link NullPointerException}
     */
    public NullSafeSupplier(Supplier<T> delegate, Supplier<T> fallback) {
        this.delegate = delegate;
        this.fallback = fallback;
    }

    /**
     * Constructs a {@code NullSafeSupplier} with a delegate supplier.
     * If an exception occurs, it returns {@code null}.
     *
     * @param delegate the primary supplier
     */
    public NullSafeSupplier(Supplier<T> delegate) {
        this(delegate, () -> null);
    }

    /**
     * Retrieves the value from the delegate supplier.
     * If a {@link NullPointerException} occurs, it returns the fallback value.
     *
     * @return the supplied value or the fallback if an exception occurs
     */
    @Override
    public T get() {
        try {
            return delegate.get();
        } catch (NullPointerException npe) {
            return fallback.get();
        }
    }

    /**
     * Factory method for creating a {@code NullSafeSupplier} with a fallback.
     *
     * @param delegate the primary supplier
     * @param fallback the fallback supplier in case of an exception
     * @param <T>      the type of the supplied value
     * @return a new instance of {@code NullSafeSupplier}
     */
    public static <T> NullSafeSupplier<T> of(Supplier<T> delegate, Supplier<T> fallback) {
        return new NullSafeSupplier<>(delegate, fallback);
    }

    /**
     * Factory method for creating a {@code NullSafeSupplier} without a fallback.
     * Returns {@code null} if an exception occurs.
     *
     * @param delegate the primary supplier
     * @param <T>      the type of the supplied value
     * @return a new instance of {@code NullSafeSupplier}
     */
    public static <T> NullSafeSupplier<T> of(Supplier<T> delegate) {
        return new NullSafeSupplier<>(delegate, () -> null);
    }

}
