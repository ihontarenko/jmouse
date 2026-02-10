package org.jmouse.core.access;

/**
 * Defines an interface for accessing and modifying an object's property.
 * <p>
 * Implementations of this interface provide mechanisms to inject and obtain values
 * dynamically from an object's properties. This is useful for frameworks that require
 * reflective or programmatic property manipulation.
 * </p>
 *
 * @param <T> the type of the object whose property is being accessed
 */
public interface PropertyAccessor<T> {

    /**
     * Injects a value into the specified property of the given object.
     * <p>
     * This method sets a new value for a property if the property is writable.
     * </p>
     *
     * @param object the target object whose property will be modified
     * @param value  the value to inject into the property
     * @throws IllegalArgumentException if the property is not writable
     */
    default void writeValue(T object, Object value) {}

    /**
     * Retrieves the current value of the specified property from the given object.
     * <p>
     * If the property is readable, this method returns its current value;
     * otherwise, it may return {@code null}.
     * </p>
     *
     * @param object the target object from which to retrieve the property value
     * @return the value of the property, or {@code null} if it cannot be accessed
     */
    default Object readValue(T object) {
        return null;
    }

    /**
     * Checks if the property is readable (i.e., has a getter).
     *
     * @return {@code true} if the property has a getter, {@code false} otherwise
     */
    default boolean isReadable() {
        return false;
    }

    /**
     * Checks if the property is writable (i.e., has a setter).
     *
     * @return {@code true} if the property has a setter, {@code false} otherwise
     */
    default boolean isWritable() {
        return false;
    }

}
