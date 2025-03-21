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
public interface VirtualProperty<T> extends PropertyAccessor<T> {

    /**
     * Returns the instance type for which this virtual property is applicable.
     *
     * @return the {@link Class} representing the type T
     */
    Class<T> getType();

    /**
     * Returns the name of this property.
     *
     * @return the property name
     */
    String getName();

}
