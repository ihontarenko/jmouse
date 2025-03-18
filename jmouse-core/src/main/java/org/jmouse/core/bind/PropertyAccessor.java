package org.jmouse.core.bind;

import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;

import java.util.function.Supplier;

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
    void writeValue(T object, Object value);

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
    Object readValue(T object);

    /**
     * Creates a direct (synchronous) PropertyAccessor based on the provided property descriptor.
     *
     * @param descriptor the descriptor of the property which includes getter and setter information
     * @param <T>        the type of the object whose property is described
     * @return a new {@code PropertyAccessor} that provides direct access to the property
     */
    static <T> PropertyAccessor<T> forDirect(PropertyDescriptor<T> descriptor) {
        return new DirectAccess<>(descriptor);
    }

    /**
     * Creates a lazy PropertyAccessor for deferred access to a property.
     * <p>
     * Uses a {@link Supplier} to obtain the target object only when needed.
     * </p>
     *
     * @param descriptor the property descriptor
     * @param <T>        the type of the object whose property is described
     * @return a new {@code PropertyAccessor} for lazy access to the property
     */
    static <T> PropertyAccessor<Supplier<T>> forLazy(PropertyDescriptor<T> descriptor) {
        return new LazyAccess<>(descriptor);
    }

    /**
     * Lazy implementation of a PropertyAccessor.
     * <p>
     * Uses a {@link Supplier} to defer obtaining the target object and delegates to a direct accessor.
     * </p>
     *
     * @param <T> the type of the object whose property is described
     */
    class LazyAccess<T> implements PropertyAccessor<Supplier<T>> {

        private final PropertyDescriptor<T> property;
        private final PropertyAccessor<T> delegate;

        /**
         * Constructs a LazyAccess instance with the specified property descriptor.
         *
         * @param property the descriptor of the property
         */
        public LazyAccess(PropertyDescriptor<T> property) {
            this.property = property;
            this.delegate = PropertyAccessor.forDirect(property);
        }

        /**
         * Injects a value into the property, obtaining the target object using the provided Supplier.
         *
         * @param factory the supplier of the target object
         * @param value   the value to write into the property
         */
        @Override
        public void writeValue(Supplier<T> factory, Object value) {
            if (property.isWritable()) {
                delegate.writeValue(factory.get(), value);
            }
        }

        /**
         * Reads the value of the property, obtaining the target object using the provided Supplier.
         *
         * @param factory the supplier of the target object
         * @return the property value, or {@code null} if the property is not readable
         */
        @Override
        public Object readValue(Supplier<T> factory) {
            Object value = null;

            if (property.isReadable()) {
                value = delegate.readValue(factory.get());
            }

            return value;
        }
    }

    /**
     * Direct implementation of a PropertyAccessor.
     * <p>
     * Provides immediate (synchronous) access to the property using getter and setter methods
     * from the property descriptor.
     * </p>
     *
     * @param <T> the type of the object whose property is described
     */
    class DirectAccess<T> implements PropertyAccessor<T> {

        private final PropertyDescriptor<T> property;

        /**
         * Constructs a DirectAccess instance with the specified property descriptor.
         *
         * @param property the descriptor of the property
         */
        public DirectAccess(PropertyDescriptor<T> property) {
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
            if (property.isWritable()) {
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

            if (property.isReadable()) {
                value = property.getGetter().get(instance);
            }

            return value;
        }
    }
}
