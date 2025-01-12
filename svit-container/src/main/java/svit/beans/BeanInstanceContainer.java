package svit.beans;

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
     * <p>
     * If the factory produces a {@code null} object, a {@link BeanContextException} is thrown.
     *
     * @param name          the name of the bean to retrieve or create.
     * @param objectFactory the factory to create the bean if it does not already exist.
     * @param <T>           the type of the bean.
     * @return the existing or newly created bean instance.
     * @throws BeanContextException if the {@link ObjectFactory} produces a {@code null} object.
     */
    default <T> T getBean(String name, ObjectFactory<T> objectFactory) {
        T bean = getBean(name);

        if (bean == null) {
            bean = objectFactory.createObject();

            if (bean == null) {
                throw new BeanContextException("ObjectFactory must produce a non-null object");
            }

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
     * Checks whether a bean with the specified name is already registered in this container.
     *
     * @param name the name of the bean.
     * @return {@code true} if a bean with the given name exists, otherwise {@code false}.
     */
    default boolean containsBean(String name) {
        return false;
    }

    /**
     * Determines if the current implementation supports the given {@link Scope}.
     * <p>
     * By default, this method returns {@code false}, indicating that the scope
     * is not supported. Implementations can override this method to specify
     * which scopes they support.
     *
     * @param scope the {@link Scope} to check for support.
     * @return {@code true} if the scope is supported, {@code false} otherwise.
     */
    default boolean supports(Scope scope) {
        return false;
    }

}
