package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rendering request carrying user-provided attributes and options.
 */
public final class RenderingRequest {

    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public RenderingRequest attribute(String name, Object value) {
        Verify.nonNull(name, "name");

        if (value != null) {
            attributes.put(name, value);
        }

        return this;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }
}
