package org.jmouse.dom.blueprint.dsl;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.BlueprintValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder for blueprint attributes.
 */
public final class AttributeMapBuilder {

    private final Map<String, BlueprintValue> attributes = new LinkedHashMap<>();

    public AttributeMapBuilder attribute(String name, BlueprintValue value) {
        Verify.nonNull(name, "name");
        Verify.nonNull(value, "value");
        attributes.put(name, value);
        return this;
    }

    public AttributeMapBuilder attribute(String name, Object constantValue) {
        return attribute(name, Blueprints.constant(constantValue));
    }

    public Map<String, BlueprintValue> build() {
        return Map.copyOf(attributes);
    }
}
