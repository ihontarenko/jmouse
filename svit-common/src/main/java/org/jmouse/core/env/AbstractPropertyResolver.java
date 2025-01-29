package org.jmouse.core.env;

/**
 * Abstract base class for {@link PropertyResolver} implementations.
 * <p>
 * This class provides a common implementation for managing a {@link PropertySourceRegistry}
 * and resolving raw property values from registered property sources.
 * </p>
 */
abstract public class AbstractPropertyResolver implements PropertyResolver {

    private PropertySourceRegistry registry;

    /**
     * Constructs an {@link AbstractPropertyResolver} with the specified {@link PropertySourceRegistry}.
     *
     * @param registry the property source registry to use for resolving properties
     */
    public AbstractPropertyResolver(PropertySourceRegistry registry) {
        this.registry = registry;
    }

    /**
     * Sets the {@link PropertySourceRegistry} to be used for resolving properties.
     *
     * @param registry the property source registry
     */
    @Override
    public void setRegistry(PropertySourceRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the current {@link PropertySourceRegistry} used by this resolver.
     *
     * @return the property source registry
     */
    @Override
    public PropertySourceRegistry getRegistry() {
        return registry;
    }

    /**
     * Resolves the raw property value for the specified property name.
     * <p>
     * Iterates through all registered {@link PropertySource}s in the registry to find the first source
     * that contains the property.
     * </p>
     *
     * @param name the name of the property to resolve
     * @return the raw property value, or {@code null} if not found
     */
    @Override
    public Object getRawProperty(String name) {
        Object value = null;

        for (PropertySource<?> source : registry.getPropertySources()) {
            if (source.containsProperty(name)) {
                value = source.getProperty(name);
                break;
            }
        }

        return value;
    }
}
