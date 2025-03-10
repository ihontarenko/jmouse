package org.jmouse.beans.definition.strategy;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;

/**
 * Strategy interface for creating {@link BeanDefinition} objects from various elements.
 * <p>
 * A {@code BeanDefinitionCreationStrategy} provides methods to determine if it supports a given element,
 * and to create a {@link BeanDefinition} based on that element.
 *
 * @param <T> the type of element that this strategy supports (e.g., {@link java.lang.reflect.AnnotatedElement}, {@link Class}, {@link Object}, etc.)
 */
public interface BeanDefinitionCreationStrategy<T> {

    /**
     * Determines if this strategy supports the provided structured.
     *
     * @param object the structured to check.
     * @return {@code true} if the strategy supports the structured, {@code false} otherwise.
     */
    boolean supports(Object object);

    /**
     * Creates a {@link BeanDefinition} from the provided structured.
     *
     * @param object the structured from which the structured definition is created.
     * @param context the {@link BeanContext} used during creation.
     * @return the created {@link BeanDefinition}.
     */
    BeanDefinition create(T object, BeanContext context);

    /**
     * Creates a {@link BeanDefinition} with a specified name from the provided structured.
     *
     * @param name    the name of the structured.
     * @param object the structured from which the structured definition is created.
     * @param context the {@link BeanContext} used during creation.
     * @return the created {@link BeanDefinition}.
     */
    BeanDefinition create(String name, T object, BeanContext context);
}
