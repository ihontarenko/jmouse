package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;

/**
 * Direct implementation of a PropertyAccessor.
 * <p>
 * Provides immediate (synchronous) access to the property using getter and setter methods
 * from the property descriptor.
 * </p>
 *
 * @param <T> the type of the object whose property is described
 */
public class DirectPropertyAccess<T> implements PropertyAccessor<T> {

    private final PropertyDescriptor<T> property;

    /**
     * Creates a direct (synchronous) PropertyAccessor based on the provided property descriptor.
     *
     * @param descriptor the descriptor of the property which includes getter and setter information
     * @param <T>        the type of the object whose property is described
     * @return a new {@code PropertyAccessor} that provides direct access to the property
     */
    public static <T> PropertyAccessor<T> forPropertyDescriptor(PropertyDescriptor<T> descriptor) {
        return new DirectPropertyAccess<>(descriptor);
    }

    /**
     * Constructs a DirectAccess instance with the specified property descriptor.
     *
     * @param property the descriptor of the property
     */
    public DirectPropertyAccess(PropertyDescriptor<T> property) {
        this.property = property;
    }

    /**
     * Writes a value into the property using the setter from the descriptor.
     *
     * @param instance the target object whose property is to be modified
     * @param value    the value to write into the property
     */
    @Override
    public void writeValue(T instance, Object value) {
        if (isWritable()) {
            property.getSetter().set(instance, value);
        }
    }

    /**
     * Reads the value of the property using the getter from the descriptor.
     *
     * @param instance the target object from which to read the property value
     * @return the property value, or {@code null} if the property cannot be read
     */
    @Override
    public Object readValue(T instance) {
        Object value = null;

        if (isReadable()) {
            value = property.getGetter().get(instance);
        }

        return value;
    }

    /**
     * Checks if the property is readable (i.e., has a getter).
     *
     * @return {@code true} if the property has a getter, {@code false} otherwise
     */
    @Override
    public boolean isReadable() {
        return property.isReadable();
    }

    /**
     * Checks if the property is writable (i.e., has a setter).
     *
     * @return {@code true} if the property has a setter, {@code false} otherwise
     */
    @Override
    public boolean isWritable() {
        return property.isWritable();
    }
}
