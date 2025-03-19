package org.jmouse.core.bind;

import java.util.*;

import static org.jmouse.core.reflection.JavaType.forInstance;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link ObjectAccessor} implementation that provides indexed access to collections and maps.
 * <p>
 * This class allows retrieving values from lists and maps using either an index (for lists)
 * or a key (for maps).
 * </p>
 */
public class StandardTypesAccessor extends AbstractAccessor implements ObjectAccessorWrapper.Aware {

    private ObjectAccessorWrapper factory;

    /**
     * Creates a new {@link StandardTypesAccessor} with the given source object.
     *
     * @param source the underlying data source (expected to be a {@link List} or {@link Map})
     */
    public StandardTypesAccessor(Object source) {
        super(source);
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

        if (isMap()) {
            value = asMap().get(name);
        }

        return ObjectAccessor.wrap(value);
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
        Object value = null;

        if (isList()) {
            List<Object> values = asList(Object.class);
            if (values.size() > index) {
                value = asList().get(index);
            }
        } else if (isSet()) {
            Set<Object>      values   = asSet(Object.class);
            Iterator<Object> iterator = values.iterator();
            int              count    = 0;
            if (values.size() > index) {
                while (iterator.hasNext()) {
                    value = iterator.next();
                    if (count++ == index) {
                        break;
                    }
                }
            }
        }

        return ObjectAccessor.wrap(value);
    }

    /**
     * Sets a property value by name.
     *
     * @param name  the property name
     * @param value the value to set
     */
    @Override
    public void set(String name, Object value) {
        if (isMap()) {
            asMap(String.class, Object.class).put(name, value);
        }
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
        if (isList()) {
            List<Object> values = asList(Object.class);
            if (index == -1) {
                values.add(value);
            } else {
                values.set(index, value);
            }
        } else if (isSet()) {
            asSet(Object.class).add(value);
        }
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

    /**
     * Sets the ObjectAccessorWrapper.
     *
     * @param factory the factory to set
     */
    @Override
    public void setFactory(ObjectAccessorWrapper factory) {
        this.factory = factory;
    }

    /**
     * Returns the ObjectAccessorWrapper.
     *
     * @return the ObjectAccessorWrapper
     */
    @Override
    public ObjectAccessorWrapper getFactory() {
        return factory;
    }
}
