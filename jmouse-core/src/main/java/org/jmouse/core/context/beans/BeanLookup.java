package org.jmouse.core.context.beans;

/**
 * 🫘 Bean lookup contract.
 *
 * <p>
 * Provides read-only access to container-managed beans
 * without exposing container internals.
 * </p>
 *
 * <p>
 * Intended to be implemented by DI containers
 * or delegated by context wrappers.
 * </p>
 */
public interface BeanLookup {

    /**
     * 🔍 Resolve bean by type.
     *
     * @param beanClass bean type
     * @param <T>       bean type
     * @return resolved bean instance
     */
    <T> T getBean(Class<T> beanClass);

    /**
     * 🔍 Resolve bean by name and type.
     *
     * @param beanName  bean name
     * @param beanClass bean type
     * @param <T>       bean type
     * @return resolved bean instance
     */
    <T> T getBean(String beanName, Class<T> beanClass);

}
