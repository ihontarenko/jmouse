package org.jmouse.mvc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 🗃️ Default {@link Model} implementation backed by a {@link LinkedHashMap}.
 *
 * <p>This implementation preserves attribute insertion order
 * and provides standard model operations to add, set, get,
 * and remove attributes.
 *
 * <p>Example usage:
 * <pre>{@code
 * Model model = new DefaultModel()
 *     .addAttribute("user", userObject)
 *     .setAttribute("count", 42);
 * Object user = model.getAttribute("user");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @since 1.0
 */
public class DefaultModel implements Model {

    private final Map<String, Object> attributes = new LinkedHashMap<>();

    /**
     * Adds the given attribute only if it does not already exist.
     *
     * @param name  the attribute name; must not be null
     * @param value the attribute value
     * @return this model instance for chaining
     * @throws NullPointerException if {@code name} is null
     */
    @Override
    public Model addAttribute(String name, Object value) {
        Objects.requireNonNull(name, "Attribute name must not be null");
        attributes.putIfAbsent(name, value);
        return this;
    }

    /**
     * Sets (adds or replaces) the attribute with the given name.
     *
     * @param name  the attribute name; must not be null
     * @param value the attribute value
     * @return this model instance for chaining
     * @throws NullPointerException if {@code name} is null
     */
    @Override
    public Model setAttribute(String name, Object value) {
        Objects.requireNonNull(name, "Attribute name must not be null");
        attributes.put(name, value);
        return this;
    }

    /**
     * Retrieves the attribute value by name.
     *
     * @param name the attribute name
     * @return the attribute value, or {@code null} if not present
     */
    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Removes the attribute with the given name.
     *
     * @param name the attribute name
     * @return this model instance for chaining
     */
    @Override
    public Model removeAttribute(String name) {
        attributes.remove(name);
        return this;
    }

    /**
     * Checks whether the model contains an attribute with the given name.
     *
     * @param name the attribute name
     * @return {@code true} if attribute exists, {@code false} otherwise
     */
    @Override
    public boolean containsAttribute(String name) {
        return attributes.containsKey(name);
    }

    /**
     * Returns an unmodifiable view of all attributes in the model.
     *
     * @return unmodifiable map of attribute name-value pairs
     */
    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Returns the number of attributes contained in the model.
     *
     * @return attribute count
     */
    @Override
    public int size() {
        return attributes.size();
    }

    /**
     * Returns a string representation of the model and its attributes.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "Model" + attributes;
    }

}
