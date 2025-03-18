package org.jmouse.el.evaluation;

import org.jmouse.core.bind.AbstractObjectAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.UnsupportedDataSourceException;

public class ScopedChainValuesAccessor extends AbstractObjectAccessor {

    /**
     * Constructs an {@link AbstractObjectAccessor} with the given source object.
     *
     * @param chain the source object to wrap
     */
    public ScopedChainValuesAccessor(ScopedChain chain) {
        super(chain);
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link ObjectAccessor}
     */
    @Override
    public ObjectAccessor get(String name) {
        ScopedChain chain = asType(ScopedChain.class);
        Object      value = null;

        if (chain.contains(name)) {
            value = chain.getValue(name);
        }

        return ObjectAccessor.wrap(value);
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link ObjectAccessor}
     */
    @Override
    public ObjectAccessor get(int index) {
        throw new UnsupportedDataSourceException(
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
        asType(ScopedChain.class).setValue(name, value);
    }

    @Override
    public void set(int index, Object value) {
        throw new UnsupportedDataSourceException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(getClass().getName()));
    }

}
