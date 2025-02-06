package org.jmouse.core.env;

import org.jmouse.util.Sorter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DefaultPropertySourceRegistry implements PropertySourceRegistry {

    private final Map<String, PropertySource<?>> sources = new HashMap<>();

    /**
     * Checks a property source exist by name.
     */
    @Override
    public boolean hasPropertySource(String sourceName) {
        return sources.containsKey(sourceName);
    }

    /**
     * Finds a property source by name.
     */
    @Override
    public PropertySource<?> getPropertySource(String sourceName) {
        return sources.get(sourceName);
    }

    /**
     * Registers a new property source.
     */
    @Override
    public void addPropertySource(PropertySource<?> propertySource) {
        sources.put(propertySource.getName(), propertySource);
    }

    /**
     * Returns all registered property sources.
     */
    @Override
    public Collection<PropertySource<?>> getPropertySources() {
        return sources.values().stream().sorted(Sorter.PRIORITY_COMPARATOR).toList();
    }

}
