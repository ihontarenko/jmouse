package org.jmouse.core.reflection.annotation;

import java.lang.reflect.Method;

/**
 * 🧩 Describes a mapping of annotation attributes within a merged hierarchy.
 *
 * <p>Used to resolve attribute values (possibly overridden or aliased) from an annotation
 * and its meta-annotations.
 *
 * <p>Example usage:
 * <pre>{@code
 * AnnotationAttributeMapping mapping = ...;
 * String name = mapping.getAttributeValue("name", String.class);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface AnnotationAttributeMapping {

    /**
     * 🔢 Returns the index of the attribute by name.
     *
     * @param name attribute name
     * @return index of the attribute, or -1 if not found
     */
    int getAttributeIndex(String name);

    /**
     * 🎯 Returns the resolved value for the given attribute method.
     *
     * @param method the annotation attribute method
     * @return resolved attribute value
     */
    Object getAttributeValue(Method method);

    /**
     * 🎯 Returns the resolved value for the given attribute method, cast to a specific type.
     *
     * @param method the annotation attribute method
     * @param type target type
     * @return resolved and casted attribute value
     * @param <T> generic return type
     */
    <T> T getAttributeValue(Method method, Class<T> type);

    /**
     * 🧬 Returns the resolved value of the attribute by name and type.
     *
     * @param name attribute name
     * @param type expected type
     * @return resolved and casted value, or {@code null} if not found
     * @param <T> generic return type
     */
    <T> T getAttributeValue(String name, Class<T> type);

    /**
     * 📦 Returns all resolved attributes as a key-value container.
     *
     * @return attribute container
     */
    AnnotationAttributes getAttributes();

    /**
     * ✅ Checks if the given value is the default for the specified attribute.
     *
     * @param attribute the annotation method
     * @param value value to test
     * @return true if value matches the default
     */
    boolean isDefaultValue(Method attribute, Object value);
}
