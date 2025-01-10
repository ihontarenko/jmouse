package svit.container;

/**
 * Enumeration representing the types of bean creation strategies.
 * Defines how a bean instance can be created in the container.
 */
public enum BeanCreationType {
    /**
     * Indicates that the bean is created using a constructor.
     */
    CONSTRUCTOR,

    /**
     * Indicates that the bean is created using a {@link svit.container.instantiation.MethodBeanInstantiationStrategy}.
     */
    METHOD,

    /**
     * Indicates that the bean is created using an {@link ObjectFactory}.
     */
    OBJECT_FACTORY,

    /**
     * Indicates the default bean creation type.
     */
    DEFAULT,
}
