package org.jmouse.core.mapping.strategy.support;

import java.util.Iterator;
import java.util.OptionalInt;

/**
 * Uniform abstraction over iterable-like sources used by collection/array mapping strategies. 🔁
 *
 * <p>{@code IterableSource} allows mapping code to handle different input shapes
 * (collections, arrays, custom iterables, streams, etc.) through a common API.</p>
 *
 * <p>Implementations may optionally expose a cheap known size and indexed access to enable
 * optimized mapping paths.</p>
 */
public interface IterableSource {

    /**
     * Return an iterator over source elements.
     *
     * @return element iterator
     */
    Iterator<?> iterator();

    /**
     * Return the known size of the source if it is cheap/available.
     *
     * @return known size, or {@link OptionalInt#empty()} when unknown
     */
    OptionalInt knownSize();

    /**
     * Return the element at the given index.
     *
     * <p>This method is optional and should be implemented only when indexed access
     * is efficient (e.g., arrays, {@link java.util.List}).</p>
     *
     * @param index element index
     * @return element value
     * @throws UnsupportedOperationException if indexed access is not supported
     */
    default Object get(int index) {
        throw new UnsupportedOperationException("Indexed access is not supported.");
    }

    /**
     * Whether this source supports cheap indexed access via {@link #get(int)}.
     *
     * @return {@code true} when indexed access is supported, otherwise {@code false}
     */
    default boolean isIndexed() {
        return false;
    }
}
