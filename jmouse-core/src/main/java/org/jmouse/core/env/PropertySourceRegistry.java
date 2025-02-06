package org.jmouse.core.env;

import java.util.Collection;

/**
 * Manages and registers property sources.
 */
public interface PropertySourceRegistry {

    /**
     * Checks a property source exist by name.
     */
    boolean hasPropertySource(String sourceName);

    /**
     * Finds a property source by name.
     */
    PropertySource<?> getPropertySource(String sourceName);

    /**
     * Registers a new property source.
     */
    void addPropertySource(PropertySource<?> propertySource);

    /**
     * Returns all registered property sources.
     */
    Collection<PropertySource<?>> getPropertySources();

}
