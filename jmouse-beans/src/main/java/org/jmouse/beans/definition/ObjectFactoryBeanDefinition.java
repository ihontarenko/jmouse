package org.jmouse.beans.definition;

import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.ObjectFactory;

import java.lang.reflect.AnnotatedElement;

/**
 * A {@link BeanDefinition} implementation for beans created using an {@link ObjectFactory}.
 * Stores the factory and descriptor about the bean.
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
        this.scope = BeanScope.PROTOTYPE;
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
     * @return the bean factory.
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

    /**
     * 🧠 Underlying annotated element (e.g. class, method).
     *
     * @return annotated element
     */
    @Override
    public AnnotatedElement getAnnotatedElement() {
        throw new UnsupportedOperationException("Not supported for ObjectFactoryBeanDefinition");
    }
}
