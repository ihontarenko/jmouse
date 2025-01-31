package org.jmouse.context.binding;

public class NullValueDataSource extends AbstractDataSource {

    public NullValueDataSource() {
        super(null);
    }

    @Override
    public DataSource get(String name) {
        throw new UnsupportedOperationException("NULL DATA SOURCE");
    }

    @Override
    public DataSource get(int index) {
        throw new UnsupportedOperationException("NULL DATA SOURCE");
    }

    /**
     * Retrieves the underlying source object.
     *
     * @return the original source object
     */
    @Override
    public Object getSource() {
        throw new UnsupportedOperationException("NULL DATA SOURCE");
    }

    /**
     * Returns the raw object stored in the data source.
     *
     * @return the raw object
     */
    @Override
    public Object getRaw() {
        throw new UnsupportedOperationException("NULL DATA SOURCE");
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
