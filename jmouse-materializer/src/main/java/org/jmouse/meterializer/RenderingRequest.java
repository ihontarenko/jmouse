package org.jmouse.meterializer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.jmouse.core.Verify.nonNull;

/**
 * Rendering request context used during template materialization.
 *
 * <p>Provides a lightweight attribute map for passing runtime data
 * (submission state, helpers, configuration, etc.) through rendering.</p>
 */
public class RenderingRequest {

    /**
     * Request attributes available during rendering.
     */
    private final Map<String, Object> attributes;

    /**
     * Creates an empty rendering request.
     */
    public RenderingRequest() {
        this.attributes = new LinkedHashMap<>();
    }

    /**
     * Creates a request with predefined attributes.
     *
     * @param attributes attribute map
     */
    protected RenderingRequest(Map<String, Object> attributes) {
        this.attributes = nonNull(attributes, "attributes");
    }

    /**
     * Adds or replaces an attribute.
     *
     * <p>{@code null} values are ignored.</p>
     *
     * @param key attribute name
     * @param value attribute value
     * @return this request instance
     */
    public RenderingRequest setAttribute(String key, Object value) {
        nonNull(key, "key");

        if (value == null) {
            return this;
        }

        attributes.put(key, value);
        return this;
    }

    /**
     * Returns the underlying attributes map.
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    /**
     * Finds an attribute by name.
     *
     * @param attributeName attribute key
     * @return optional attribute value
     */
    public Optional<Object> findAttribute(String attributeName) {
        nonNull(attributeName, "attributeName");
        Object value = attributes.get(attributeName);
        return Optional.ofNullable(value);
    }

    /**
     * Finds an attribute and casts it to the expected type.
     *
     * @param key attribute name
     * @param type expected type
     * @return optional typed attribute
     *
     * @throws IllegalArgumentException if attribute exists but type mismatch occurs
     */
    public <T> Optional<T> findAttribute(String key, Class<T> type) {
        nonNull(type, "expectedType");

        Optional<Object> found = findAttribute(key);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        Object value = found.get();

        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }

        throw new IllegalArgumentException(
                "Request attribute '%s' has type %s, but expected %s"
                        .formatted(key, value.getClass().getName(), type.getName()));
    }

    /**
     * Creates an overlay request that falls back to the given parent request.
     *
     * <p>
     * Attributes defined in the current request take precedence over the parent.
     * </p>
     *
     * @param parent parent request
     * @return overlay request
     */
    public RenderingRequest overlayOn(RenderingRequest parent) {
        return new Overlay(this, nonNull(parent, "parent"));
    }

    /**
     * Overlay request implementation.
     *
     * <p>
     * Resolves attributes from the local request first,
     * then falls back to the parent request.
     * </p>
     */
    private static final class Overlay extends RenderingRequest {

        private final RenderingRequest local;
        private final RenderingRequest parent;

        private Overlay(RenderingRequest local, RenderingRequest parent) {
            super(new LinkedHashMap<>());
            this.local = nonNull(local, "local");
            this.parent = nonNull(parent, "parent");
        }

        /**
         * Returns local attributes only.
         */
        @Override
        public Map<String, Object> attributes() {
            return local.attributes();
        }

        /**
         * Resolves attribute from local request, falling back to parent.
         */
        @Override
        public Optional<Object> findAttribute(String attributeName) {
            Optional<Object> local = this.local.findAttribute(attributeName);

            if (local.isPresent()) {
                return local;
            }

            return parent.findAttribute(attributeName);
        }

        /**
         * Resolves typed attribute from local request, falling back to parent.
         */
        @Override
        public <T> Optional<T> findAttribute(String key, Class<T> type) {
            Optional<T> local = this.local.findAttribute(key, type);

            if (local.isPresent()) {
                return local;
            }

            return parent.findAttribute(key, type);
        }
    }
}