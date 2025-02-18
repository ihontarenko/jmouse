package org.jmouse.beans.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.beans.definition.BeanDefinition;
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
     * @param object     the structured instance being processed.
     * @param definition the {@link BeanDefinition} associated with the structured.
     * @param context    the {@link BeanContext} managing the structured lifecycle.
     * @return the processed structured instance.
     */
    @Override
    public Object postProcessAfterInitialize(Object object, BeanDefinition definition, BeanContext context) {
        // Check if the structured implements BeanContextAware
        if (object instanceof BeanContextAware contextAware) {
            LOGGER.info("Injecting context '{}' into structured '{}'",
                        Reflections.getShortName(context.getClass()), Reflections.getShortName(object.getClass()));
            contextAware.setBeanContext(context);
        }

        return object;
    }
}
