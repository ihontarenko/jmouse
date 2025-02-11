package org.jmouse.testing_ground.beans;

import org.jmouse.util.Delegate;

/**
 * A {@link BeanContainerRegistry} implementation that delegates all operations to another registry.
 * <p>
 * This class is useful for scenarios where you want to dynamically switch or wrap an existing
 * {@link BeanContainerRegistry} instance while preserving its functionality.
 * </p>
 */
public class DelegateBeanContainerRegistry implements BeanContainerRegistry, Delegate<BeanContainerRegistry> {

    /**
     * The underlying {@link BeanContainerRegistry} to which all operations are delegated.
     */
    private BeanContainerRegistry delegate;

    /**
     * Constructs a {@code DelegateBeanContainerRegistry} with the specified delegate.
     *
     * @param delegate the {@link BeanContainerRegistry} to which operations will be delegated.
     */
    public DelegateBeanContainerRegistry(BeanContainerRegistry delegate) {
        this.delegate = delegate;
    }

    /**
     * Retrieves the {@link BeanContainer} associated with the given {@link Scope}.
     *
     * @param scope the {@link Scope} for which to retrieve the container.
     * @return the {@link BeanContainer} associated with the specified scope.
     */
    @Override
    public BeanContainer getBeanContainer(Scope scope) {
        return delegate.getBeanContainer(scope);
    }

    /**
     * Registers a {@link BeanContainer} for the specified {@link Scope}.
     *
     * @param scope     the scope for which the container is being registered.
     * @param container the {@link BeanContainer} to associate with the scope.
     */
    @Override
    public void registerBeanContainer(Scope scope, BeanContainer container) {
        delegate.registerBeanContainer(scope, container);
    }

    /**
     * Removes all registered {@link BeanContainer}s from the registry.
     */
    @Override
    public void removeBeanInstanceContainers() {
        delegate.removeBeanInstanceContainers();
    }

    /**
     * Checks if a {@link BeanContainer} is registered for the specified {@link Scope}.
     *
     * @param scope the scope to check for a registered container.
     * @return {@code true} if a container is registered for the scope, {@code false} otherwise.
     */
    @Override
    public boolean containsBeanContainer(Scope scope) {
        return delegate.containsBeanContainer(scope);
    }

    /**
     * Sets the delegate {@link BeanContainerRegistry}.
     *
     * @param delegate the delegate to set.
     */
    @Override
    public void setDelegate(BeanContainerRegistry delegate) {
        this.delegate = delegate;
    }

    /**
     * Retrieves the current delegate {@link BeanContainerRegistry}.
     *
     * @return the delegate instance, or {@code null} if no delegate is set.
     */
    @Override
    public BeanContainerRegistry getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return "Delegate: " + delegate.toString();
    }

}
