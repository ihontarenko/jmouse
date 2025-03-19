package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.AbstractAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.UnsupportedDataSourceException;

public class NullObjectAccessor extends AbstractAccessor {

    public static final NullObjectAccessor INSTANCE = new NullObjectAccessor();

    public NullObjectAccessor() {
        super(null);
    }

    @Override
    public ObjectAccessor get(String name) {
        return INSTANCE;
    }

    @Override
    public ObjectAccessor get(int index) {
        return INSTANCE;
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
     * <p>The default implementation throws an {@link UnsupportedDataSourceException},
     * indicating that indexed access is not supported unless overridden by an implementation.</p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedDataSourceException if indexed access is not supported
     */
    @Override
    public void set(int index, Object value) {

    }

    /**
     * Retrieves the underlying source object.
     *
     * @return the original source object
     */
    @Override
    public Object unwrap() {
        return null;
    }

    /**
     * Returns the raw object stored in the data source.
     *
     * @return the raw object
     */
    @Override
    public Object asObject() {
        return unwrap();
    }

    /**
     * Returns the type of the data source.
     *
     * @return the {@link Class} representing the data source type
     */
    @Override
    public Class<?> getDataType() {
        return void.class;
    }
}
