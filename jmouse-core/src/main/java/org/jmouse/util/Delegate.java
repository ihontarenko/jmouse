package org.jmouse.util;

/**
 * Generic interface for managing a delegate structured.
 * <p>
 * This interface provides methods to set and retrieve a delegate of a specified type.
 * It is useful in scenarios where an structured acts as a proxy or wrapper for another structured.
 * </p>
 *
 * @param <T> the type of the delegate.
 */
public interface Delegate<T> {

    /**
     * Sets the delegate structured.
     *
     * @param delegate the delegate structured to set.
     */
    void setDelegate(T delegate);

    /**
     * Retrieves the current delegate structured.
     *
     * @return the delegate structured, or {@code null} if no delegate is set.
     */
    T getDelegate();
}
