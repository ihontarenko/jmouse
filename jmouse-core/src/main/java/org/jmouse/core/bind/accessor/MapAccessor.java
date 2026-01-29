package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.UnsupportedOperationException;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.bind.descriptor.structured.map.MapDescriptor;
import org.jmouse.core.bind.descriptor.structured.map.MapIntrospector;

import java.util.*;

import static org.jmouse.core.reflection.InferredType.forInstance;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link ObjectAccessor} implementation that provides indexed access to collections and maps.
 * <p>
 * This class allows retrieving values from lists and maps using either an index (for lists)
 * or a key (for maps).
 * </p>
 */
public class MapAccessor extends AbstractAccessor {

    private final MapDescriptor<Object, Object> descriptor;

    /**
     * Creates a new {@link MapAccessor} with the given source object.
     *
     * @param source the underlying data source (expected to be a {@link List} or {@link Map})
     */
    public MapAccessor(Object source) {
        super(source);
        this.descriptor = new MapIntrospector<>(asMap(Object.class, Object.class)).introspect().toDescriptor();
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by name.
     * <p>
     * If the source is a {@link Map}, this method returns the value associated with the given key.
     * </p>
     *
     * @param name the name (key) to retrieve from the map
     * @return the corresponding {@link ObjectAccessor}, or an empty data source if not found
     */
    @Override
    public ObjectAccessor get(String name) {
        Object value = null;

        if (isMap() && descriptor.getProperty(name) instanceof PropertyDescriptor<Map<Object, Object>> property) {
            value = property.getAccessor().readValue(asMap(Object.class, Object.class));
        }

        return wrap(value);
    }

    /**
     * Retrieve a nested {@link ObjectAccessor} by key object.
     *
     * <p>Intended for map-like structures where keys are not necessarily strings.</p>
     *
     * @param key map key (may be {@code null}, depending on implementation)
     * @return nested accessor for the given key
     * @throws java.lang.UnsupportedOperationException if this accessor is not map-like
     */
    @Override
    public ObjectAccessor get(Object key) {
        Object value = null;

        if (asMap() instanceof Map<?,?> map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> source = (Map<Object, Object>) map;
            if (source.containsKey(key)) {
                value = source.get(key);
            }
        }

        return wrap(value);
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by index.
     * <p>
     * If the source is a {@link List}, this method returns the value at the specified index.
     * </p>
     *
     * @param index the index to retrieve from the list
     * @return the corresponding {@link ObjectAccessor}, or an empty data source if out of bounds
     */
    @Override
    public ObjectAccessor get(int index) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(getClass().getName()));
    }

    /**
     * Sets a property value by name.
     *
     * @param name  the property name
     * @param value the value to set
     */
    @Override
    public void set(String name, Object value) {
        if (isMap() && descriptor.getProperty(name) instanceof PropertyDescriptor<Map<Object, Object>> property) {
            property.getAccessor().writeValue(asMap(Object.class, Object.class), value);
        }
    }

    /**
     * Sets a property value by index.
     *
     * <p>The default implementation throws an {@link UnsupportedOperationException},
     * indicating that indexed access is not supported unless overridden by an implementation.</p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedOperationException if indexed access is not supported
     */
    @Override
    public void set(int index, Object value) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support indexed assigning"
                        .formatted(getClass().getName()));
    }

    /**
     * Returns a string representation of this data source.
     *
     * @return a formatted string with the class name and the type of the stored source
     */
    @Override
    public String toString() {
        return "%s: %s".formatted(getShortName(this), forInstance(unwrap()));
    }


}
