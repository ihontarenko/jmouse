package org.jmouse.beans;

import java.util.function.Supplier;

/**
 * 🫘 BeanProvider
 *
 * Abstraction for retrieving beans (managed components) from a container.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>🔎 Lookup beans by type</li>
 *   <li>⚙️ Provide optional fallbacks via {@link Supplier}</li>
 *   <li>🌱 Decoupled from specific DI container implementations</li>
 * </ul>
 */
public interface BeanProvider {

    /**
     * 🔎 Retrieve a bean of the given type from the container.
     *
     * @param beanClass class of the requested bean
     * @param <T>       type of the bean
     * @return bean instance or {@code null} if not found
     */
    <T> T getBean(final Class<T> beanClass);

    /**
     * 🔎 Retrieve a bean of the given type, or provide a fallback.
     *
     * <p>If no bean is found and {@code defaultBean} is non-null,
     * the supplier is invoked to create/return a fallback instance.</p>
     *
     * @param beanClass   class of the requested bean
     * @param defaultBean fallback supplier (optional)
     * @param <T>         type of the bean
     * @return resolved bean or fallback
     */
    default <T> T getBean(Class<T> beanClass, Supplier<T> defaultBean) {
        T bean = getBean(beanClass);

        if (bean == null && defaultBean != null) {
            bean = defaultBean.get();
        }

        return bean;
    }
}
