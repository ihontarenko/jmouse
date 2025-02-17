package org.jmouse.core.bind;

import org.jmouse.core.bind.bean.bean.PropertyDescriptor;

/**
 * A default implementation of {@link PropertyAccessor} that operates based on
 * a given {@link PropertyDescriptor}.
 * <p>
 * This implementation ensures that only writable properties can be modified,
 * and only readable properties can be accessed.
 * </p>
 *
 * @param <T> the type of the bean whose property is being accessed
 */
class PropertyDescriptorAccessor<T> implements PropertyAccessor<T> {

    private final PropertyDescriptor<T> descriptor;

    /**
     * Constructs a {@code PropertyAccessor.PropertyDescriptorAccessor} using the given property descriptor.
     *
     * @param descriptor the property descriptor used to access the property
     */
    public PropertyDescriptorAccessor(PropertyDescriptor<T> descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * Injects a value into the bean's property if it is writable.
     * <p>
     * The method first checks if the property is writable before attempting to set the value.
     * </p>
     *
     * @param object the bean whose property value is being modified
     * @param value  the value to be injected into the property
     */
    @Override
    public void injectValue(T object, Object value) {
        if (descriptor.isWritable()) {
            descriptor.getSetter().set(object, value);
        }
    }

    /**
     * Retrieves the value of the bean's property if it is readable.
     * <p>
     * If the property has a getter, the method invokes it to retrieve the value;
     * otherwise, it returns {@code null}.
     * </p>
     *
     * @param object the bean whose property value is being retrieved
     * @return the value of the property, or {@code null} if it is not readable
     */
    @Override
    public Object obtainValue(T object) {
        Object value = null;

        if (descriptor.isReadable()) {
            value = descriptor.getGetter().get(object);
        }

        return value;
    }

    @Override
    public String toString() {
        return "Accessor to: %s".formatted(descriptor);
    }

}
