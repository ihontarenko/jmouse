package svit.container.instantiation;

import svit.container.BeanContext;
import svit.container.BeanInstantiationException;
import svit.container.BeanInstantiationType;
import svit.container.ObjectFactory;
import svit.container.definition.BeanDefinition;
import svit.container.definition.ObjectFactoryBeanDefinition;

/**
 * A {@link BeanInstantiationStrategy} implementation for beans defined by an {@link ObjectFactory}.
 * This strategy creates bean instances using the provided {@link ObjectFactory}.
 */
public class ObjectFactoryBeanInstantiationStrategy extends AbstractBeanInstantiationStrategy {

    /**
     * Creates a bean instance using the {@link ObjectFactory} provided in the {@link ObjectFactoryBeanDefinition}.
     *
     * @param definition the bean definition containing the {@link ObjectFactory}.
     * @param context    the {@link BeanContext} in which the bean is being created.
     * @return the created bean instance.
     * @throws BeanInstantiationException if the {@link ObjectFactory} is null or fails to produce an object.
     */
    @Override
    public Object create(BeanDefinition definition, BeanContext context) {
        ObjectFactory<Object> objectFactory = ((ObjectFactoryBeanDefinition) definition).getObjectFactory();

        if (objectFactory == null) {
            throw new BeanInstantiationException(
                    "Bean instantiation failed because object-factory is required to produce a non-null object.");
        }

        return objectFactory.createObject();
    }

    /**
     * Determines if this strategy supports the given {@link BeanDefinition}.
     * <p>
     * This strategy supports definitions with an instantiation type of {@link BeanInstantiationType#OBJECT_FACTORY}.
     *
     * @param definition the bean definition to evaluate
     * @return {@code true} if the definition is supported, otherwise {@code false}
     */
    @Override
    public boolean supports(BeanDefinition definition) {
        return definition.getInstantiationType() == BeanInstantiationType.OBJECT_FACTORY;
    }
}
