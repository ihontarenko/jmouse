package svit.container;

import svit.container.definition.BeanDefinition;
import svit.container.processor.BeanPostProcessor;
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
     * Initializes a bean instance by applying pre-initialization and post-initialization
     * processing steps and invoking any annotated initializer methods.
     *
     * @param instance   the bean instance to initialize.
     * @param definition the {@link BeanDefinition} associated with the bean.
     */
    void initializeBean(Object instance, BeanDefinition definition);

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
     * Retrieves all beans annotated with the specified annotation.
     *
     * @param annotation the annotation class to match.
     * @return a list of beans annotated with the given annotation class.
     */
    List<Object> getAnnotatedBeans(Class<? extends Annotation> annotation);

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
     * Registers a bean instance of the specified type and lifecycle.
     *
     * @param type      the type of the bean.
     * @param bean      the bean instance to register.
     * @param lifecycle the lifecycle scope for the bean.
     */
    void registerBean(Class<?> type, Object bean, BeanScope lifecycle);

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
     * Creates a bean instance from the given {@link BeanDefinition}, handling
     * cyclic dependency detection, post-processing, and optional registration.
     * <p>
     * This method performs several steps:
     * <ol>
     *   <li>Detects and prevents cyclic dependencies by tracking visited definitions.</li>
     *   <li>Uses the underlying {@link BeanFactory} to create the actual bean instance.</li>
     *   <li>Runs all registered {@link BeanPostProcessor} instances before and after the bean is initialized.</li>
     *   <li>Invokes the method annotated with {@code @Initialization}, if present, on the newly created bean.</li>
     *   <li>If the {@link BeanDefinition} is marked as singleton, the bean is registered in the singleton container.</li>
     * </ol>
     *
     * @param definition the {@link BeanDefinition} describing how the bean should be created
     * @param <T>        the type of the bean
     * @return the created bean instance
     * @throws BeanInstantiationException if bean creation fails due to dependencies or initialization errors
     */
    <T> T createBean(BeanDefinition definition);

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
