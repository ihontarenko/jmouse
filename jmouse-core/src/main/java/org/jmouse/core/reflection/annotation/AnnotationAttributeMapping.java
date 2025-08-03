package org.jmouse.core.reflection.annotation;

import java.lang.reflect.Method;

/**
 * 📌 Resolves attribute mappings from one annotation to another.
 *
 * <p>Used to unify access to native or aliased annotation attributes.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface AnnotationAttributeMapping {

    /**
     * 🔢 Returns the index of the attribute by its name.
     */
    int getAttributeIndex(String name);

    /**
     * 🎯 Resolves the attribute value by index.
     *
     * @param index the index of the attribute
     * @param type  expected result type
     */
    <T> T getAttributeValue(int index, Class<T> type);

    /**
     * 🔍 Resolves the attribute value by name.
     *
     * @param name attribute name
     * @param type expected result type
     */
    <T> T getAttributeValue(String name, Class<T> type);

    /**
     * ✅ Checks if the given value equals the method’s default.
     */
    boolean isDefaultValue(Method attribute, Object value);
}
