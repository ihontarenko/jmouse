package org.jmouse.core.access;

import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;

import java.util.function.Supplier;

/**
 * Lazy implementation of a PropertyAccessor.
 * <p>
 * Uses a {@link Supplier} to defer obtaining the target object and delegates to a direct accessor.
 * </p>
 *
 * @param <T> the type of the object whose property is described
 */
public class LazyPropertyAccess<T> implements PropertyAccessor<Supplier<T>> {

    private final PropertyDescriptor<T> property;
    private final PropertyAccessor<T>   delegate;

    /**
     * Constructs a LazyAccess instance with the specified property descriptor.
     *
     * @param property the descriptor of the property
     */
    public LazyPropertyAccess(PropertyDescriptor<T> property) {
        this.property = property;
        this.delegate = DirectPropertyAccess.forPropertyDescriptor(property);
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
    public static <T> PropertyAccessor<Supplier<T>> forPropertyDescriptor(PropertyDescriptor<T> descriptor) {
        return new LazyPropertyAccess<>(descriptor);
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
