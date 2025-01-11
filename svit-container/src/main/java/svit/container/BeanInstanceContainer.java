package svit.container;

import svit.reflection.Reflections;

/**
 * Interface for managing bean instances in a container.
 * Provides methods to retrieve and register beans by name, with support for lazy initialization
 * and optional scoping.
 * <p>
 * Example usage:
 * <pre>{@code
 * BeanInstanceContainer container = new MyBeanContainer();
 *
 * // Retrieve a bean or create it if missing
 * User user = container.getBean("user", User::new);
 *
 * // Check if a bean exists
 * if (!container.containsBean("userService")) {
 *     container.registerBean("userService", new UserService());
 * }
 * }</pre>
 */
public interface BeanInstanceContainer {

    /**
     * Retrieves a bean by its name, creating and registering it using the provided {@link ObjectFactory}
     * if it does not already exist.
     *
     * @param name          the name of the bean.
     * @param objectFactory the factory to create the bean if it is not already present.
     * @param <T>           the type of the bean.
     * @return the existing or newly created bean instance.
     */
    default <T> T getBean(String name, ObjectFactory<T> objectFactory) {
        T bean = getBean(name);

        if (bean == null) {
            bean = objectFactory.createObject();
            registerBean(name, bean);
        }

        return bean;
    }

    /**
     * Retrieves a bean instance by its name.
     *
     * @param name the name of the bean to retrieve.
     * @param <T>  the type of the bean.
     * @return the bean instance, or {@code null} if no bean is found with the given name.
     */
    <T> T getBean(String name);

    /**
     * Registers a bean instance with the given name.
     *
     * @param name the name of the bean.
     * @param bean the bean instance to register.
     */
    void registerBean(String name, Object bean);

    /**
     * Registers a bean instance with the given name and a specified {@link BeanScope}.
     * <p>
     * The default implementation throws a {@link BeanContextException} to indicate that
     * scope-based registration is not supported. Override this method in a subclass
     * if scope-based registration is required.
     *
     * @param name      the name of the bean.
     * @param bean      the bean instance to register.
     * @param beanScope the scope of the bean.
     * @throws BeanContextException if this operation is not supported.
     */
    default void registerBean(String name, Object bean, BeanScope beanScope) {
        throw new BeanContextException(
                "The registration of a bean is prohibited in the '%s' implementation."
                        .formatted(Reflections.getShortName(getClass())));
    }

    /**
     * Checks whether a bean with the specified name is already registered in this container.
     *
     * @param name the name of the bean.
     * @return {@code true} if a bean with the given name exists, otherwise {@code false}.
     */
    default boolean containsBean(String name) {
        return false;
    }
}
