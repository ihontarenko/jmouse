package org.jmouse.context.binding;

import java.util.function.Supplier;

/**
 * A {@link Supplier} implementation that caches the result of the first invocation.
 * <p>
 * This supplier lazily initializes and caches the value returned by the provided factory supplier.
 * Subsequent calls to {@link #get()} return the cached value instead of invoking the factory again.
 * </p>
 *
 * @param <T> the type of the value supplied
 * <p>
 * Example:
 * <pre>{@code
 * Supplier<Integer> cachedSupplier = new CachedSupplier<>(() -> expensiveComputation());
 * Integer value1 = cachedSupplier.get(); // Calls the factory
 * Integer value2 = cachedSupplier.get(); // Returns cached result
 * }</pre>
 */
public class CachedSupplier<T> implements Supplier<T> {

    private final Supplier<T> factory;
    private T value;

    /**
     * Creates a {@link CachedSupplier} with the given factory supplier.
     *
     * @param factory the supplier responsible for producing the value
     */
    public CachedSupplier(Supplier<T> factory) {
        this.factory = factory;
    }

    /**
     * Returns the cached value if available; otherwise, computes and caches it.
     *
     * @return the cached or newly computed value
     */
    @Override
    public T get() {
        T cached = value;

        if (cached == null) {
            cached = factory.get();
            value = cached;
        }

        return cached;
    }
}
