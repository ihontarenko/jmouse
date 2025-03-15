package org.jmouse.core.bind;

import java.util.*;

import static org.jmouse.core.reflection.JavaType.forInstance;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link PropertyValuesAccessor} implementation that provides indexed access to collections and maps.
 * <p>
 * This class allows retrieving values from lists and maps using either an index (for lists)
 * or a key (for maps).
 * </p>
 */
public class StandardPropertyValuesAccessor extends AbstractPropertyValuesAccessor {

    /**
     * Creates a new {@link StandardPropertyValuesAccessor} with the given source object.
     *
     * @param source the underlying data source (expected to be a {@link List} or {@link Map})
     */
    public StandardPropertyValuesAccessor(Object source) {
        super(source);
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by name.
     * <p>
     * If the source is a {@link Map}, this method returns the value associated with the given key.
     * </p>
     *
     * @param name the name (key) to retrieve from the map
     * @return the corresponding {@link PropertyValuesAccessor}, or an empty data source if not found
     */
    @Override
    public PropertyValuesAccessor get(String name) {
        Object value = null;

        if (isMap()) {
            value = asMap().get(name);
        }

        return PropertyValuesAccessor.wrap(value);
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by index.
     * <p>
     * If the source is a {@link List}, this method returns the value at the specified index.
     * </p>
     *
     * @param index the index to retrieve from the list
     * @return the corresponding {@link PropertyValuesAccessor}, or an empty data source if out of bounds
     */
    @Override
    public PropertyValuesAccessor get(int index) {
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

        return PropertyValuesAccessor.wrap(value);
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
