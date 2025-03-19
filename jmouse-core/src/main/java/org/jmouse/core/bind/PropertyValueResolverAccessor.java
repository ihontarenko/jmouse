package org.jmouse.core.bind;

public class PropertyValueResolverAccessor extends AbstractAccessor {

    /**
     * Constructs an {@link AbstractAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public PropertyValueResolverAccessor(PropertyValueResolver source) {
        super(source);
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link ObjectAccessor}
     */
    @Override
    public ObjectAccessor get(String name) {
        return wrap(asType(PropertyValueResolver.class).getProperty(name));
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
        asType(PropertyValueResolver.class).setProperty(name, value);
    }

    /**
     * Sets a property value by index.
     *
     * @param index the property index
     * @param value the value to set
     */
    @Override
    public void set(int index, Object value) {
        throw new UnsupportedDataSourceException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(getClass().getName()));
    }

}
