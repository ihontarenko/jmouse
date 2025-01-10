package svit.container;

import svit.container.definition.BeanDefinition;

/**
 * Interface representing a factory for creating bean instances.
 * <p>
 * A {@code BeanFactory} is responsible for creating objects based on
 * their {@link BeanDefinition}, resolving their dependencies, and managing
 * their initialization lifecycle.
 * </p>
 * <p>
 * This interface is typically used by a {@link BeanContext} to delegate
 * the creation of bean instances.
 * </p>
 *
 * @see BeanDefinition
 * @see BeanContext
 */
public interface BeanFactory extends BeanContextAware {

    /**
     * Creates a new instance of a bean based on the provided {@link BeanDefinition}.
     * <p>
     * The factory uses the information in the {@code BeanDefinition} to determine
     * how to construct the object, resolve its dependencies, and initialize it.
     * </p>
     *
     * @param definition the bean definition containing metadata about the bean
     * @param <T>        the type of the bean
     * @return the newly created bean instance
     * @throws BeanInstantiationException if the bean cannot be created due to errors
     *                                    in the definition or during dependency resolution
     */
    <T> T createBean(BeanDefinition definition);
}
