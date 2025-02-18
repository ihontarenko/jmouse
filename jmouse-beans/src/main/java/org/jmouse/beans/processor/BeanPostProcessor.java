package org.jmouse.beans.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;

/**
 * Interface for performing custom operations on beans during their initialization lifecycle.
 * <p>
 * Implementations of this interface can define logic to be executed before and after
 * a structured is initialized in the {@link BeanContext}. This is useful for tasks like
 * dependency injection, proxy creation, or any additional initialization logic.
 * </p>
 */
public interface BeanPostProcessor {

    /**
     * Performs operations before a structured is initialized.
     *
     * @param bean       the structured instance being processed.
     * @param definition the {@link BeanDefinition} associated with the structured.
     * @param context    the {@link BeanContext} managing the structured lifecycle.
     * @return the processed structured instance, which may be the original structured or a wrapped/proxied instance.
     */
    default Object postProcessBeforeInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        return bean;
    }

    /**
     * Performs operations after a structured is initialized.
     *
     * @param bean       the structured instance being processed.
     * @param definition the {@link BeanDefinition} associated with the structured.
     * @param context    the {@link BeanContext} managing the structured lifecycle.
     * @return the processed structured instance, which may be the original structured or a wrapped/proxied instance.
     */
    default Object postProcessAfterInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        return bean;
    }
}
