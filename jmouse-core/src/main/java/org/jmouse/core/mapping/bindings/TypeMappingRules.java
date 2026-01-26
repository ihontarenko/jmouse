package org.jmouse.core.mapping.bindings;

import org.jmouse.core.Verify;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TypeMappingRules {

    private final Class<?>               sourceType;
    private final Class<?>               targetType;
    private final Map<String, FieldRule> rules;

    TypeMappingRules(Class<?> sourceType, Class<?> targetType, Map<String, FieldRule> rules) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
        this.rules = Collections.unmodifiableMap(new LinkedHashMap<>(rules));
    }

    public Class<?> sourceType() { return sourceType; }
    public Class<?> targetType() { return targetType; }

    public FieldRule findRule(String targetName) {
        return rules.get(targetName);
    }
}
