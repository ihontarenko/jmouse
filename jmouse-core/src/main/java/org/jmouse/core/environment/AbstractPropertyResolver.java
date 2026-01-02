package org.jmouse.core.environment;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract base class for {@link PropertyResolver} implementations.
 * <p>
 * Provides a registry of {@link PropertySource} instances and implements common property resolution logic.
 * </p>
 */
abstract public class AbstractPropertyResolver implements PropertyResolver {

    private final Map<String, PropertySource<?>> sources = new LinkedHashMap<>();

    /**
     * Retrieves the raw property value for the specified property name.
     * <p>
     * Iterates through all registered {@link PropertySource}s to locate the first one containing the property.
     * </p>
     *
     * @param name the name of the property to resolve
     * @return the raw property value, or {@code null} if not found
     */
    @Override
    public Object getRawProperty(String name) {
        for (PropertySource<?> source : getPropertySources()) {
            if (source.containsProperty(name)) {
                return source.getProperty(name);
            }
        }
        return null;
    }

    /**
     * Retrieves a {@link PropertySource} by its name.
     *
     * @param name the name of the property source
     * @return the corresponding {@link PropertySource}, or {@code null} if not found
     */
    @Override
    public PropertySource<?> getPropertySource(String name) {
        return sources.get(name);
    }

    /**
     * Registers a new {@link PropertySource}.
     *
     * @param propertySource the property source to add
     */
    @Override
    public void addPropertySource(PropertySource<?> propertySource) {
        sources.put(propertySource.getName(), propertySource);
    }

    /**
     * Checks whether a {@link PropertySource} with the given name exists.
     *
     * @param name the name of the property source
     * @return {@code true} if the property source exists, {@code false} otherwise
     */
    @Override
    public boolean hasPropertySource(String name) {
        return sources.containsKey(name);
    }

    /**
     * Removes a {@link PropertySource} by its name.
     *
     * @param name the name of the property source to remove
     * @return {@code true} if the property source was removed, {@code false} if it did not exist
     */
    @Override
    public boolean removePropertySource(String name) {
        return sources.remove(name) != null;
    }

    /**
     * Retrieves all registered {@link PropertySource} instances.
     *
     * @return a collection of registered property sources
     */
    @Override
    public Collection<? extends PropertySource<?>> getPropertySources() {
        return sources.values();
    }
}
