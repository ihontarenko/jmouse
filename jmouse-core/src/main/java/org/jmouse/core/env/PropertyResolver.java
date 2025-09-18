package org.jmouse.core.env;

import java.util.*;

public interface PropertyResolver {

    /**
     * Retrieves the raw property value without any type conversion.
     *
     * @param name the property name
     * @return the raw property value, or {@code null} if not found
     */
    Object getRawProperty(String name);

    /**
     * Retrieves the property value as the specified type.
     *
     * @param name       the property name
     * @param targetType the target type to convert the property value to
     * @param <T>        the type of the property value
     * @return the converted property value, or {@code null} if not found
     */
    <T> T getProperty(String name, Class<T> targetType);

    /**
     * Retrieves the property value as a string.
     *
     * @param name the property name
     * @return the property value as a string, or {@code null} if not found
     */
    default String getProperty(String name) {
        return getProperty(name, String.class);
    }

    /**
     * Retrieves the property value as a string with a default value.
     *
     * @param name         the property name
     * @param defaultValue the default value to return if the property is not found
     * @return the property value as a string, or the default value if not found
     */
    default String getProperty(String name, String defaultValue) {
        String value = getProperty(name);

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * Retrieves the property value as the specified type with a default value.
     *
     * @param name         the property name
     * @param targetType   the target type to convert the property value to
     * @param defaultValue the default value to return if the property is not found
     * @param <T>          the type of the property value
     * @return the converted property value, or the default value if not found
     */
    default <T> T getProperty(String name, Class<T> targetType, T defaultValue) {
        T value = getProperty(name, targetType);

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * Retrieves the property value as the specified type, throwing an exception if not found.
     *
     * @param name       the property name
     * @param targetType the target type to convert the property value to
     * @param <T>        the type of the property value
     * @return the converted property value
     * @throws PropertyNotFoundException if the property is not found
     */
    @SuppressWarnings("unchecked")
    default <T> T getRequiredProperty(String name, Class<? super T> targetType) {
        T value = getProperty(name, (Class<T>) targetType);

        if (value == null) {
            throw new PropertyNotFoundException("Required property '%s' not found".formatted(name));
        }

        return value;
    }

    /**
     * Checks if the specified property exists in the registry.
     *
     * @param name the property name
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    default boolean containsProperty(String name) {
        return getRawProperty(name) != null;
    }

    /**
     * Retrieves a collection of all property names from all registered {@link PropertySource} instances.
     * <p>
     * This method aggregates property names from every available {@link PropertySource} into a single collection.
     * </p>
     *
     * @return a {@link Collection} containing all property names across registered sources
     */
    default Collection<String> getPropertyNames() {
        List<String> names = new ArrayList<>();

        for (PropertySource<?> source : getPropertySources()) {
            names.addAll(List.of(source.getPropertyNames()));
        }

        return names;
    }

    /**
     * Retrieves a flattened view of all properties from all registered {@link PropertySource} instances.
     * <p>
     * This method consolidates properties from all sources into a single {@link Map}, where each key represents
     * a property name and its corresponding value.
     * </p>
     *
     * @return a {@link Map} containing all properties across all registered sources
     */
    default Map<String, Object> getFlattenedProperties() {
        Map<String, Object> flattened = new HashMap<>();

        for (String propertyName : getPropertyNames()) {
            flattened.put(propertyName, getRawProperty(propertyName));
        }

        return flattened;
    }

    /**
     * Retrieves a {@link PropertySource} by its name.
     *
     * @param name the name of the property source
     * @return the corresponding {@link PropertySource}, or {@code null} if not found
     */
    PropertySource<?> getPropertySource(String name);

    /**
     * Adds a new {@link PropertySource} to the collection.
     *
     * @param propertySource the property source to add
     */
    void addPropertySource(PropertySource<?> propertySource);

    /**
     * Checks whether a property source with the given name exists.
     *
     * @param name the name of the property source
     * @return {@code true} if the property source exists, {@code false} otherwise
     */
    boolean hasPropertySource(String name);

    /**
     * Removes a {@link PropertySource} by its name.
     *
     * @param name the name of the property source to remove
     * @return {@code true} if the property source was removed, {@code false} if no such source existed
     */
    boolean removePropertySource(String name);

    /**
     * Returns all registered {@link PropertySource} instances.
     *
     * @return a collection of registered property sources
     */
    Collection<? extends PropertySource<?>> getPropertySources();



}
