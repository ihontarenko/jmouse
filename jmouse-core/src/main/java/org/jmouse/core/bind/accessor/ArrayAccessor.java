package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;

public class ArrayAccessor extends AbstractAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArrayAccessor.class);

    /**
     * Constructs an {@link AbstractAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public ArrayAccessor(Object source) {
        super(source);
    }

    /**
     * Retrieves a nested ObjectAccessor by property name.
     *
     * @param name the name of the nested data source
     * @return the nested ObjectAccessor
     */
    @Override
    public ObjectAccessor get(String name) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support named accessing"
                        .formatted(getClass().getName()));
    }

    /**
     * Retrieves a nested ObjectAccessor by index.
     *
     * @param index the index of the nested data source
     * @return the nested ObjectAccessor
     */
    @Override
    public ObjectAccessor get(int index) {
        Object value = null;

        try {
            value = Array.get(source, index);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException exception) {
            LOGGER.error("Exception thrown while accessing array by index {}.", index, exception);
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
     * <p>
     * The default implementation throws an {@link UnsupportedOperationException},
     * indicating that indexed access is not supported unless overridden.
     * </p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedOperationException if indexed access is not supported
     */
    @Override
    public void set(int index, Object value) {
        try {
            Array.set(source, index, value);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException exception) {
            LOGGER.error("Exception thrown while assigning array by index {}.", index, exception);
        }
    }
}
