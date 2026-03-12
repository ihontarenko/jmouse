package org.jmouse.core.context.beans;

/**
 * 🫘 Bean lookup contract for contexts.
 *
 * <p>
 * Separates <b>container-level bean access</b>
 * from generic key-value context concerns.
 * </p>
 *
 * <p>
 * Typically backed by a {@link BeanLookup} delegate
 * provided by the surrounding container.
 * </p>
 */
public interface BeanLookupContext extends BeanLookup {

    /**
     * 🔌 Attach bean valueProvider.
     *
     * @param beanProvider backing {@link BeanLookup}
     */
    void setBeanLookup(BeanLookup beanProvider);

    BeanLookup getBeanLookup();

}
