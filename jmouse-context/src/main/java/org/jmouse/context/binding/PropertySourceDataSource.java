package org.jmouse.context.binding;

import org.jmouse.core.env.PropertyResolver;

public class PropertySourceDataSource extends JavaTypeDataSource {

    /**
     * Constructs an {@link AbstractDataSource} with the given source object.
     *
     * @param source the source object to wrap
     */
    public PropertySourceDataSource(PropertyResolver source) {
        super(null);
    }

    /**
     * Retrieves a nested {@link DataSource} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link DataSource}
     */
    @Override
    public DataSource get(String name) {
        return DataSource.of(getPropertyResolver().getProperty(name));
    }

    /**
     * Retrieves a nested {@link DataSource} using a {@link NamePath}.
     *
     * @param name the structured name path
     * @return the nested {@link DataSource}
     */
    @Override
    public DataSource get(NamePath name) {
        return get(name.path());
    }

    /**
     * Retrieves a nested {@link DataSource} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link DataSource}
     */
    @Override
    public DataSource get(int index) {
        throw new UnsupportedOperationException("Numeric indexed property not supported");
    }

    public PropertyResolver getPropertyResolver() {
        return as(PropertyResolver.class);
    }

}
