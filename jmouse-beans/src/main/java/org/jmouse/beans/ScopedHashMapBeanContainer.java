package org.jmouse.beans;

/**
 * A default implementation of the {@link ScopedBeanContainer} that integrates with {@link ScopeResolver}.
 * <p>
 * This container delegates structured operations (retrieval, registration, and existence checks) to specific
 * {@link BeanContainer}s based on the resolved {@link Scope} of a structured name.
 * </p>
 * <p>
 * Typical usage involves defining a {@link ScopeResolver} to manage the lifecycle of beans dynamically based on
 * their scope. For binder, singletons, prototypes, or tenant-specific beans can be managed seamlessly.
 * </p>
 *
 * @see ScopedBeanContainer
 * @see ConcurrentHashMapBeanContainerRegistry
 * @see ScopeResolver
 */
public class ScopedHashMapBeanContainer extends ConcurrentHashMapBeanContainerRegistry implements ScopedBeanContainer {

    /**
     * The resolver responsible for determining the {@link Scope} of a structured based on its name.
     */
    private final ScopeResolver scopeResolver;

    /**
     * Constructs a {@code DefaultScopedBeanContainer} with the specified {@link ScopeResolver}.
     * <p>
     * The {@code scopeResolver} is used to resolve the {@link Scope} for a given structured name, allowing
     * the container to delegate operations to the appropriate {@link BeanContainer}.
     * </p>
     *
     * @param scopeResolver the {@link ScopeResolver} for resolving structured scopes.
     */
    public ScopedHashMapBeanContainer(ScopeResolver scopeResolver) {
        this.scopeResolver = scopeResolver;
    }

    /**
     * Retrieves a structured instance by its name, delegating to the appropriate {@link BeanContainer}
     * based on the resolved {@link Scope}.
     *
     * @param name the name of the structured to retrieve.
     * @param <T>  the type of the structured.
     * @return the structured instance, or {@code null} if no structured is found with the given name.
     */
    @Override
    public <T> T getBean(String name) {
        return getBeanContainer(scopeResolver.resolveScope(name)).getBean(name);
    }

    /**
     * Registers a structured instance with the specified name, delegating to the appropriate {@link BeanContainer}
     * based on the resolved {@link Scope}.
     *
     * @param name the name of the structured.
     * @param bean the structured instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {
        getBeanContainer(scopeResolver.resolveScope(name)).registerBean(name, bean);
    }

    /**
     * Checks whether a structured with the specified name is registered, delegating to the appropriate
     * {@link BeanContainer} based on the resolved {@link Scope}.
     *
     * @param name the name of the structured to check.
     * @return {@code true} if the structured exists in the container, {@code false} otherwise.
     */
    @Override
    public boolean containsBean(String name) {
        return getBeanContainer(scopeResolver.resolveScope(name)).containsBean(name);
    }

    @Override
    public String toString() {
        return "ScopedBeanContainer: " + super.toString();
    }

}
