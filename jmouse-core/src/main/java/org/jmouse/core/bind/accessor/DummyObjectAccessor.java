package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.UnsupportedOperationException;

public class DummyObjectAccessor extends AbstractAccessor {

    /**
     * Constructs an {@link AbstractAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public DummyObjectAccessor(Object source) {
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
        throw new java.lang.UnsupportedOperationException("DUMMY DATA SOURCE");
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link ObjectAccessor}
     */
    @Override
    public ObjectAccessor get(int index) {
        throw new java.lang.UnsupportedOperationException("DUMMY DATA SOURCE");
    }

    /**
     * Sets a property value by name.
     *
     * @param name  the property name
     * @param value the value to set
     */
    @Override
    public void set(String name, Object value) {
        throw new java.lang.UnsupportedOperationException("DUMMY DATA SOURCE");
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
        throw new java.lang.UnsupportedOperationException("DUMMY DATA SOURCE");
    }
}
