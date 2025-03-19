package org.jmouse.core.bind;

/**
 * Represents a virtual property that can be dynamically computed or derived from an object of type T.
 * <p>
 * Virtual properties are not necessarily physical fields on the object; instead, they are computed on demand
 * using custom logic provided by the implementation of this interface.
 * </p>
 *
 * @param <T> the type of the object for which this virtual property applies
 */
public interface VirtualProperty<T> {

    /**
     * Returns the instance type for which this virtual property is applicable.
     *
     * @return the {@link Class} representing the type T
     */
    Class<T> getInstanceType();

    /**
     * Computes and returns the value of this virtual property for the given object.
     *
     * @param object the object from which to derive the property value
     * @param name   the name of the virtual property
     * @return the computed value of the virtual property, or {@code null} if the property cannot be resolved
     */
    Object getValue(T object, String name);
}
