package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;

/**
 * A factory interface for creating bean instances using various {@link BeanInstantiationStrategy} implementations.
 * <p>
 * Classes implementing this interface can register multiple instantiation strategies and
 * delegate the creation of a bean to the strategy that supports a given {@link BeanDefinition}.
 */
public interface BeanInstantiationFactory {

    /**
     * Adds a new {@link BeanInstantiationStrategy} to the factory.
     *
     * @param strategy the strategy to add
     */
    void addStrategy(BeanInstantiationStrategy strategy);

    /**
     * Creates an instance of a bean based on the provided {@link BeanDefinition} and {@link BeanContext},
     * using one of the registered instantiation strategies.
     *
     * @param definition the bean definition containing metadata for creating the bean
     * @param context    the context in which the bean is to be created
     * @return the instantiated bean object, or {@code null} if no registered strategy supports the definition
     */
    Object createInstance(BeanDefinition definition, BeanContext context);
}
