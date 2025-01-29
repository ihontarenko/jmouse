package svit.beans;

import svit.beans.definition.BeanDefinition;

/**
 * Interface for initializing beans during their lifecycle.
 * <p>
 * A {@code BeanInitializer} is responsible for performing initialization logic on a bean instance,
 * typically by applying configuration defined in the corresponding {@link BeanDefinition}.
 * This may include invoking lifecycle methods, applying proxies, or other pre/post-initialization tasks.
 * </p>
 *
 * @see BeanDefinition
 */
public interface BeanInitializer {

    /**
     * Performs initialization logic on a bean instance.
     * <p>
     * This method is invoked to prepare the bean instance for use. Implementations may apply additional
     * configurations, wrap the bean in a proxy, or perform validation based on the provided {@link BeanDefinition}.
     * </p>
     *
     * @param instance   the bean instance to initialize.
     * @param definition the {@link BeanDefinition} associated with the bean, providing metadata for initialization.
     * @return the initialized bean instance, potentially wrapped or modified.
     */
    <T> T initializeBean(T instance, BeanDefinition definition);
}
