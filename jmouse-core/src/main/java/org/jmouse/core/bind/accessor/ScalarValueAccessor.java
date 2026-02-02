package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.UnsupportedOperationException;

public class ScalarValueAccessor extends AbstractAccessor {

    /**
     * Constructs an {@link AbstractAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public ScalarValueAccessor(Object source) {
        super(source);
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link ObjectAccessor}
     */
    @Override
    public ObjectAccessor get(String name) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support named accessing"
                        .formatted(getClass().getName()));
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link ObjectAccessor}
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
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support named assigning"
                        .formatted(getClass().getName()));
    }

    /**
     * Sets a property value by index.
     *
     * @param index the property index
     * @param value the value to set
     */
    @Override
    public void set(int index, Object value) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support indexed assigning"
                        .formatted(getClass().getName()));
    }

}
