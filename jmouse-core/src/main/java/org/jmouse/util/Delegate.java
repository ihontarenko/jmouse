package org.jmouse.util;

/**
 * Generic interface for managing a delegate object.
 * <p>
 * This interface provides methods to set and retrieve a delegate of a specified type.
 * It is useful in scenarios where an object acts as a proxy or wrapper for another object.
 * </p>
 *
 * @param <T> the type of the delegate.
 */
public interface Delegate<T> {

    /**
     * Sets the delegate object.
     *
     * @param delegate the delegate object to set.
     */
    void setDelegate(T delegate);

    /**
     * Retrieves the current delegate object.
     *
     * @return the delegate object, or {@code null} if no delegate is set.
     */
    T getDelegate();
}
