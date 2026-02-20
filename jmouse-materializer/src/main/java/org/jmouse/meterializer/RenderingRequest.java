package org.jmouse.meterializer;

import org.jmouse.core.Verify;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rendering request carrying user-provided attributes and options.
 *
 * <p>This object is mutable by design and is expected to be created per rendering call.</p>
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>typed read accessors</li>
 *   <li>overlay behavior without manual attribute merging</li>
 * </ul>
 */
public class RenderingRequest {

    private final Map<String, Object> attributes;

    public RenderingRequest() {
        this.attributes = new LinkedHashMap<>();
    }

    protected RenderingRequest(Map<String, Object> attributes) {
        this.attributes = Verify.nonNull(attributes, "attributes");
    }

    public RenderingRequest putAttribute(String attributeName, Object attributeValue) {
        Verify.nonNull(attributeName, "attributeName");

        if (attributeValue == null) {
            return this;
        }

        attributes.put(attributeName, attributeValue);
        return this;
    }

    public RenderingRequest putAllAttributes(Map<String, ?> attributesToAdd) {
        Verify.nonNull(attributesToAdd, "attributesToAdd");

        for (Map.Entry<String, ?> entry : attributesToAdd.entrySet()) {
            String name  = entry.getKey();
            Object value = entry.getValue();

            if (name == null) {
                continue;
            }
            if (value == null) {
                continue;
            }

            attributes.put(name, value);
        }

        return this;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public Map<String, Object> unmodifiableAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    // ---------------------------------------------------------------------
    // Read accessors
    // ---------------------------------------------------------------------

    public Optional<Object> findAttribute(String attributeName) {
        Verify.nonNull(attributeName, "attributeName");

        Object value = attributes.get(attributeName);
        return Optional.ofNullable(value);
    }

    public <T> Optional<T> findAttribute(String attributeName, Class<T> expectedType) {
        Verify.nonNull(expectedType, "expectedType");

        Optional<Object> found = findAttribute(attributeName);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        Object value = found.get();
        if (expectedType.isInstance(value)) {
            return Optional.of(expectedType.cast(value));
        }

        throw new IllegalArgumentException("Request attribute '" + attributeName + "' has type " + value.getClass()
                .getName() + ", but expected " + expectedType.getName());
    }

    public <T> T requireAttribute(String attributeName, Class<T> expectedType) {
        Optional<T> found = findAttribute(attributeName, expectedType);

        if (found.isPresent()) {
            return found.get();
        }

        throw new IllegalStateException(
                "Missing request attribute '" + attributeName + "' of type " + expectedType.getName());
    }

    public String getStringAttribute(String attributeName) {
        Optional<Object> found = findAttribute(attributeName);

        if (found.isEmpty()) {
            return null;
        }

        Object value = found.get();
        return String.valueOf(value);
    }

    public Integer getIntegerAttribute(String attributeName) {
        Optional<Object> found = findAttribute(attributeName);

        if (found.isEmpty()) {
            return null;
        }

        Object value = found.get();

        if (value instanceof Integer integerValue) {
            return integerValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }

        return Integer.valueOf(String.valueOf(value));
    }

    public boolean getBooleanAttribute(String attributeName) {
        Optional<Object> found = findAttribute(attributeName);

        if (found.isEmpty()) {
            return false;
        }

        Object value = found.get();

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }

        return Boolean.parseBoolean(String.valueOf(value));
    }

    // ---------------------------------------------------------------------
    // Overlay
    // ---------------------------------------------------------------------

    /**
     * Returns a request that reads from this request first and falls back to the parent request.
     *
     * <p>Local attributes override parent attributes.</p>
     */
    public RenderingRequest overlayOn(RenderingRequest parentRequest) {
        Verify.nonNull(parentRequest, "parentRequest");
        return new OverlayRenderingRequest(this, parentRequest);
    }

    /**
     * Overlay implementation: local request overrides parent request.
     */
    private static final class OverlayRenderingRequest extends RenderingRequest {

        private final RenderingRequest localRequest;
        private final RenderingRequest parentRequest;

        private OverlayRenderingRequest(RenderingRequest localRequest, RenderingRequest parentRequest) {
            super(new LinkedHashMap<>());
            this.localRequest = Verify.nonNull(localRequest, "localRequest");
            this.parentRequest = Verify.nonNull(parentRequest, "parentRequest");
        }

        @Override
        public Map<String, Object> attributes() {
            // Expose local map for mutation.
            return localRequest.attributes();
        }

        @Override
        public Optional<Object> findAttribute(String attributeName) {
            Optional<Object> local = localRequest.findAttribute(attributeName);

            if (local.isPresent()) {
                return local;
            }

            return parentRequest.findAttribute(attributeName);
        }

        @Override
        public <T> Optional<T> findAttribute(String attributeName, Class<T> expectedType) {
            Optional<T> local = localRequest.findAttribute(attributeName, expectedType);

            if (local.isPresent()) {
                return local;
            }

            return parentRequest.findAttribute(attributeName, expectedType);
        }
    }
}
