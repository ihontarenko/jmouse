package svit.beans;

/**
 * Interface for managing access to a {@link BeanDefinitionContainer}.
 * <p>
 * This interface provides methods to set and retrieve a {@link BeanDefinitionContainer},
 * which holds and manages {@link svit.beans.definition.BeanDefinition} objects.
 * </p>
 */
public interface BeanDefinitionContainerRegistry {

    /**
     * Sets the {@link BeanDefinitionContainer} to be used by the registry.
     *
     * @param definitionContainer the {@link BeanDefinitionContainer} to set.
     * @throws IllegalArgumentException if the provided container is {@code null}.
     */
    void setBeanDefinitionContainer(BeanDefinitionContainer definitionContainer);

    /**
     * Retrieves the current {@link BeanDefinitionContainer}.
     *
     * @return the {@link BeanDefinitionContainer} used by the registry.
     * @throws IllegalStateException if no container has been set.
     */
    BeanDefinitionContainer getBeanDefinitionContainer();
}
