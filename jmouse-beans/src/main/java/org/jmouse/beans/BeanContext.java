package org.jmouse.beans;

import org.jmouse.beans.naming.BeanNameResolver;
import org.jmouse.beans.definition.BeanDefinitionFactory;
import org.jmouse.beans.processor.BeanPostProcessorAware;

import java.util.List;
import java.util.Map;


/**
 * 🎯 Represents the central parser and registry for bean lifecycle in the application context.
 * <p>
 * Combines multiple responsibilities:
 * <ul>
 *     <li>📦 Managing bean containers and scopes</li>
 *     <li>🛠 Registering and initializing bean definitions</li>
 *     <li>🔁 Applying bean post-processors</li>
 *     <li>🎯 Resolving beans by name and type</li>
 * </ul>
 *
 * @see BeanContainer           for low-level access to scoped beans
 * @see BeanContainerRegistry  for managing multiple containers
 * @see BeanDefinitionContainer for tracking bean definitions
 * @see BeanInitializer        for creating and injecting beans
 * @see BeanPostProcessorAware for post-processing support
 */
public interface BeanContext extends BeanContainer, BeanContainerRegistry,
        BeanDefinitionContainer, BeanInitializer, BeanPostProcessorAware {

    /**
     * 📌 Defines how this context will behave when a bean is not found locally.
     * <p>Default strategy is {@code DELEGATE_TO_PARENT} — try parent context first.
     *
     * @return the configured lookup strategy
     */
    default BeanLookupStrategy getBeanLookupStrategy() {
        return BeanLookupStrategy.DELEGATE_TO_PARENT;
    }

    /**
     * ⚙️ Set lookup strategy for missing beans.
     * <p>Defines how this context behaves when a bean is not found locally.
     *
     * @param beanLookupStrategy the strategy to apply (e.g. inherit or delegate)
     */
    void setBeanLookupStrategy(BeanLookupStrategy beanLookupStrategy);

    /**
     * Adds the base classes to be scanned and processed by this context.
     *
     * @param baseClasses the array of base classes to be set.
     */
    void addBaseClasses(Class<?>... baseClasses);

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
     * Checks whether a bean with the given name is defined **locally** in this context,
     * excluding any parent contexts.
     *
     * @param name the name of the bean
     * @return {@code true} if the bean exists in this context only; {@code false} if it's inherited or absent
     */
    boolean isLocalBean(String name);

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
     * Retrieves a map of bean names to their instances, matching the specified type.
     *
     * @param type the class type of the beans
     * @param <T>  the type of the beans
     * @return a map containing bean names as keys and their corresponding bean instances as values
     */
    <T> Map<String, T> getBeansOfType(Class<T> type);

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
