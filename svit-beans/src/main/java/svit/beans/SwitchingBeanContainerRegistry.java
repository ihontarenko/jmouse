package svit.beans;

public class SwitchingBeanContainerRegistry implements BeanContainerRegistry {

    private final ObjectFactory<BeanContainerRegistry> registryCreator;


    public SwitchingBeanContainerRegistry(ObjectFactory<BeanContainerRegistry> registryCreator) {
        this.registryCreator = registryCreator;
    }

    /**
     * Retrieves the {@link BeanContainer} associated with the specified {@link Scope}.
     *
     * @param scope the scope of the desired bean container.
     * @return the corresponding bean container, or {@code null} if none is registered for the given scope.
     * @throws IllegalArgumentException if the scope is {@code null}.
     */
    @Override
    public BeanContainer getBeanContainer(Scope scope) {
        return null;
    }

    /**
     * Registers a {@link BeanContainer} for the specified {@link Scope}.
     *
     * @param scope     the scope of the bean container to register.
     * @param container the bean container to associate with the given scope.
     * @throws IllegalArgumentException if the scope or container is {@code null}.
     */
    @Override
    public void registerBeanContainer(Scope scope, BeanContainer container) {

    }

    /**
     * Removes all registered {@link BeanContainer}s from the registry.
     * <p>
     * This method clears all scope-to-container mappings, effectively resetting the registry.
     */
    @Override
    public void removeBeanInstanceContainers() {

    }

    /**
     * Checks if a {@link BeanContainer} is registered for the specified {@link Scope}.
     *
     * @param scope the scope to check for a registered bean container.
     * @return {@code true} if a container is registered for the given scope, {@code false} otherwise.
     * @throws IllegalArgumentException if the scope is {@code null}.
     */
    @Override
    public boolean containsBeanContainer(Scope scope) {
        return false;
    }

}
