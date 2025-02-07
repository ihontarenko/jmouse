package org.jmouse.core.bind;

public class DummyDataSource extends AbstractDataSource {

    /**
     * Constructs an {@link AbstractDataSource} with the given source object.
     *
     * @param source the source object to wrap
     */
    public DummyDataSource(Object source) {
        super(source);
    }

    /**
     * Retrieves a nested {@link DataSource} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link DataSource}
     */
    @Override
    public DataSource get(String name) {
        throw new UnsupportedOperationException("DUMMY DATA SOURCE");
    }

    /**
     * Retrieves a nested {@link DataSource} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link DataSource}
     */
    @Override
    public DataSource get(int index) {
        throw new UnsupportedOperationException("DUMMY DATA SOURCE");
    }

}
