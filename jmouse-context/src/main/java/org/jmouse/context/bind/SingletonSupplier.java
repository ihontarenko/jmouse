package org.jmouse.context.bind;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A {@link Supplier} implementation that ensures a value is initialized only once (lazy singleton).
 * <p>
 * {@code SingletonSupplier} lazily initializes and caches the supplied instance.
 * If the primary supplier returns {@code null}, a fallback supplier (if provided) is used.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Supplier<String> supplier = SingletonSupplier.of(() -> "Hello, Singleton!");
 * System.out.println(supplier.get()); // Prints: "Hello, Singleton!"
 * }</pre>
 *
 * @param <T> the type of the supplied value
 */
public class SingletonSupplier<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private final Supplier<T> fallback;
    private volatile T instance;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Constructs a {@code SingletonSupplier} with a supplier, fallback, and an initial instance.
     *
     * @param supplier the primary supplier
     * @param fallback the fallback supplier if the primary one returns {@code null}
     * @param instance the initial instance (can be pre-set)
     */
    public SingletonSupplier(Supplier<T> supplier, Supplier<T> fallback, T instance) {
        this.supplier = supplier;
        this.fallback = fallback;
        this.instance = instance;
    }

    /**
     * Creates a {@code SingletonSupplier} with a primary and fallback supplier.
     *
     * @param supplier the primary supplier
     * @param fallback the fallback supplier if the primary one returns {@code null}
     * @param <T>      the type of the supplied value
     * @return a new instance of {@code SingletonSupplier}
     */
    public static <T> Supplier<T> of(Supplier<T> supplier, Supplier<T> fallback) {
        return new SingletonSupplier<>(supplier, fallback, null);
    }

    /**
     * Creates a {@code SingletonSupplier} with only a primary supplier.
     * If the supplier returns {@code null}, the result remains {@code null}.
     *
     * @param supplier the primary supplier
     * @param <T>      the type of the supplied value
     * @return a new instance of {@code SingletonSupplier}
     */
    public static <T> Supplier<T> of(Supplier<T> supplier) {
        return of(supplier, () -> null);
    }

    /**
     * Creates a {@code SingletonSupplier} that always returns the provided instance.
     *
     * @param instance the pre-defined instance
     * @param <T>      the type of the supplied value
     * @return a new instance of {@code SingletonSupplier}
     */
    public static <T> Supplier<T> of(T instance) {
        return new SingletonSupplier<>(null, () -> null, instance);
    }

    /**
     * Returns the singleton instance, initializing it if necessary.
     * <p>
     * The method ensures thread safety using a {@link ReentrantLock}.
     * If the primary supplier returns {@code null}, the fallback supplier (if provided) is used.
     * </p>
     *
     * @return the supplied instance
     */
    @Override
    public T get() {
        T instance = this.instance;

        if (instance == null) {
            try {
                lock.lock();
                instance = this.instance;
                if (instance == null) {
                    if (supplier != null) {
                        instance = supplier.get();
                        if (instance == null && fallback != null) {
                            instance = fallback.get();
                        }
                    }
                    this.instance = instance;
                }
            } finally {
                lock.unlock();
            }
        }

        return instance;
    }

}
