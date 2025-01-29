package org.jmouse.core.env;

/**
 * Represents a source of properties with methods to retrieve property values by name.
 * <p>
 * This interface provides a mechanism to access properties from a specific source (e.g., file, environment variables)
 * and includes utility methods for checking property existence and retrieving all property names.
 * </p>
 *
 * @param <T> the type of the underlying property source
 */
public interface PropertySource<T> {

    /**
     * Returns the name of this property source.
     *
     * @return the name of the property source
     */
    String getName();

    /**
     * Returns the underlying source of this property source.
     *
     * @return the underlying source of properties
     */
    T getSource();

    /**
     * Checks if the specified property exists in this source.
     *
     * @param name the name of the property to check
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    default boolean containsProperty(String name) {
        return getProperty(name) != null;
    }

    /**
     * Retrieves the value of the specified property from this source.
     *
     * @param name the name of the property to retrieve
     * @return the value of the property, or {@code null} if the property is not found
     */
    Object getProperty(String name);

    /**
     * Returns all property names available in this source.
     *
     * @return an array of all property names
     */
    String[] getPropertyNames();
}
