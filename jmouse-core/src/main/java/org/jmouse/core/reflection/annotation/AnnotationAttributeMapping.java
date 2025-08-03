package org.jmouse.core.reflection.annotation;

import java.util.Map;

/**
 * 📌 Resolves attribute mappings from one annotation to another.
 */
public interface AnnotationAttributeMapping {

    int getAttributeIndex(String name);

    <T> T getAttributeValue(int index, Class<T> type);

    default <T> T getAttributeValue(String name, Class<T> type) {
        int index = getAttributeIndex(name);

        if (index != -1) {
            return getAttributeValue(index, type);
        }

        return null;
    }

    /**
     * Maps attributes from source to target annotation type.
     *
     * @param annotation the annotation instance to synthesize
     * @return map of synthesized attributes
     */
    Map<String, Object> resolveMappings();
}