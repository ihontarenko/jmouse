package org.jmouse.core.annotation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared context used during annotation processing. ⚙️
 *
 * <p>
 * Provides a lightweight attribute storage that processors may use to
 * exchange data, cache intermediate results, or expose shared objects.
 * The context is typically created once per processing session and
 * passed to all {@link AnnotationProcessor} implementations.
 * </p>
 */
public interface AnnotationProcessingContext {

    /**
     * Returns mutable attributes map.
     *
     * <p>
     * Acts as a simple key–value storage for processors participating
     * in the same processing pipeline.
     * </p>
     */
    Map<String, Object> attributes();

    /**
     * Returns attribute value by name.
     *
     * @param name attribute key
     * @param <T>  expected type
     *
     * @return attribute value or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    default <T> T attribute(String name) {
        return (T) attributes().get(name);
    }

    /**
     * Stores attribute value.
     *
     * @param name  attribute key
     * @param value attribute value
     */
    default void setAttribute(String name, Object value) {
        attributes().put(name, value);
    }

    /**
     * Default in-memory {@link AnnotationProcessingContext} implementation. 🧱
     */
    class Default implements AnnotationProcessingContext {

        private final Map<String, Object> attributes = new LinkedHashMap<>();

        @Override
        public Map<String, Object> attributes() {
            return attributes;
        }
    }
}