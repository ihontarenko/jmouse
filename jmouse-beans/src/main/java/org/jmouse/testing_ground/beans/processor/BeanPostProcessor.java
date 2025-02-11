package org.jmouse.testing_ground.beans.processor;

import org.jmouse.testing_ground.beans.BeanContext;
import org.jmouse.testing_ground.beans.definition.BeanDefinition;

/**
 * Interface for performing custom operations on beans during their initialization lifecycle.
 * <p>
 * Implementations of this interface can define logic to be executed before and after
 * a bean is initialized in the {@link BeanContext}. This is useful for tasks like
 * dependency injection, proxy creation, or any additional initialization logic.
 * </p>
 */
public interface BeanPostProcessor {

    /**
     * Performs operations before a bean is initialized.
     *
     * @param bean       the bean instance being processed.
     * @param definition the {@link BeanDefinition} associated with the bean.
     * @param context    the {@link BeanContext} managing the bean lifecycle.
     * @return the processed bean instance, which may be the original bean or a wrapped/proxied instance.
     */
    default Object postProcessBeforeInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        return bean;
    }

    /**
     * Performs operations after a bean is initialized.
     *
     * @param bean       the bean instance being processed.
     * @param definition the {@link BeanDefinition} associated with the bean.
     * @param context    the {@link BeanContext} managing the bean lifecycle.
     * @return the processed bean instance, which may be the original bean or a wrapped/proxied instance.
     */
    default Object postProcessAfterInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        return bean;
    }
}
