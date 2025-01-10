package svit.container;

import svit.container.definition.BeanDefinition;
import svit.container.processor.BeanPostProcessor;

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
}
