package org.jmouse.core.bind;

/**
 * Defines an interface for accessing and modifying properties of an object.
 * <p>
 * Implementations of this interface provide mechanisms to inject and obtain values
 * dynamically from an object's properties, making it useful for frameworks
 * that require reflective or programmatic property manipulation.
 * </p>
 *
 * @param <T> the type of the object whose property is being accessed
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public interface PropertyAccessor<T> {

    /**
     * Injects a value into a property of the given object.
     * <p>
     * This method allows setting a new value for a property, assuming
     * that the property is writable.
     * </p>
     *
     * @param object the target object whose property is to be modified
     * @param value  the value to inject into the property
     * @throws IllegalArgumentException if the property is not writable
     */
    void injectValue(T object, Object value);

    /**
     * Retrieves the current value of a property from the given object.
     * <p>
     * If the property is readable, this method returns its current value;
     * otherwise, it may return {@code null}.
     * </p>
     *
     * @param object the target object whose property value is to be retrieved
     * @return the value of the property, or {@code null} if it cannot be accessed
     */
    Object obtainValue(T object);

}
