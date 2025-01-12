package svit.container;

import svit.container.naming.BeanNameResolver;
import svit.container.definition.BeanDefinitionFactory;
import svit.container.processor.BeanPostProcessorAware;

/**
 * Interface representing a bean context in the container framework.
 * Provides methods to manage bean factories, definitions, and initializers.
 */
public interface BeanContext
        extends BeanContainer, BeanInstanceContainer, BeanDefinitionContainer, BeanPostProcessorAware {

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
     * Sets the {@link BeanDefinitionFactory} for this context.
     *
     * @param definitionFactory the bean definition factory to set.
     */
    void setBeanDefinitionFactory(BeanDefinitionFactory definitionFactory);

    /**
     * Sets the {@link BeanNameResolver} for this context.
     *
     * @param nameResolver the bean name resolver to set.
     */
    void setNameResolver(BeanNameResolver nameResolver);

    /**
     * Sets the parent context for this bean context.
     *
     * @param parent the parent {@link BeanContext}.
     */
    void setParentContext(BeanContext parent);

    /**
     * Retrieves the {@link BeanInstanceContainer} for the specified lifecycle scope.
     *
     * @param lifecycle the lifecycle scope for which to retrieve the container
     * @return the {@link BeanInstanceContainer} associated with the given lifecycle
     */
    BeanInstanceContainer getBeanInstanceContainer(BeanScope scope);

    /**
     * Registers a {@link BeanInstanceContainer} for a specific {@link Scope}.
     * <p>
     * This method allows mapping a scope to a container that manages bean instances
     * within that scope. For example, you can register separate containers for
     * singleton, prototype, request, or session scopes.
     * </p>
     *
     * @param scope    the {@link Scope} for which the container is being registered.
     * @param container the {@link BeanInstanceContainer} to be associated with the given scope.
     * @throws IllegalArgumentException if the provided scope or container is null.
     */
    void registerBeanInstanceContainer(Scope scope, BeanInstanceContainer container);

    /**
     * Retrieves the parent context of this bean context.
     *
     * @return the parent {@link BeanContext}.
     */
    BeanContext getParentContext();

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
