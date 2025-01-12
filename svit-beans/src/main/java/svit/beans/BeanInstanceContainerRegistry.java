package svit.beans;

/**
 * Registry for managing {@link BeanInstanceContainer}s associated with different {@link BeanScope}s.
 * <p>
 * This interface provides methods for retrieving, registering, and removing bean instance containers
 * based on their associated scope.
 * </p>
 */
public interface BeanInstanceContainerRegistry {

    /**
     * Retrieves the {@link BeanInstanceContainer} associated with the specified {@link BeanScope}.
     *
     * @param scope the {@link BeanScope} for which to retrieve the container.
     * @return the {@link BeanInstanceContainer} associated with the provided scope,
     *         or {@code null} if no container is registered for the scope.
     */
    BeanInstanceContainer getBeanInstanceContainer(BeanScope scope);

    /**
     * Registers a {@link BeanInstanceContainer} for a specific {@link Scope}.
     * <p>
     * Associates the given container with the provided scope, enabling
     * management of beans within that scope.
     * </p>
     *
     * @param scope     the {@link Scope} for which the container is being registered.
     * @param container the {@link BeanInstanceContainer} to associate with the scope.
     */
    void registerBeanInstanceContainer(Scope scope, BeanInstanceContainer container);

    /**
     * Removes all registered {@link BeanInstanceContainer}s.
     * <p>
     * Clears all previously registered containers, effectively resetting the registry.
     * </p>
     */
    void removeBeanInstanceContainers();
}
