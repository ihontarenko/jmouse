package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.bind.UnsupportedOperationException;
import org.jmouse.core.env.MapPropertySource;
import org.jmouse.core.env.PropertyResolver;
import org.jmouse.core.env.PropertySource;

import java.util.Map;

/**
 * A {@link ObjectAccessor} implementation that wraps a {@link PropertyResolver},
 * converting its properties into a hierarchical structure.
 */
public class PropertyResolverAccessor implements ObjectAccessor, ObjectAccessorWrapper.Aware {

    private final MapAccessor           delegate;

    /**
     * Creates a new {@link PropertyResolverAccessor} by extracting properties
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
    public PropertyResolverAccessor(PropertyResolver resolver) {
        String              temporaryPropertySourceName = "temporary-property-source";
        String              temporaryKeyName            = "temporary-key";
        Map<String, Object> properties                  = Map.of(temporaryKeyName, resolver.getFlattenedProperties());
        PropertySource<?>   propertySource              = new MapPropertySource(temporaryPropertySourceName, properties);

        resolver.addPropertySource(propertySource);
        Map<String, Object> hierarchical = resolver.getRequiredProperty(temporaryKeyName, Map.class);
        resolver.removePropertySource(temporaryPropertySourceName);

        this.delegate = new MapAccessor(hierarchical);
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
     * Creates a {@link ObjectAccessor} instance from the given source object.
     *
     * @param source the source object
     * @return a {@link ObjectAccessor} instance wrapping the source
     */
    @Override
    public ObjectAccessor wrap(Object source) {
        return delegate.wrap(source);
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by name.
     *
     * @param name the property key
     * @return the corresponding {@link ObjectAccessor}, or an empty source if not found
     */
    @Override
    public ObjectAccessor get(String name) {
        return delegate.get(name);
    }

    /**
     * Retrieves a nested {@link ObjectAccessor} by index.
     *
     * @param index the index within a collection-based property
     * @return the corresponding {@link ObjectAccessor}, or an empty source if not found
     */
    @Override
    public ObjectAccessor get(int index) {
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
     * <p>The default implementation throws an {@link UnsupportedOperationException},
     * indicating that indexed access is not supported unless overridden by an implementation.</p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedOperationException if indexed access is not supported
     */
    @Override
    public void set(int index, Object value) {
        delegate.set(index, value);
    }

    /**
     * Sets the ObjectAccessorWrapper.
     *
     * @param wrapper the factory to set
     */
    @Override
    public void setWrapper(ObjectAccessorWrapper wrapper) {
        this.delegate.setWrapper(wrapper);;
    }

    /**
     * Returns the ObjectAccessorWrapper.
     *
     * @return the ObjectAccessorWrapper
     */
    @Override
    public ObjectAccessorWrapper getWrapper() {
        return delegate.getWrapper();
    }
}
