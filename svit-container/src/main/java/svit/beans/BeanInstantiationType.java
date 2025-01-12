package svit.beans;

/**
 * Enumeration representing the types of bean instantiation strategies.
 * Defines how a bean instance can be created in the container.
 */
public enum BeanInstantiationType {
    /**
     * Indicates that the bean is created using a {@link svit.beans.instantiation.ConstructorBeanInstantiationStrategy}.
     */
    CONSTRUCTOR,

    /**
     * Indicates that the bean is created using a {@link svit.beans.instantiation.MethodBeanInstantiationStrategy}.
     */
    FACTORY_METHOD,

    /**
     * Indicates that the bean is created using a {@link svit.beans.instantiation.ObjectFactoryBeanInstantiationStrategy}.
     */
    OBJECT_FACTORY,

    /**
     * Indicates the simple bean creation type.
     */
    SIMPLE,
}
