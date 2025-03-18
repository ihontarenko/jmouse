package org.jmouse.core.bind;

import org.jmouse.core.env.MapPropertySource;
import org.jmouse.core.env.PropertyResolver;
import org.jmouse.core.env.PropertySource;

import java.util.Map;

/**
 * A {@link PropertyValuesAccessor} implementation that wraps a {@link PropertyResolver},
 * converting its properties into a hierarchical structure.
 */
public class PropertyResolverDataSource implements PropertyValuesAccessor {

    private final StandardTypesPropertyValuesAccessor delegate;

    /**
     * Creates a new {@link PropertyResolverDataSource} by extracting properties
     * from the given {@link PropertyResolver} and converting them into a structured
     * hierarchical format.
     * <p>
     * <strong>DirectAccess Details:</strong>
     * <ul>
     *   <li>A temporary property source is created to store all flattened properties.</li>
     *   <li>This temporary property source is added to the resolver under a unique key.</li>
     *   <li>The properties are then retrieved as a {@code Map}, forcing the execution
     *       of the {@code HierarchicalMapConverter}.</li>
     *   <li>Once converted, the temporary property source is removed to avoid pollution.</li>
     * </ul>
     *
     * @param resolver the {@link PropertyResolver} to extract properties from
     */
    public PropertyResolverDataSource(PropertyResolver resolver) {
        String              temporaryPropertySourceName = "temporary-property-source";
        String              temporaryKeyName            = "temporary-key";
        Map<String, Object> properties                  = Map.of(temporaryKeyName, resolver.getFlattenedProperties());
        PropertySource<?>   propertySource              = new MapPropertySource(temporaryPropertySourceName, properties);

        resolver.addPropertySource(propertySource);
        Map<String, Object> hierarchical = resolver.getRequiredProperty(temporaryKeyName, Map.class);
        resolver.removePropertySource(temporaryPropertySourceName);

        this.delegate = new StandardTypesPropertyValuesAccessor(hierarchical);
    }

    /**
     * Returns the underlying source object.
     *
     * @return the extracted hierarchical data
     */
    @Override
    public Object unwrap() {
        return delegate.unwrap();
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by name.
     *
     * @param name the property key
     * @return the corresponding {@link PropertyValuesAccessor}, or an empty source if not found
     */
    @Override
    public PropertyValuesAccessor get(String name) {
        return delegate.get(name);
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by index.
     *
     * @param index the index within a collection-based property
     * @return the corresponding {@link PropertyValuesAccessor}, or an empty source if not found
     */
    @Override
    public PropertyValuesAccessor get(int index) {
        return delegate.get(index);
    }

    /**
     * Sets a property value by name.
     *
     * @param name  the property name
     * @param value the value to set
     */
    @Override
    public void set(String name, Object value) {
        delegate.set(name, value);
    }

    /**
     * Sets a property value by index.
     *
     * <p>The default implementation throws an {@link UnsupportedDataSourceException},
     * indicating that indexed access is not supported unless overridden by an implementation.</p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedDataSourceException if indexed access is not supported
     */
    @Override
    public void set(int index, Object value) {
        delegate.set(index, value);
    }
}
