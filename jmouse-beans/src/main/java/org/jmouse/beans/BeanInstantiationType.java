package org.jmouse.beans;

import org.jmouse.beans.instantiation.ConstructorBeanInstantiationStrategy;
import org.jmouse.beans.instantiation.MethodBeanInstantiationStrategy;
import org.jmouse.beans.instantiation.ObjectFactoryBeanInstantiationStrategy;

/**
 * Enumeration representing the types of structured instantiation strategies.
 * Defines how a structured instance can be created in the container.
 */
public enum BeanInstantiationType {
    /**
     * Indicates that the structured is created using a {@link ConstructorBeanInstantiationStrategy}.
     */
    CONSTRUCTOR,

    /**
     * Indicates that the structured is created using a {@link MethodBeanInstantiationStrategy}.
     */
    FACTORY_METHOD,

    /**
     * Indicates that the structured is created using a {@link ObjectFactoryBeanInstantiationStrategy}.
     */
    OBJECT_FACTORY,

    /**
     * Indicates the simple structured creation type.
     */
    SIMPLE,
}
