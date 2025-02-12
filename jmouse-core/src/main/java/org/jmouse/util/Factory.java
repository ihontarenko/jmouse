package org.jmouse.util;

import java.util.function.Supplier;

/**
 * A functional interface representing a factory for creating instances of type {@code T}.
 * <p>
 * This interface allows encapsulating bean creation logic, making it useful in contexts
 * where dependency injection, lazy initialization, or configurable instantiation is required.
 * </p>
 *
 * @param <T> the type of bean this factory creates
 */
@FunctionalInterface
public interface Factory<T> {

    /**
     * Creates a factory from a given supplier.
     *
     * @param supplier the supplier providing instances of {@code T}
     * @param <T>      the type of the created bean
     * @return a factory that delegates to the supplier
     */
    static <T> Factory<T> of(Supplier<T> supplier) {
        return supplier::get;
    }

    /**
     * Creates and returns a new instance of {@code T}.
     *
     * @return a new instance of {@code T}
     */
    T create();
}
