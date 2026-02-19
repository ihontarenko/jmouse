package org.jmouse.dom.template.model;

import java.util.Map;

/**
 * Form metadata that may be used by HTML form rendering.
 *
 * <p>All fields are optional. Rendering can be driven by request attributes as well.</p>
 */
public record FormMetadata(
        String title,
        String description,
        String action,
        String method,
        Map<String, String> attributes
) {

    public FormMetadata {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static FormMetadata empty() {
        return new FormMetadata(null, null, null, null, Map.of());
    }

}
