package org.jmouse.testing_ground.beans;

/**
 * Interface for managing the registry of {@link BeanContainer}s across different {@link Scope}s.
 * <p>
 * This interface provides methods to register, retrieve, and manage bean containers based on their scopes.
 * It is a central component for managing beans in a multi-scope environment.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BeanContainerRegistry registry = new SimpleBeanContainerRegistry();
 * BeanContainer singletonContainer = new SingletonBeanContainer();
 *
 * registry.registerBeanContainer(BeanScope.SINGLETON, singletonContainer);
 *
 * if (registry.containsBeanContainer(BeanScope.SINGLETON)) {
 *     BeanContainer container = registry.getBeanContainer(BeanScope.SINGLETON);
 *     System.out.println("Singleton container found: " + container);
 * }
 *
 * registry.removeBeanInstanceContainers();
 * }</pre>
 *
 * @see BeanContainer
 * @see Scope
 * @see BeanScope
 */
public interface BeanContainerRegistry {

    /**
     * Retrieves the {@link BeanContainer} associated with the specified {@link Scope}.
     *
     * @param scope the scope of the desired bean container.
     * @return the corresponding bean container, or {@code null} if none is registered for the given scope.
     * @throws IllegalArgumentException if the scope is {@code null}.
     */
    BeanContainer getBeanContainer(Scope scope);

    /**
     * Registers a {@link BeanContainer} for the specified {@link Scope}.
     *
     * @param scope     the scope of the bean container to register.
     * @param container the bean container to associate with the given scope.
     * @throws IllegalArgumentException if the scope or container is {@code null}.
     */
    void registerBeanContainer(Scope scope, BeanContainer container);

    /**
     * Removes all registered {@link BeanContainer}s from the registry.
     * <p>
     * This method clears all scope-to-container mappings, effectively resetting the registry.
     */
    void removeBeanInstanceContainers();

    /**
     * Checks if a {@link BeanContainer} is registered for the specified {@link Scope}.
     *
     * @param scope the scope to check for a registered bean container.
     * @return {@code true} if a container is registered for the given scope, {@code false} otherwise.
     * @throws IllegalArgumentException if the scope is {@code null}.
     */
    boolean containsBeanContainer(Scope scope);
}
