package org.jmouse.core;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Lazily creates and caches a single instance using an initialization argument. 🧩
 *
 * <p>
 * The argument is used only during the first successful initialization.
 * Once initialized, all subsequent calls return the same cached instance,
 * regardless of the provided argument.
 * </p>
 *
 * <p>
 * This is useful when object construction requires runtime data,
 * but the created object itself should still behave like a singleton.
 * </p>
 *
 * <pre>{@code
 * SingletonFactory<Config, Service> factory =
 *         SingletonFactory.of(Service::new);
 *
 * Service service1 = factory.get(config);
 * Service service2 = factory.get(otherConfig);
 *
 * assert service1 == service2;
 * }</pre>
 *
 * @param <A> initialization argument type
 * @param <T> instance type
 */
public final class SingletonFactory<A, T> implements ParameterizedSupplier<A, T> {

    private final Function<? super A, ? extends T> factory;
    private final Function<? super A, ? extends T> fallback;
    private final ReentrantLock                    lock = new ReentrantLock();

    private volatile T instance;

    public SingletonFactory(
            Function<? super A, ? extends T> factory,
            Function<? super A, ? extends T> fallback,
            T instance
    ) {
        this.factory = factory;
        this.fallback = fallback;
        this.instance = instance;
    }

    public static <A, T> SingletonFactory<A, T> of(Function<? super A, ? extends T> factory) {
        return new SingletonFactory<>(factory, null, null);
    }

    public static <A, T> SingletonFactory<A, T> of(
            Function<? super A, ? extends T> factory,
            Function<? super A, ? extends T> fallback
    ) {
        return new SingletonFactory<>(factory, fallback, null);
    }

    public static <A, T> SingletonFactory<A, T> of(T instance) {
        return new SingletonFactory<>(null, null, instance);
    }

    @Override
    public T get(A argument) {
        T value = this.instance;

        if (value == null) {
            lock.lock();
            try {
                value = this.instance;

                if (value == null) {
                    if (factory != null) {
                        value = factory.apply(argument);

                        if (value == null && fallback != null) {
                            value = fallback.apply(argument);
                        }
                    }

                    this.instance = value;
                }
            } finally {
                lock.unlock();
            }
        }

        return value;
    }

}