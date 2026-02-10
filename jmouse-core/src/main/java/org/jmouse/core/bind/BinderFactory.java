package org.jmouse.core.bind;

import org.jmouse.core.access.TypedValue;

import java.util.Collection;

/**
 * Factory for managing and providing {@link ObjectBinder} instances.
 * This interface allows dynamic registration, retrieval, and removal of binders.
 */
public interface BinderFactory {

    /**
     * Retrieves an appropriate {@link ObjectBinder} for the given {@link TypedValue} type.
     *
     * @param bindable the target bindable type
     * @return the matching {@link ObjectBinder}, or {@code null} if none is found
     */
    ObjectBinder getBinderFor(TypedValue<?> bindable);

    /**
     * Registers a new {@link ObjectBinder} in the factory.
     * Registered binders are used to resolve binding requests.
     *
     * @param binder the binder to register
     */
    void registerBinder(ObjectBinder binder);

    /**
     * Returns all registered {@link ObjectBinder} instances.
     *
     * @return a collection of registered binders
     */
    Collection<ObjectBinder> getBinders();

    /**
     * Unregisters a previously registered {@link ObjectBinder}.
     *
     * @param binder the binder to remove
     */
    void unregisterBinder(ObjectBinder binder);

    /**
     * Clears all registered {@link ObjectBinder} instances.
     */
    void clearBinders();
}
