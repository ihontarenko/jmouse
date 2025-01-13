package svit.beans;

import svit.beans.definition.BeanDefinition;
import svit.beans.processor.BeanPostProcessor;
import svit.reflection.Reflections;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * A container interface for managing beans, providing methods to retrieve, register, and query bean instances.
 * Extends the functionality of {@link BeanInstanceContainer}.
 *
 * <p>Example usage:
 * <pre>{@code
 * BeanContainer container = new DefaultBeanContext();
 * container.registerBean(MyService.class, new MyServiceImpl());
 * MyService service = container.getBean(MyService.class);
 * }</pre>
 */
public interface BeanContainer extends BeanInstanceContainer {

    /**
     * Retrieves the names of all beans that match the specified type.
     *
     * @param type the class type of the beans.
     * @return a list of bean names that match the given type.
     */
    List<String> getBeanNames(Class<?> type);

    /**
     * Retrieves a bean by its type.
     *
     * @param type the class type of the bean.
     * @param <T>  the type of the bean.
     * @return the bean instance that matches the specified type.
     * @throws BeanContextException if no beans or multiple beans of the given type are found.
     */
    <T> T getBean(Class<T> type);

    /**
     * Retrieves a bean by its type and name.
     *
     * @param type the class type of the bean.
     * @param name the name of the bean.
     * @param <T>  the type of the bean.
     * @return the bean instance that matches the specified type and name.
     * @throws BeanContextException if the bean with the given name is not found or does not match the given type.
     */
    <T> T getBean(Class<T> type, String name);

    /**
     * Retrieves all beans that match the specified type.
     *
     * @param type the class type of the beans.
     * @param <T>  the type of the beans.
     * @return a list of beans matching the given type.
     */
    <T> List<T> getBeans(Class<T> type);

    /**
     * Registers a bean instance of the specified type with a default {@link BeanScope#SINGLETON}.
     *
     * @param type the type of the bean.
     * @param bean the bean instance to register.
     */
    default void registerBean(Class<?> type, Object bean) {
        registerBean(type, bean, BeanScope.SINGLETON);
    }

    /**
     * Registers a bean instance of the specified type and scope.
     *
     * @param type      the type of the bean.
     * @param bean      the bean instance to register.
     * @param scope     the scope for the bean.
     */
    void registerBean(Class<?> type, Object bean, BeanScope scope);

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
                "Scope-based bean registration is not supported in %s. Bean '%s' with scope '%s' cannot be registered."
                        .formatted(Reflections.getShortName(getClass()), name, beanScope));
    }

    /**
     * Registers a bean instance with the given name using an {@link ObjectFactory}.
     * <p>
     * By default, this method registers the bean with a {@link BeanScope#PROTOTYPE} scope.
     * Override this method if a different default behavior is required.
     *
     * @param name the name of the bean.
     * @param bean the factory for creating the bean instance.
     * @throws BeanContextException if this operation is not supported in the implementation.
     */
    default void registerBean(String name, ObjectFactory<Object> bean) {
        registerBean(name, bean, BeanScope.PROTOTYPE);
    }

    /**
     * Registers a bean instance with the given name using an {@link ObjectFactory} and a specified {@link BeanScope}.
     * <p>
     * The default implementation throws a {@link BeanContextException} to indicate that
     * scope-based registration is not supported. Override this method in a subclass
     * if this functionality is required.
     *
     * @param name      the name of the bean.
     * @param bean      the factory for creating the bean instance.
     * @param beanScope the scope of the bean.
     * @throws BeanContextException if this operation is not supported.
     */
    default void registerBean(String name, ObjectFactory<Object> bean, BeanScope beanScope) {
        throw new BeanContextException(
                "Factory-based scope registration is not supported in %s. Bean '%s' with scope '%s' cannot be registered."
                        .formatted(Reflections.getShortName(getClass()), name, beanScope));
    }

    /**
     * Checks if a bean corresponding to the given {@link BeanDefinition} is already registered.
     * <p>
     * This method delegates to {@link #containsBean(String)} by extracting the bean name
     * from the provided definition.
     *
     * @param definition the {@link BeanDefinition} whose bean name will be checked
     * @return {@code true} if a bean with the specified definition name is registered,
     *         otherwise {@code false}
     */
    default boolean containsBean(BeanDefinition definition) {
        return containsBean(definition.getBeanName());
    }
}
