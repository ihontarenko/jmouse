package svit.container.definition;

import svit.container.BeanInstantiationType;
import svit.container.BeanScope;
import svit.container.ObjectFactory;

/**
 * A {@link BeanDefinition} implementation for beans created using an {@link ObjectFactory}.
 * Stores the factory and metadata about the bean.
 */
public class ObjectFactoryBeanDefinition extends AbstractBeanDefinition {

    private final ObjectFactory<Object> objectFactory;

    /**
     * Constructs an {@link ObjectFactoryBeanDefinition}.
     *
     * @param name          the name of the bean.
     * @param type          the type of the bean.
     * @param objectFactory the factory responsible for creating the bean.
     */
    public ObjectFactoryBeanDefinition(String name, Class<?> type, ObjectFactory<Object> objectFactory) {
        super(name, type);

        this.objectFactory = objectFactory;
        this.beanScope = BeanScope.PROTOTYPE;
    }

    /**
     * Retrieves the creation type for this bean definition.
     *
     * @return {@link BeanInstantiationType#OBJECT_FACTORY}.
     */
    @Override
    public BeanInstantiationType getInstantiationType() {
        return BeanInstantiationType.OBJECT_FACTORY;
    }

    /**
     * Retrieves the {@link ObjectFactory} associated with this bean definition.
     *
     * @return the object factory.
     */
    public ObjectFactory<Object> getObjectFactory() {
        return objectFactory;
    }

    /**
     * Returns a string representation of this bean definition.
     *
     * @return a string representation including the name and type of the bean.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("OBJECT_FACTORY_BEAN_DEFINITION: ");

        builder.append(super.toString());

        return builder.toString();
    }

}
