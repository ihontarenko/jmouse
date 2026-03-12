package org.jmouse.core.events;

import org.jmouse.core.SingletonSupplier;

import java.util.function.Supplier;

/**
 * Lazy singleton access point for {@link EventManager}. 🔔
 *
 * <p>
 * The manager instance is created once using {@link EventManagerFactory}
 * and cached via {@link SingletonSupplier}. Subsequent calls return the
 * same {@link EventManager} instance.
 * </p>
 *
 * <p>
 * Optional base classes may be provided to configure event discovery
 * during the first initialization.
 * </p>
 */
final public class EventManagerSingleton {

    /**
     * Lazily initialized supplier of the {@link EventManager}.
     */
    private static volatile Supplier<EventManager> instanceSupplier;

    private EventManagerSingleton() { }

    /**
     * Returns the singleton {@link EventManager}.
     *
     * <p>
     * If the manager has not been initialized yet, it will be created
     * using the provided base classes.
     * </p>
     *
     * @param baseClasses optional base classes used for event discovery
     * @return singleton event manager
     */
    public synchronized static EventManager getEventManager(Class<?>... baseClasses) {
        Supplier<EventManager> supplier = instanceSupplier;

        if (supplier == null) {
            supplier = getEventManagerSupplier(baseClasses);
            instanceSupplier = supplier;
        }

        return supplier.get();
    }

    /**
     * Creates a supplier for lazy {@link EventManager} initialization.
     *
     * @param baseClasses base classes used by {@link EventManagerFactory}
     * @return singleton supplier
     */
    public static Supplier<EventManager> getEventManagerSupplier(Class<?>... baseClasses) {
        return SingletonSupplier.of(() -> EventManagerFactory.create(baseClasses));
    }

}