package org.jmouse.core.bind;

import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;

import java.util.function.Supplier;

/**
 * Defines an interface for accessing and modifying properties of an object.
 * <p>
 * Implementations of this interface provide mechanisms to inject and obtain values
 * dynamically from an object's properties, making it useful for frameworks
 * that require reflective or programmatic property manipulation.
 * </p>
 *
 * @param <T> the type of the object whose property is being accessed
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
    void writeValue(T object, Object value);

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
    Object readValue(T object);

    static <T> PropertyAccessor<T> forDirect(PropertyDescriptor<T> descriptor) {
        return new DirectAccess<>(descriptor);
    }

    static <T> PropertyAccessor<Supplier<T>> forLazy(PropertyDescriptor<T> descriptor) {
        return new LazyAccess<>(descriptor);
    }

    class LazyAccess<T> implements PropertyAccessor<Supplier<T>> {

        private final PropertyDescriptor<T> property;
        private final PropertyAccessor<T>   delegate;

        public LazyAccess(PropertyDescriptor<T> property) {
            this.property = property;
            this.delegate = PropertyAccessor.forDirect(property);
        }

        @Override
        public void writeValue(Supplier<T> factory, Object value) {
            if (property.isWritable()) {
                delegate.writeValue(factory.get(), value);
            }
        }

        @Override
        public Object readValue(Supplier<T> factory) {
            Object value = null;

            if (property.isReadable()) {
                value = delegate.readValue(factory.get());
            }

            return value;
        }

    }

    class DirectAccess<T> implements PropertyAccessor<T> {

        private final PropertyDescriptor<T> property;

        public DirectAccess(PropertyDescriptor<T> property) {
            this.property = property;
        }

        @Override
        public void writeValue(T instance, Object value) {
            if (property.isWritable()) {
                property.getSetter().set(instance, value);
            }
        }

        @Override
        public Object readValue(T instance) {
            Object value = null;

            if (property.isReadable()) {
                value = property.getGetter().get(instance);
            }

            return value;
        }
    }

}
