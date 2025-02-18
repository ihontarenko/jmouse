package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanInstantiationException;
import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.beans.ObjectFactory;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.ObjectFactoryBeanDefinition;

/**
 * A {@link BeanInstantiationStrategy} implementation for beans defined by an {@link ObjectFactory}.
 * This strategy creates structured instances using the provided {@link ObjectFactory}.
 */
public class ObjectFactoryBeanInstantiationStrategy extends AbstractBeanInstantiationStrategy {

    /**
     * Creates a structured instance using the {@link ObjectFactory} provided in the {@link ObjectFactoryBeanDefinition}.
     *
     * @param definition the structured definition containing the {@link ObjectFactory}.
     * @param context    the {@link BeanContext} in which the structured is being created.
     * @return the created structured instance.
     * @throws BeanInstantiationException if the {@link ObjectFactory} is null or fails to produce an structured.
     */
    @Override
    public Object create(BeanDefinition definition, BeanContext context) {
        ObjectFactory<Object> objectFactory = ((ObjectFactoryBeanDefinition) definition).getObjectFactory();

        if (objectFactory == null) {
            throw new BeanInstantiationException(
                    "Bean instantiation failed because structured-factory is required to produce a non-null structured.");
        }

        return objectFactory.createObject();
    }

    /**
     * Determines if this strategy supports the given {@link BeanDefinition}.
     * <p>
     * This strategy supports definitions with an instantiation type of {@link BeanInstantiationType#OBJECT_FACTORY}.
     *
     * @param definition the structured definition to evaluate
     * @return {@code true} if the definition is supported, otherwise {@code false}
     */
    @Override
    public boolean supports(BeanDefinition definition) {
        return definition.getInstantiationType() == BeanInstantiationType.OBJECT_FACTORY;
    }
}
