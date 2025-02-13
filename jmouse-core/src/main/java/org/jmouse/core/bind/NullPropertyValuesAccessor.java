package org.jmouse.core.bind;

public class NullPropertyValuesAccessor extends AbstractPropertyValuesAccessor {

    public NullPropertyValuesAccessor() {
        super(null);
    }

    @Override
    public PropertyValuesAccessor get(String name) {
        return PropertyValuesAccessor.wrap(null);
    }

    @Override
    public PropertyValuesAccessor get(int index) {
        return PropertyValuesAccessor.wrap(null);
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
