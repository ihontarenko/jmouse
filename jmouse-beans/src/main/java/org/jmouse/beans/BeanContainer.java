package org.jmouse.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.core.reflection.Reflections;

/**
 * Represents a container for beans. Provides methods to retrieve, register, and manage beans within the container.
 */
public interface BeanContainer {

    // Error message template for unsupported method calls
    String UNSUPPORTED_CALL_EXCEPTION_MESSAGE = "Method '%s' is unsupported in this '%s' implementation.";

    Logger LOGGER = LoggerFactory.getLogger(BeanContainer.class);

    /**
     * Retrieves a bean by its name.
     *
     * @param name the name of the bean to retrieve.
     * @param <T> the type of the bean.
     * @return the bean instance.
     */
    <T> T getBean(String name);

    /**
     * Registers a bean with the given name.
     *
     * @param name the name of the bean to register.
     * @param bean the bean instance to register.
     */
    void registerBean(String name, Object bean);

    /**
     * Checks if a bean with the given name exists in the container.
     *
     * @param name the name of the bean.
     * @return true if the bean exists, false otherwise.
     */
    boolean containsBean(String name);

    /**
     * Retrieves a bean by name, or creates it using the provided ObjectFactory if it doesn't exist.
     * Registers the created bean in the container.
     *
     * @param name the name of the bean.
     * @param objectFactory the ObjectFactory used to create the bean if it doesn't exist.
     * @param <T> the type of the bean.
     * @return the bean instance.
     * @throws BeanContextException if the ObjectFactory produces a null bean.
     */
    default <T> T getBean(String name, ObjectFactory<T> objectFactory) {
        T bean = getBean(name);

        if (bean == null) {
            bean = objectFactory.createObject();

            if (bean == null) {
                throw new BeanContextException("ObjectFactory must produce a non-null bean");
            }

            LOGGER.info("Register bean '{}' with '{}' container", name, Reflections.getShortName(getClass()));
            registerBean(name, bean);
        }

        return bean;
    }

    /**
     * Checks if the container supports a given scope.
     *
     * @param scope the scope to check.
     * @return false by default, indicating the container does not support the given scope.
     */
    default boolean supports(Scope scope) {
        return false;
    }

    /**
     * Retrieves a bean by its class type.
     *
     * @param type the class type of the bean.
     * @param <T> the type of the bean.
     * @return the bean instance.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default <T> T getBean(Class<T> type) {
        throw new BeanContextException(
                UNSUPPORTED_CALL_EXCEPTION_MESSAGE.formatted("getBean(Class<T>)", getClass()));
    }

    /**
     * Retrieves a bean by its class type and name.
     *
     * @param type the class type of the bean.
     * @param name the name of the bean.
     * @param <T> the type of the bean.
     * @return the bean instance.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default <T> T getBean(Class<T> type, String name) {
        throw new BeanContextException(
                UNSUPPORTED_CALL_EXCEPTION_MESSAGE.formatted("getBean(Class<T>, String)", getClass()));
    }

    /**
     * Registers a bean with the given class type and singleton scope.
     *
     * @param type the class type of the bean.
     * @param objectFactory the ObjectFactory for the bean.
     */
    default void registerBean(Class<?> type, ObjectFactory<Object> objectFactory) {
        registerBean(type, objectFactory, BeanScope.SINGLETON);
    }

    /**
     * Registers a bean with the given class type, ObjectFactory, and scope.
     *
     * @param type the class type of the bean.
     * @param objectFactory the ObjectFactory for the bean.
     * @param scope the scope of the bean.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default void registerBean(Class<?> type, ObjectFactory<Object> objectFactory, Scope scope) {
        registerBean(type, (Object) objectFactory, scope);
    }

    /**
     * Registers a bean with the given class type and singleton scope.
     *
     * @param type the class type of the bean.
     * @param bean the bean instance to register.
     */
    default void registerBean(Class<?> type, Object bean) {
        registerBean(type, bean, BeanScope.SINGLETON);
    }

    /**
     * Registers a bean with the given class type, instance, and scope.
     *
     * @param type the class type of the bean.
     * @param bean the bean instance to register.
     * @param scope the scope of the bean.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default void registerBean(Class<?> type, Object bean, Scope scope) {
        throw new BeanContextException(
                UNSUPPORTED_CALL_EXCEPTION_MESSAGE
                        .formatted("registerBean(Class<?>, Object, BeanScope)", getClass()));
    }

    /**
     * Registers a bean with the given name, ObjectFactory, and scope.
     *
     * @param name the name of the bean.
     * @param objectFactory the ObjectFactory for the bean.
     */
    default void registerBean(String name, ObjectFactory<Object> objectFactory) {
        registerBean(name, (Object) objectFactory, BeanScope.SINGLETON);
    }

    /**
     * Registers a bean with the given name, ObjectFactory, and scope.
     *
     * @param name the name of the bean.
     * @param objectFactory the ObjectFactory for the bean.
     * @param scope the scope of the bean.
     */
    default void registerBean(String name, ObjectFactory<Object> objectFactory, Scope scope) {
        registerBean(name, (Object) objectFactory, scope);
    }

    /**
     * Registers a bean with the given name, instance, and scope.
     *
     * @param name the name of the bean.
     * @param bean the bean instance to register.
     * @param scope the scope of the bean.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default void registerBean(String name, Object bean, Scope scope) {
        throw new BeanContextException(
                UNSUPPORTED_CALL_EXCEPTION_MESSAGE
                        .formatted("registerBean(String, Object, BeanScope)", getClass()));
    }

}
