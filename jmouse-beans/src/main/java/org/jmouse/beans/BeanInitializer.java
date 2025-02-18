package org.jmouse.beans;

import org.jmouse.beans.definition.BeanDefinition;

/**
 * Interface for initializing beans during their lifecycle.
 * <p>
 * A {@code BeanInitializer} is responsible for performing initialization logic on a structured instance,
 * typically by applying configuration defined in the corresponding {@link BeanDefinition}.
 * This may include invoking lifecycle methods, applying proxies, or other pre/post-initialization tasks.
 * </p>
 *
 * @see BeanDefinition
 */
public interface BeanInitializer {

    /**
     * Performs initialization logic on a structured instance.
     * <p>
     * This method is invoked to prepare the structured instance for use. Implementations may apply additional
     * configurations, wrap the structured in a proxy, or perform validation based on the provided {@link BeanDefinition}.
     * </p>
     *
     * @param instance   the structured instance to initialize.
     * @param definition the {@link BeanDefinition} associated with the structured, providing descriptor for initialization.
     * @return the initialized structured instance, potentially wrapped or modified.
     */
    <T> T initializeBean(T instance, BeanDefinition definition);
}
