package org.jmouse.dom.building;

import org.jmouse.core.Verify;
import org.jmouse.dom.TagName;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Request describing what should be built into a DOM node tree.
 */
public final class NodeBuildRequest {

    private final Object              value;
    private final Type                declaredType;
    private final String              purpose;
    private final TagName             preferredTagName;
    private final Map<String, Object> attributes;

    private NodeBuildRequest(
            Object value,
            Type declaredType,
            String purpose,
            TagName preferredTagName,
            Map<String, Object> attributes
    ) {

        this.value = value;
        this.declaredType = Verify.nonNull(declaredType, "declaredType");
        this.purpose = purpose;
        this.preferredTagName = preferredTagName;
        this.attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
    }

    public static NodeBuildRequest of(Object value, Type declaredType) {
        return new NodeBuildRequest(value, declaredType, null, null, null);
    }

    public NodeBuildRequest withPurpose(String purpose) {
        return new NodeBuildRequest(value, declaredType, purpose, preferredTagName, attributes);
    }

    public NodeBuildRequest withPreferredTagName(TagName tagName) {
        return new NodeBuildRequest(value, declaredType, purpose, tagName, attributes);
    }

    public NodeBuildRequest withAttributes(Map<String, Object> attributes) {
        return new NodeBuildRequest(value, declaredType, purpose, preferredTagName, attributes);
    }

    public Object value() {
        return value;
    }

    public Type declaredType() {
        return declaredType;
    }

    public Optional<String> purpose() {
        return Optional.ofNullable(purpose);
    }

    public Optional<TagName> preferredTagName() {
        return Optional.ofNullable(preferredTagName);
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

}
