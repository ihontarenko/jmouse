package svit.beans;

import svit.beans.naming.BeanNameResolver;
import svit.beans.definition.BeanDefinitionFactory;
import svit.beans.processor.BeanPostProcessorAware;

/**
 * Interface representing a bean context in the container framework.
 * Provides methods to manage bean factories, definitions, and initializers.
 */
public interface BeanContext
        extends BeanContainer, BeanInstanceContainerRegistry, BeanDefinitionContainer, BeanInitializer, BeanPostProcessorAware {

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
