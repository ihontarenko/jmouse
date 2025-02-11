package org.jmouse.testing_ground.beans;

import org.jmouse.testing_ground.beans.instantiation.ConstructorBeanInstantiationStrategy;
import org.jmouse.testing_ground.beans.instantiation.MethodBeanInstantiationStrategy;
import org.jmouse.testing_ground.beans.instantiation.ObjectFactoryBeanInstantiationStrategy;

/**
 * Enumeration representing the types of bean instantiation strategies.
 * Defines how a bean instance can be created in the container.
 */
public enum BeanInstantiationType {
    /**
     * Indicates that the bean is created using a {@link ConstructorBeanInstantiationStrategy}.
     */
    CONSTRUCTOR,

    /**
     * Indicates that the bean is created using a {@link MethodBeanInstantiationStrategy}.
     */
    FACTORY_METHOD,

    /**
     * Indicates that the bean is created using a {@link ObjectFactoryBeanInstantiationStrategy}.
     */
    OBJECT_FACTORY,

    /**
     * Indicates the simple bean creation type.
     */
    SIMPLE,
}
