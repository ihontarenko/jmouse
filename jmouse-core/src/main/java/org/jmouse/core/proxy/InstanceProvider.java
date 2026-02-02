package org.jmouse.core.proxy;

import java.util.function.Supplier;

/**
 * 🎭 Abstraction for providing target instances.
 *
 * <p>Defines how proxy targets are supplied at runtime.
 * Implementations may return the <em>same</em> instance, create
 * a <em>new</em> instance each time, or provide context-specific
 * variants (e.g., thread-local).</p>
 *
 * <h3>Lifecycle strategies</h3>
 * <ul>
 *   <li>🔒 {@link #singleton(Object)} – always the same instance</li>
 *   <li>♻️ {@link #prototype(Supplier)} – new instance for each call</li>
 *   <li>🧵 {@link #threadLocal(Supplier)} – one instance per thread</li>
 * </ul>
 *
 * <p>Functional interface: can be used as a lambda or method reference.</p>
 *
 * @param <T> the type of instance provided
 */
@FunctionalInterface
public interface InstanceProvider<T> {

    /**
     * Supplies an instance of type {@code T}.
     *
     * @return the provided instance (never {@code null} unless explicitly designed so)
     */
    T get();

    /**
     * 🔒 Singleton valueProvider – always returns the same fixed instance.
     *
     * @param instance the single instance to return
     * @param <T>      the type of the instance
     * @return a valueProvider that always returns {@code instance}
     */
    static <T> InstanceProvider<T> singleton(T instance) {
        return new SingletonProvider<>(instance);
    }

    /**
     * ♻️ Prototype valueProvider – creates a new instance on each call.
     *
     * @param supplier factory to create instances
     * @param <T>      the type of the instance
     * @return a valueProvider that calls {@code supplier.get()} each time
     */
    static <T> InstanceProvider<T> prototype(Supplier<T> supplier) {
        return new PrototypeProvider<>(supplier);
    }

    /**
     * 🧵 Thread-local valueProvider – provides one instance per thread.
     *
     * <p>Each thread will lazily receive its own instance, supplied
     * by {@code supplier}, which is cached for subsequent calls.</p>
     *
     * @param supplier factory to create instances
     * @param <T>      the type of the instance
     * @return a valueProvider bound to the current thread
     */
    static <T> InstanceProvider<T> threadLocal(Supplier<T> supplier) {
        return new ThreadLocalProvider<>(supplier);
    }
}
