package org.jmouse.web.mvc;

import java.util.Map;

/**
 * Represents a container for model attributes used in view rendering 🧩
 * <p>
 * Supports adding, retrieving, and removing named attributes.
 * </p>
 *
 * <pre>{@code
 * Model model = new DefaultModel();
 * model.addAttribute("user", user);
 * model.setAttribute("role", "admin");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public interface Model {

    /**
     * Adds an attribute only if it doesn't exist yet.
     *
     * @param name  the name of the attribute
     * @param value the value to associate
     * @return this model (for chaining)
     */
    Model addAttribute(String name, Object value);

    /**
     * Sets (or replaces) the attribute with the given name.
     *
     * @param name  the name of the attribute
     * @param value the value to associate
     * @return this model (for chaining)
     */
    Model setAttribute(String name, Object value);

    /**
     * Gets the attribute by name.
     *
     * @param name the attribute name
     * @return the associated value, or {@code null} if not present
     */
    Object getAttribute(String name);

    /**
     * Removes the attribute with the given name.
     *
     * @param name the attribute name
     * @return this model (for chaining)
     */
    Model removeAttribute(String name);

    /**
     * Checks if an attribute exists.
     *
     * @param name the attribute name
     * @return {@code true} if present
     */
    boolean containsAttribute(String name);

    /**
     * Returns all model attributes as an unmodifiable map.
     *
     * @return attribute map
     */
    Map<String, Object> getAttributes();

    /**
     * Returns the number of stored attributes.
     *
     * @return count
     */
    int size();
}


