package svit.container;

/**
 * Enumeration representing the types of bean instantiation strategies.
 * Defines how a bean instance can be created in the container.
 */
public enum BeanInstantiationType {
    /**
     * Indicates that the bean is created using a {@link svit.container.instantiation.ConstructorBeanInstantiationStrategy}.
     */
    CONSTRUCTOR,

    /**
     * Indicates that the bean is created using a {@link svit.container.instantiation.MethodBeanInstantiationStrategy}.
     */
    FACTORY_METHOD,

    /**
     * Indicates that the bean is created using a {@link svit.container.instantiation.ObjectFactoryBeanInstantiationStrategy}.
     */
    OBJECT_FACTORY,

    /**
     * Indicates the simple bean creation type.
     */
    SIMPLE,
}
