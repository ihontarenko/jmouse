package svit.beans;

import svit.beans.naming.BeanNameResolver;
import svit.beans.definition.BeanDefinitionFactory;
import svit.beans.processor.BeanPostProcessorAware;

import java.util.List;


/**
 * Represents the core interface for managing the bean lifecycle, definitions, containers,
 * and scope resolution within the application context.
 *
 * @see BeanContainer
 * @see BeanContainerRegistry
 * @see BeanDefinitionContainer
 * @see BeanInitializer
 * @see BeanPostProcessorAware
 */
public interface BeanContext extends BeanContainer, BeanContainerRegistry,
        BeanDefinitionContainer, BeanInitializer, BeanPostProcessorAware {

    /**
     * Sets the base classes to be scanned and processed by this context.
     * <p>
     * These classes are used to detect annotations, definitions, and additional context information
     * necessary for bean registration and initialization.
     * </p>
     *
     * @param baseClasses the array of base classes to be set.
     */
    void setBaseClasses(Class<?>... baseClasses);

    /**
     * Retrieves the base classes that are currently being used by this context.
     *
     * @return an array of base classes that the context is working with.
     */
    Class<?>[] getBaseClasses();

    /**
     * Retrieves the names of all beans that match the specified type.
     *
     * @param type the class type of the beans.
     * @return a list of bean names that match the given type.
     */
    List<String> getBeanNames(Class<?> type);

    /**
     * Retrieves all beans that match the specified type.
     *
     * @param type the class type of the beans.
     * @param <T>  the type of the beans.
     * @return a list of beans matching the given type.
     */
    <T> List<T> getBeans(Class<T> type);

    /**
     * Retrieves the current {@link BeanFactory}.
     *
     * @return the associated bean factory.
     */
    BeanFactory getBeanFactory();

    /**
     * Retrieves the current {@link BeanDefinitionFactory}.
     *
     * @return the associated bean definition factory.
     */
    BeanDefinitionFactory getBeanDefinitionFactory();

    /**
     * Sets the {@link BeanDefinitionFactory} for this context.
     *
     * @param definitionFactory the bean definition factory to set.
     */
    void setBeanDefinitionFactory(BeanDefinitionFactory definitionFactory);

    /**
     * Retrieves the current {@link BeanNameResolver}.
     *
     * @return the associated bean name resolver.
     */
    BeanNameResolver getNameResolver();

    /**
     * Sets the {@link BeanFactory} for this context.
     *
     * @param beanFactory the bean factory to set.
     */
    void setBeanFactory(BeanFactory beanFactory);

    /**
     * Sets the {@link BeanNameResolver} for this context.
     *
     * @param nameResolver the bean name resolver to set.
     */
    void setNameResolver(BeanNameResolver nameResolver);

    /**
     * Retrieves the {@link BeanContainerRegistry} associated with this context.
     *
     * @return the current {@link BeanContainerRegistry} used by this context.
     * @throws IllegalStateException if the registry is not set.
     */
    BeanContainerRegistry getBeanContainerRegistry();

    /**
     * Sets the {@link BeanContainerRegistry} for this context.
     *
     * @param containerRegistry the {@link BeanContainerRegistry} to set.
     *                          Must not be {@code null}.
     * @throws NullPointerException if the provided {@code containerRegistry} is {@code null}.
     */
    void setBeanContainerRegistry(BeanContainerRegistry containerRegistry);

    /**
     * Sets the parent context for this bean context.
     *
     * @param parent the parent {@link BeanContext}.
     */
    void setParentContext(BeanContext parent);

    /**
     * Retrieves the parent context of this bean context.
     *
     * @return the parent {@link BeanContext}.
     */
    BeanContext getParentContext();

    /**
     * Gets the unique identifier of this context.
     *
     * @return the context ID as a {@link String}.
     */
    String getContextId();

    /**
     * Sets the unique identifier for this context.
     *
     * @param contextId the context ID to set.
     */
    void setContextId(String contextId);

    /**
     * Adds a {@link BeanContextInitializer} to this context.
     *
     * @param initializer the initializer to add.
     */
    void addInitializer(BeanContextInitializer initializer);

    /**
     * Executes all registered {@link BeanContextInitializer}s to initialize the context.
     * <p>
     * If an initializer has already been executed (tracked via the {@code initialized} set),
     * it will be skipped to prevent duplicate initialization.
     */
    void refresh();

    /**
     * Clears all tracked initializations, allowing the registered {@link BeanContextInitializer}s
     * to be executed again.
     * <p>
     * This method is useful when the context needs to be reinitialized from scratch.
     * <p>
     */
    void cleanup();
}
