package org.jmouse.core.env;

/**
 * Defines methods for resolving properties from a {@link PropertySourceRegistry}.
 * <p>
 * This interface provides methods to retrieve property values in various types, check for property existence,
 * and handle default values or required properties.
 * </p>
 */
public interface PropertyResolver {

    /**
     * Sets the {@link PropertySourceRegistry} to be used for resolving properties.
     *
     * @param registry the property source registry
     */
    void setRegistry(PropertySourceRegistry registry);

    /**
     * Returns the current {@link PropertySourceRegistry} used by this resolver.
     *
     * @return the property source registry
     */
    PropertySourceRegistry getRegistry();

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
    default <T> T getRequiredProperty(String name, Class<T> targetType) {
        T value = getProperty(name, targetType);

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
}
