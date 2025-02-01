package org.jmouse.context.binding;

public class NullValueDataSource extends AbstractDataSource {

    public NullValueDataSource() {
        super(null);
    }

    @Override
    public DataSource get(String name) {
        return DataSource.of(null);
    }

    @Override
    public DataSource get(int index) {
        return DataSource.of(null);
    }

    /**
     * Retrieves the underlying source object.
     *
     * @return the original source object
     */
    @Override
    public Object getSource() {
        return null;
    }

    /**
     * Returns the raw object stored in the data source.
     *
     * @return the raw object
     */
    @Override
    public Object getRaw() {
        return getSource();
    }

    /**
     * Returns the type of the data source.
     *
     * @return the {@link Class} representing the data source type
     */
    @Override
    public Class<?> getType() {
        return void.class;
    }
}
