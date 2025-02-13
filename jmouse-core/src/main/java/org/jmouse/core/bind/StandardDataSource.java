package org.jmouse.core.bind;

import java.util.List;
import java.util.Map;

import static org.jmouse.core.reflection.JavaType.forInstance;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link PropertyValueAccessor} implementation that provides indexed access to collections and maps.
 * <p>
 * This class allows retrieving values from lists and maps using either an index (for lists)
 * or a key (for maps).
 * </p>
 */
public class StandardDataSource extends AbstractDataSource {

    /**
     * Creates a new {@link StandardDataSource} with the given source object.
     *
     * @param source the underlying data source (expected to be a {@link List} or {@link Map})
     */
    public StandardDataSource(Object source) {
        super(source);
    }

    /**
     * Retrieves a nested {@link PropertyValueAccessor} by name.
     * <p>
     * If the source is a {@link Map}, this method returns the value associated with the given key.
     * </p>
     *
     * @param name the name (key) to retrieve from the map
     * @return the corresponding {@link PropertyValueAccessor}, or an empty data source if not found
     */
    @Override
    public PropertyValueAccessor get(String name) {
        Object value = null;

        if (isMap()) {
            value = asMap().get(name);
        }

        return PropertyValueAccessor.wrap(value);
    }

    /**
     * Retrieves a nested {@link PropertyValueAccessor} by index.
     * <p>
     * If the source is a {@link List}, this method returns the value at the specified index.
     * </p>
     *
     * @param index the index to retrieve from the list
     * @return the corresponding {@link PropertyValueAccessor}, or an empty data source if out of bounds
     */
    @Override
    public PropertyValueAccessor get(int index) {
        Object value = null;

        if (isList()) {
            List<Object> list = asList(Object.class);
            if (list.size() > index) {
                value = asList().get(index);
            }
        }

        return PropertyValueAccessor.wrap(value);
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
