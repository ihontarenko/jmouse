package org.jmouse.core.access.accessor;

import org.jmouse.core.access.AbstractAccessor;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.UnsupportedOperationException;

import java.util.*;

import static org.jmouse.core.reflection.InferredType.forInstance;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link ObjectAccessor} implementation that provides indexed access to collections and maps.
 * <p>
 * This class allows retrieving values from lists and maps using either an index (for lists)
 * or a key (for maps).
 * </p>
 *
 * <p>A property's name is its key's text: a key that is that text itself, else the key whose {@code toString()} it is. The map
 * is asked as it stands at each read — nothing of it is introspected up front, so wrapping a map costs nothing, however many
 * entries it holds, and a key put after wrapping is read like any other.</p>
 */
public class MapAccessor extends AbstractAccessor {

    /**
     * Creates a new {@link MapAccessor} with the given source object.
     *
     * @param source the underlying data source (expected to be a {@link List} or {@link Map})
     */
    public MapAccessor(Object source) {
        super(source);
    }

    /**
     * The key a property's name stands for: the name itself where the map holds it, else the key written as the name; none
     * where the map holds neither.
     */
    private Optional<Object> keyNamed(Map<Object, Object> map, String name) {
        if (map.containsKey(name)) {
            return Optional.of(name);
        }

        for (Object key : map.keySet()) {
            if (key != null && !(key instanceof String) && key.toString().equals(name)) {
                return Optional.of(key);
            }
        }

        return Optional.empty();
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
        return wrap(read(name));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the entry straight out of the map, so nothing is allocated to carry a value the caller
     * is about to take out again.</p>
     */
    @Override
    public Object read(String name) {
        if (!isMap()) {
            return null;
        }

        Map<Object, Object> map = asMap(Object.class, Object.class);
        return keyNamed(map, name).map(map::get).orElse(null);
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
        // Only a key the map holds is set: a property is one of its entries, never a new one.
        if (isMap()) {
            Map<Object, Object> map = asMap(Object.class, Object.class);
            keyNamed(map, name).ifPresent(key -> map.put(key, value));
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
