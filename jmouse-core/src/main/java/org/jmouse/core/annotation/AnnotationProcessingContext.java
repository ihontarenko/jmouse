package org.jmouse.core.annotation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared context for annotation processing. ⚙️
 */
public interface AnnotationProcessingContext {

    /**
     * Returns mutable attributes map.
     */
    Map<String, Object> attributes();

    /**
     * Returns attribute by name.
     */
    @SuppressWarnings("unchecked")
    default <T> T attribute(String name) {
        return (T) attributes().get(name);
    }

    /**
     * Stores attribute value.
     */
    default void setAttribute(String name, Object value) {
        attributes().put(name, value);
    }

    class Default implements AnnotationProcessingContext {

        private final Map<String, Object> attributes = new LinkedHashMap<>();

        @Override
        public Map<String, Object> attributes() {
            return attributes;
        }
    }

}