package svit.beans;

/**
 * Registry for managing {@link BeanContainer}s associated with different {@link BeanScope}s.
 * <p>
 * This interface provides methods for retrieving, registering, and removing bean instance containers
 * based on their associated scope.
 * </p>
 */
public interface BeanContainerRegistry {

    /**
     * Retrieves the {@link BeanContainer} associated with the specified {@link Scope}.
     *
     * @param scope the {@link Scope} for which to retrieve the container.
     * @return the {@link BeanContainer} associated with the provided scope,
     *         or {@code null} if no container is registered for the scope.
     */
    BeanContainer getBeanContainer(Scope scope);

    /**
     * Registers a {@link BeanContainer} for a specific {@link Scope}.
     * <p>
     * Associates the given container with the provided scope, enabling
     * management of beans within that scope.
     * </p>
     *
     * @param scope     the {@link Scope} for which the container is being registered.
     * @param container the {@link BeanContainer} to associate with the scope.
     */
    void registerBeanContainer(Scope scope, BeanContainer container);

    /**
     * Removes all registered {@link BeanContainer}s.
     * <p>
     * Clears all previously registered containers, effectively resetting the registry.
     * </p>
     */
    void removeBeanInstanceContainers();
}
