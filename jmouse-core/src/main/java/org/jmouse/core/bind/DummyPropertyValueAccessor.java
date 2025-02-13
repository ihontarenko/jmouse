package org.jmouse.core.bind;

public class DummyPropertyValueAccessor extends AbstractPropertyValueAccessor {

    /**
     * Constructs an {@link AbstractPropertyValueAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public DummyPropertyValueAccessor(Object source) {
        super(source);
    }

    /**
     * Retrieves a nested {@link PropertyValueAccessor} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link PropertyValueAccessor}
     */
    @Override
    public PropertyValueAccessor get(String name) {
        throw new UnsupportedOperationException("DUMMY DATA SOURCE");
    }

    /**
     * Retrieves a nested {@link PropertyValueAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link PropertyValueAccessor}
     */
    @Override
    public PropertyValueAccessor get(int index) {
        throw new UnsupportedOperationException("DUMMY DATA SOURCE");
    }

}
