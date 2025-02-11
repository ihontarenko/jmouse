package org.jmouse.testing_ground.beans.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.testing_ground.beans.BeanContext;
import org.jmouse.testing_ground.beans.BeanContextAware;
import org.jmouse.testing_ground.beans.definition.BeanDefinition;
import org.jmouse.core.reflection.Reflections;

/**
 * A {@link BeanPostProcessor} implementation that injects the {@link BeanContext}
 * into beans implementing {@link BeanContextAware}.
 * <p>
 * This processor automatically sets the context for beans that require it
 * during their initialization phase.
 * </p>
 *
 * @see BeanContextAware
 */
public class BeanContextAwareBeanPostProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanContextAwareBeanPostProcessor.class);

    /**
     * Injects the {@link BeanContext} into beans implementing {@link BeanContextAware}
     * after their initialization.
     *
     * @param object     the bean instance being processed.
     * @param definition the {@link BeanDefinition} associated with the bean.
     * @param context    the {@link BeanContext} managing the bean lifecycle.
     * @return the processed bean instance.
     */
    @Override
    public Object postProcessAfterInitialize(Object object, BeanDefinition definition, BeanContext context) {
        // Check if the bean implements BeanContextAware
        if (object instanceof BeanContextAware contextAware) {
            LOGGER.info("Injecting context '{}' into bean '{}'",
                        Reflections.getShortName(context.getClass()), Reflections.getShortName(object.getClass()));
            contextAware.setBeanContext(context);
        }

        return object;
    }
}
