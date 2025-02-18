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
     * Retrieves a structured by its name.
     *
     * @param name the name of the structured to retrieve.
     * @param <T> the type of the structured.
     * @return the structured instance.
     */
    <T> T getBean(String name);

    /**
     * Registers a structured with the given name.
     *
     * @param name the name of the structured to register.
     * @param bean the structured instance to register.
     */
    void registerBean(String name, Object bean);

    /**
     * Checks if a structured with the given name exists in the container.
     *
     * @param name the name of the structured.
     * @return true if the structured exists, false otherwise.
     */
    boolean containsBean(String name);

    /**
     * Retrieves a structured by name, or creates it using the provided ObjectFactory if it doesn't exist.
     * Registers the created structured in the container.
     *
     * @param name the name of the structured.
     * @param objectFactory the ObjectFactory used to create the structured if it doesn't exist.
     * @param <T> the type of the structured.
     * @return the structured instance.
     * @throws BeanContextException if the ObjectFactory produces a null structured.
     */
    default <T> T getBean(String name, ObjectFactory<T> objectFactory) {
        T bean = getBean(name);

        if (bean == null) {
            bean = objectFactory.createObject();

            if (bean == null) {
                throw new BeanContextException("ObjectFactory must produce a non-null structured");
            }

            LOGGER.info("Register structured '{}' with '{}' container", name, Reflections.getShortName(getClass()));
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
     * Retrieves a structured by its class type.
     *
     * @param type the class type of the structured.
     * @param <T> the type of the structured.
     * @return the structured instance.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default <T> T getBean(Class<T> type) {
        throw new BeanContextException(
                UNSUPPORTED_CALL_EXCEPTION_MESSAGE.formatted("getBean(Class<T>)", getClass()));
    }

    /**
     * Retrieves a structured by its class type and name.
     *
     * @param type the class type of the structured.
     * @param name the name of the structured.
     * @param <T> the type of the structured.
     * @return the structured instance.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default <T> T getBean(Class<T> type, String name) {
        throw new BeanContextException(
                UNSUPPORTED_CALL_EXCEPTION_MESSAGE.formatted("getBean(Class<T>, String)", getClass()));
    }

    /**
     * Registers a structured with the given class type and singleton scope.
     *
     * @param type the class type of the structured.
     * @param objectFactory the ObjectFactory for the structured.
     */
    default void registerBean(Class<?> type, ObjectFactory<Object> objectFactory) {
        registerBean(type, objectFactory, BeanScope.SINGLETON);
    }

    /**
     * Registers a structured with the given class type, ObjectFactory, and scope.
     *
     * @param type the class type of the structured.
     * @param objectFactory the ObjectFactory for the structured.
     * @param scope the scope of the structured.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default void registerBean(Class<?> type, ObjectFactory<Object> objectFactory, Scope scope) {
        registerBean(type, (Object) objectFactory, scope);
    }

    /**
     * Registers a structured with the given class type and singleton scope.
     *
     * @param type the class type of the structured.
     * @param bean the structured instance to register.
     */
    default void registerBean(Class<?> type, Object bean) {
        registerBean(type, bean, BeanScope.SINGLETON);
    }

    /**
     * Registers a structured with the given class type, instance, and scope.
     *
     * @param type the class type of the structured.
     * @param bean the structured instance to register.
     * @param scope the scope of the structured.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default void registerBean(Class<?> type, Object bean, Scope scope) {
        throw new BeanContextException(
                UNSUPPORTED_CALL_EXCEPTION_MESSAGE
                        .formatted("registerBean(Class<?>, Object, BeanScope)", getClass()));
    }

    /**
     * Registers a structured with the given name, ObjectFactory, and scope.
     *
     * @param name the name of the structured.
     * @param objectFactory the ObjectFactory for the structured.
     */
    default void registerBean(String name, ObjectFactory<Object> objectFactory) {
        registerBean(name, (Object) objectFactory, BeanScope.SINGLETON);
    }

    /**
     * Registers a structured with the given name, ObjectFactory, and scope.
     *
     * @param name the name of the structured.
     * @param objectFactory the ObjectFactory for the structured.
     * @param scope the scope of the structured.
     */
    default void registerBean(String name, ObjectFactory<Object> objectFactory, Scope scope) {
        registerBean(name, (Object) objectFactory, scope);
    }

    /**
     * Registers a structured with the given name, instance, and scope.
     *
     * @param name the name of the structured.
     * @param bean the structured instance to register.
     * @param scope the scope of the structured.
     * @throws BeanContextException if this method is unsupported in the current implementation.
     */
    default void registerBean(String name, Object bean, Scope scope) {
        throw new BeanContextException(
                UNSUPPORTED_CALL_EXCEPTION_MESSAGE
                        .formatted("registerBean(String, Object, BeanScope)", getClass()));
    }

}
