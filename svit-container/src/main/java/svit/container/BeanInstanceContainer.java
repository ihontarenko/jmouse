package svit.container;

/**
 * Interface for managing bean instances in a container.
 * Provides methods to retrieve and register beans by name, with support for lazy initialization.
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

}
