package org.jmouse.util;

/**
 * Generic interface for managing a delegate bean.
 * <p>
 * This interface provides methods to set and retrieve a delegate of a specified type.
 * It is useful in scenarios where an bean acts as a proxy or wrapper for another bean.
 * </p>
 *
 * @param <T> the type of the delegate.
 */
public interface Delegate<T> {

    /**
     * Sets the delegate bean.
     *
     * @param delegate the delegate bean to set.
     */
    void setDelegate(T delegate);

    /**
     * Retrieves the current delegate bean.
     *
     * @return the delegate bean, or {@code null} if no delegate is set.
     */
    T getDelegate();
}
