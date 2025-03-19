package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;

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
        return null;
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link ObjectAccessor}
     */
    @Override
    public ObjectAccessor get(int index) {
        return null;
    }

    /**
     * Sets a property value by name.
     *
     * @param name  the property name
     * @param value the value to set
     */
    @Override
    public void set(String name, Object value) {

    }

    /**
     * Sets a property value by index.
     *
     * @param index the property index
     * @param value the value to set
     */
    @Override
    public void set(int index, Object value) {

    }

}
