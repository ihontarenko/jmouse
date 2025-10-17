package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.UnsupportedOperationException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jmouse.core.reflection.InferredType.forInstance;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link ObjectAccessor} implementation that provides indexed access to collections and maps.
 * <p>
 * This class allows retrieving values from lists and maps using either an index (for lists)
 * or a key (for maps).
 * </p>
 */
public class CollectionAccessor extends AbstractAccessor {

    /**
     * Creates a new {@link CollectionAccessor} with the given source object.
     *
     * @param source the underlying data source (expected to be a {@link List} or {@link Map})
     */
    public CollectionAccessor(Object source) {
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
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support named accessing"
                        .formatted(getClass().getName()));
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
            int              count    = 0;
            Iterator<Object> iterator = values.iterator();
            if (values.size() > index) {
                while (iterator.hasNext()) {
                    value = iterator.next();
                    if (count++ == index) {
                        break;
                    }
                }
            }
        }

        return wrap(value);
    }

    /**
     * Sets a property value by name.
     *
     * @param name  the property name
     * @param value the value to set
     */
    @Override
    public void set(String name, Object value) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support named assigning"
                        .formatted(getClass().getName()));
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


}
