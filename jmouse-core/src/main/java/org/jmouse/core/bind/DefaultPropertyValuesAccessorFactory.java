package org.jmouse.core.bind;

import java.util.List;

/**
 * A flexible factory for creating ObjectAccessor instances.
 * <p>
 * The factory is configured with a list of providers (strategies) that decide
 * whether they support a given source and create an accessor accordingly.
 * </p>
 */
public class DefaultPropertyValuesAccessorFactory implements PropertyValuesAccessorFactory {

    private final List<PropertyValuesAccessorProvider> providers;

    /**
     * Constructs a factory with the given list of providers.
     *
     * @param providers a list of PropertyValuesAccessorProvider instances
     */
    public DefaultPropertyValuesAccessorFactory(List<PropertyValuesAccessorProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    /**
     * Wraps the given source into a ObjectAccessor by delegating to the appropriate provider.
     * <p>
     * If no provider supports the source, an UnsupportedDataSourceException is thrown.
     * </p>
     *
     * @param source the source object to wrap
     * @return a ObjectAccessor for the given source
     * @throws UnsupportedDataSourceException if no provider can handle the source type
     */
    @Override
    public ObjectAccessor wrap(Object source) {
        ObjectAccessor instance = new NullObjectAccessor();

        for (PropertyValuesAccessorProvider provider : providers) {
            if (provider.supports(source)) {
                instance = provider.create(source);
                break;
            }
        }

        return instance;
    }
}
