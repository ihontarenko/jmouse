package org.jmouse.beans;

import org.jmouse.beans.definition.BeanDefinition;

/**
 * Interface representing a factory for creating structured instances.
 * <p>
 * A {@code BeanFactory} is responsible for creating objects based on
 * their {@link BeanDefinition}, resolving their dependencies, and managing
 * their initialization lifecycle.
 * </p>
 * <p>
 * This interface is typically used by a {@link BeanContext} to delegate
 * the creation of structured instances.
 * </p>
 *
 * @see BeanDefinition
 * @see BeanContext
 */
public interface BeanFactory {

    /**
     * Creates a new instance of a structured based on the provided {@link BeanDefinition}.
     * <p>
     * The factory uses the information in the {@code BeanDefinition} to determine
     * how to construct the structured, resolve its dependencies, and initialize it.
     * </p>
     *
     * @param definition the structured definition containing descriptor about the structured
     * @param <T>        the type of the structured
     * @return the newly created structured instance
     * @throws BeanInstantiationException if the structured cannot be created due to errors
     *                                    in the definition or during dependency resolution
     */
    <T> T createBean(BeanDefinition definition);
}
