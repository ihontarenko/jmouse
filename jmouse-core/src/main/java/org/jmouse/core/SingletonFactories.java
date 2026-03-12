package org.jmouse.core;

import java.util.function.Function;

/**
 * Factory utilities for singleton and memoized functions. 🧩
 *
 * <p>
 * Provides convenient shortcuts for creating commonly used
 * lazy and caching primitives such as {@link SingletonFactory}
 * and {@link MemoizedFunction}.
 * </p>
 */
public final class SingletonFactories {

    private SingletonFactories() {}

    /**
     * Creates a lazily initialized singleton factory.
     *
     * @param factory instance factory
     *
     * @param <A> initialization argument type
     * @param <T> instance type
     *
     * @return singleton factory
     */
    public static <A, T> SingletonFactory<A, T> singleton(Function<? super A, ? extends T> factory) {
        return SingletonFactory.of(factory);
    }

    /**
     * Creates a memoized function with caching.
     *
     * @param factory value factory
     *
     * @param <K> key type
     * @param <V> value type
     *
     * @return memoized function
     */
    public static <K, V> MemoizedFunction<K, V> memoized(Function<? super K, ? extends V> factory) {
        return MemoizedFunction.of(factory);
    }

}