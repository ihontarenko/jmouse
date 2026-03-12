package org.jmouse.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Memoizing {@link Function} with thread-safe caching. 🧩
 *
 * <p>
 * Computes values lazily using the provided factory function and
 * stores them in an internal cache. Once computed, the value is
 * reused for subsequent calls with the same key.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * MemoizedFunction<String, Integer> length =
 *         MemoizedFunction.of(String::length);
 *
 * int a = length.apply("hello");
 * int b = length.apply("hello"); // cached
 * }</pre>
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class MemoizedFunction<K, V> implements Function<K, V> {

    private final Function<? super K, ? extends V> factory;
    private final ConcurrentHashMap<K, V>          cache = new ConcurrentHashMap<>();

    /**
     * Creates memoized function using the given factory.
     *
     * @param factory value factory
     */
    public MemoizedFunction(Function<? super K, ? extends V> factory) {
        this.factory = Verify.nonNull(factory, "factory");
    }

    /**
     * Creates memoized function.
     */
    public static <K, V> MemoizedFunction<K, V> of(Function<? super K, ? extends V> factory) {
        return new MemoizedFunction<>(factory);
    }

    /**
     * Returns cached value or computes it if absent.
     */
    @Override
    public V apply(K key) {
        return cache.computeIfAbsent(key, factory);
    }

    /**
     * Alias for {@link #apply(Object)}.
     */
    public V get(K key) {
        return apply(key);
    }

    /**
     * Returns whether the cache contains the given key.
     */
    public boolean contains(K key) {
        return cache.containsKey(key);
    }

    /**
     * Clears cached values.
     */
    public void clear() {
        cache.clear();
    }
}