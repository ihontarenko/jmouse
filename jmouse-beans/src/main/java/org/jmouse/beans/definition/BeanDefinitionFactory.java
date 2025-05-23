package org.jmouse.beans.definition;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.strategy.BeanDefinitionCreationStrategy;

/**
 * Factory interface for creating {@link BeanDefinition} instances.
 * This interface defines the contract for managing the creation of bean definitions
 * and supports a pluggable strategy-based approach for custom definition creation logic.
 */
public interface BeanDefinitionFactory {

    /**
     * Creates a {@link BeanDefinition} for the given bean using the specified name and context.
     * <p>
     * If the preferred name is {@code null}, a default name will be resolved or generated.
     * The creation process relies on registered {@link BeanDefinitionCreationStrategy} instances.
     * </p>
     *
     * @param preferredName the preferred name for the bean definition (can be {@code null}).
     * @param object        the bean for which the bean definition is being created.
     * @param context       the {@link BeanContext} used to provide additional context for the creation process.
     * @return the created {@link BeanDefinition}.
     * @throws IllegalArgumentException if no suitable strategy is found for the given bean.
     */
    BeanDefinition createDefinition(String preferredName, Object object, BeanContext context);

    /**
     * Creates a {@link BeanDefinition} for the given bean using the default name resolution mechanism.
     * <p>
     * This method delegates to {@link #createDefinition(String, Object, BeanContext)} with a {@code null} name.
     * </p>
     *
     * @param object  the bean for which the bean definition is being created.
     * @param context the {@link BeanContext} used to provide additional context for the creation process.
     * @return the created {@link BeanDefinition}.
     * @throws IllegalArgumentException if no suitable strategy is found for the given bean.
     */
    default BeanDefinition createDefinition(Object object, BeanContext context) {
        return createDefinition(null, object, context);
    }

    /**
     * Adds a new {@link BeanDefinitionCreationStrategy} to the factory.
     * <p>
     * Strategies are used to determine how a bean definition is created for a given bean.
     * Strategies are evaluated in the order they are added to the factory.
     * </p>
     *
     * @param strategy the {@link BeanDefinitionCreationStrategy} to add.
     * @throws NullPointerException if the strategy is {@code null}.
     */
    void addStrategy(BeanDefinitionCreationStrategy<?> strategy);

}
