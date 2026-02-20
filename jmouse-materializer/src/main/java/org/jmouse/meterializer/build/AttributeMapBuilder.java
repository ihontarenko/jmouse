package org.jmouse.meterializer.build;

import org.jmouse.core.Verify;
import org.jmouse.meterializer.ValueExpression;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.jmouse.meterializer.ValueExpression.constant;

/**
 * Builder for blueprint attributes.
 */
public final class AttributeMapBuilder {

    private final Map<String, ValueExpression> attributes = new LinkedHashMap<>();

    public AttributeMapBuilder attribute(String name, ValueExpression value) {
        Verify.nonNull(name, "name");
        Verify.nonNull(value, "value");
        attributes.put(name, value);
        return this;
    }

    public AttributeMapBuilder attribute(String name, Object constantValue) {
        return attribute(name, constant(constantValue));
    }

    public Map<String, ValueExpression> build() {
        return Map.copyOf(attributes);
    }
}
