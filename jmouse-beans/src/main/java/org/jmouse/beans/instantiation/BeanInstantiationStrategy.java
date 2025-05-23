package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;

/**
 * Interface representing a strategy for instantiating beans in a container.
 * Provides methods to create a bean instance and determine if the strategy supports a given {@link BeanDefinition}.
 */
public interface BeanInstantiationStrategy {

    /**
     * Creates a new instance of a bean based on the provided {@link BeanDefinition} and {@link BeanContext}.
     *
     * @param definition the bean definition containing descriptor about the bean.
     * @param context    the context in which the bean is being created.
     * @return the newly created bean instance.
     */
    Object create(BeanDefinition definition, BeanContext context);

    /**
     * Determines if this strategy supports the given {@link BeanDefinition}.
     *
     * @param definition the bean definition to check.
     * @return {@code true} if the strategy supports the definition, {@code false} otherwise.
     */
    boolean supports(BeanDefinition definition);
}
