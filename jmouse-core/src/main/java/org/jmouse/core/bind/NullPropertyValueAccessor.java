package org.jmouse.core.bind;

public class NullPropertyValueAccessor extends AbstractPropertyValueAccessor {

    public NullPropertyValueAccessor() {
        super(null);
    }

    @Override
    public PropertyValueAccessor get(String name) {
        return PropertyValueAccessor.wrap(null);
    }

    @Override
    public PropertyValueAccessor get(int index) {
        return PropertyValueAccessor.wrap(null);
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
