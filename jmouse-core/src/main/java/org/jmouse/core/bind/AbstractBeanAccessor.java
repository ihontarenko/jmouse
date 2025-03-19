package org.jmouse.core.bind;

import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;

import java.util.HashSet;
import java.util.Set;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link ObjectAccessor} implementation for accessing properties of a structured instance.
 * <p>
 * This class allows retrieving properties dynamically from a wrapped structured instance.
 * It does not support indexed access since beans are typically key-value structures.
 * </p>
 */
abstract public class AbstractBeanAccessor extends AbstractAccessor {

    private final ObjectDescriptor<Object> descriptor;

    /**
     * Creates a {@link AbstractBeanAccessor} for the given structured instance.
     *
     * @param source the structured instance to wrap
     * @throws IllegalArgumentException if the source is {@code null}
     */
    public AbstractBeanAccessor(Object source) {
        super(source);
        this.descriptor = getDescriptor(source.getClass());
    }

    /**
     * Sets a property value by name.
     *
     * @param name  the property name
     * @param value the value to set
     */
    @Override
    public void set(String name, Object value) {
        PropertyDescriptor<Object> property = descriptor.getProperty(name);

        if (!descriptor.hasProperty(name)) {
            throw new IllegalArgumentException(
                    "Accessor '%s' does not have property: '%s'.".formatted(descriptor, name));
        }

        property.getAccessor().writeValue(unwrap(), value);
    }

    /**
     * Sets a property value by index.
     *
     * <p>The default implementation throws an {@link UnsupportedDataSourceException},
     * indicating that indexed access is not supported unless overridden by an implementation.</p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedDataSourceException if indexed access is not supported
     */
    @Override
    public void set(int index, Object value) {
        throw new UnsupportedDataSourceException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(descriptor));
    }

    /**
     * Retrieves a property from the structured instance as a {@link ObjectAccessor}.
     *
     * @param name the name of the property to retrieve
     * @return a {@link ObjectAccessor} wrapping the property value
     * @throws IllegalArgumentException if the property does not exist
     */
    @Override
    public ObjectAccessor get(String name) {
        PropertyDescriptor<Object> property = descriptor.getProperty(name);

        if (!descriptor.hasProperty(name)) {
            throw new IllegalArgumentException(
                    "Accessor '%s' does not have property: '%s'.".formatted(descriptor, name));
        }

        return wrap(property.getAccessor().readValue(source));
    }

    /**
     * Throws an exception since structured instances do not support indexed access.
     *
     * @param index the index to retrieve
     * @return never returns a value
     * @throws UnsupportedDataSourceException always, since indexed access is not supported
     */
    @Override
    public ObjectAccessor get(int index) {
        throw new UnsupportedDataSourceException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(descriptor));
    }

    /**
     * Retrieves a collection of keys representing the entries in this {@link ObjectAccessor}.
     *
     * @return a collection of keys as strings
     */
    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();

        descriptor.getProperties().forEach((name, property) -> keys.add(name));

        return keys;
    }

    /**
     * Returns a string representation of this data source.
     *
     * @return a formatted string with the class name and the type of the stored source
     */
    @Override
    public String toString() {
        return "%s: %s".formatted(getShortName(this), descriptor);
    }

    abstract protected ObjectDescriptor<Object> getDescriptor(Class<?> type);

}
