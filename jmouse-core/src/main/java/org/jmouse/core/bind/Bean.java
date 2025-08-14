package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.Factory;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Represents a generic structured model that encapsulates metadata and properties of a Java type.
 * <p>
 * This class provides methods for managing properties dynamically, enabling reflection-based
 * property access and manipulation.
 * </p>
 *
 * @param <T> the type of the structured instance
 */
abstract public class Bean<T> {

    protected final ObjectDescriptor<T> descriptor;

    /**
     * The Java type represented by this structured.
     */
    protected final JavaType type;

    /**
     * Constructs a structured for the specified type.
     *
     * @param type the Java type this structured represents
     */
    protected Bean(JavaType type, ObjectDescriptor<T> descriptor) {
        this.type = type;
        this.descriptor = descriptor;
    }

    /**
     * Retrieves all properties defined in this structured.
     *
     * @return a collection of properties
     */
    public Collection<? extends PropertyDescriptor<T>> getProperties() {
        return descriptor.getProperties().values();
    }

    /**
     * Retrieves a specific property by name.
     *
     * @param name the name of the property
     * @return the property associated with the given name, or {@code null} if not found
     */
    public PropertyDescriptor<T> getProperty(String name) {
        return descriptor.getProperty(name);
    }

    /**
     * Checks whether this structured contains a property with the given name.
     *
     * @param name the property name to check
     * @return {@code true} if the property exists, otherwise {@code false}
     */
    public boolean hasProperty(String name) {
        return descriptor.hasProperty(name);
    }

    /**
     * Retrieves the value of this property from an instance provided by the factory.
     *
     * @param factory a factory supplying an instance of {@code T}
     * @return a supplier returning the property value
     */
    public Supplier<Object> getValue(PropertyDescriptor<T> property, Factory<T> factory) {
        return () -> {
            Object value = null;

            if (property.isReadable()) {
                value = property.getAccessor().readValue(factory.create());
            }

            return value;
        };
    }

    /**
     * Sets the value of this property on an instance provided by the factory.
     *
     * @param factory a factory supplying an instance of {@code T}
     * @param value   the value to set
     * @param <V>     the type of the value
     */
    public <V> void setValue(PropertyDescriptor<T> property, Factory<T> factory, V value) {
        if (property.isWritable()) {
            property.getAccessor().writeValue(factory.create(), value);
        }
    }

}
