package svit.beans;

import svit.beans.definition.BeanDefinition;

/**
 * Interface for initializing bean instances.
 * <p>
 * The {@code BeanInitializer} is responsible for performing pre-initialization
 * and post-initialization steps, as well as invoking any initializer methods
 * annotated within the bean's class.
 * </p>
 * <p>
 * Typical usage includes setting up dependencies, applying custom logic,
 * or executing lifecycle callbacks during the initialization process.
 * </p>
 */
public interface BeanInitializer {

    /**
     * Initializes a bean instance by applying pre-initialization and post-initialization
     * processing steps and invoking any annotated initializer methods.
     *
     * @param instance   the bean instance to initialize.
     * @param definition the {@link BeanDefinition} associated with the bean.
     */
    void initializeBean(Object instance, BeanDefinition definition);

}
