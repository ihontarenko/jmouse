package org.jmouse.core.access;

/**
 * A high‑level abstraction for resolving property values from an object.
 * <p>
 * This interface builds upon the low‑level ObjectAccessor abstraction by providing additional
 * functionality such as virtual property resolution, nested property navigation via structured property paths,
 * and type conversion if needed.
 * </p>
 */
public interface PropertyValueResolver {

    /**
     * Retrieves the underlying ObjectAccessor that provides low‑level access to the data source.
     *
     * @return the underlying ObjectAccessor
     */
    ObjectAccessor getAccessor();

    /**
     * Retrieves the value of the specified property.
     * <p>
     * This method first attempts to retrieve the property directly from the underlying ObjectAccessor.
     * If the value is {@code null} and a VirtualPropertyResolver is available, it will attempt to resolve the
     * property virtually.
     * </p>
     *
     * @param name the name of the property to retrieve
     * @return the resolved property value, or {@code null} if not found
     */
    Object getProperty(String name);

    /**
     * Sets the value of the specified property.
     *
     * @param name the name of the property to set
     * @param value        the value to assign to the property
     */
    default void setProperty(String name, Object value) {
        getAccessor().inject(name, value);
    }

    /**
     * Resolves a nested property value using a structured property path.
     * <p>
     * This method navigates through the nested structure (using the underlying ObjectAccessor’s navigation
     * capabilities) and returns the unwrapped value of the final nested property.
     * </p>
     *
     * @param path the structured property path (e.g. "address.street.name")
     * @return the resolved property value, or {@code null} if not found
     * @throws NumberFormatException if an indexed path segment contains a non‑numeric value
     */
    default Object resolve(String path) {
        return getAccessor().navigate(path).unwrap();
    }
}
