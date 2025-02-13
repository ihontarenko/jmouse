package org.jmouse.core.bind;

public class DummyPropertyValuesAccessor extends AbstractPropertyValuesAccessor {

    /**
     * Constructs an {@link AbstractPropertyValuesAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public DummyPropertyValuesAccessor(Object source) {
        super(source);
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link PropertyValuesAccessor}
     */
    @Override
    public PropertyValuesAccessor get(String name) {
        throw new UnsupportedOperationException("DUMMY DATA SOURCE");
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link PropertyValuesAccessor}
     */
    @Override
    public PropertyValuesAccessor get(int index) {
        throw new UnsupportedOperationException("DUMMY DATA SOURCE");
    }

}
