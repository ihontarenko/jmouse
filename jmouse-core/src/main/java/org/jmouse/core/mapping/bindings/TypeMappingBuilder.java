package org.jmouse.core.mapping.bindings;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TypeMappingBuilder<S, T> {

    private final Class<S> sourceType;
    private final Class<T> targetType;
    private final Map<String, FieldRule> rules = new LinkedHashMap<>();

    public TypeMappingBuilder(Class<S> sourceType, Class<T> targetType) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
    }

    public TypeMappingBuilder<S, T> bind(String targetName, String sourceReference) {
        rules.put(targetName, FieldRule.bind(targetName, sourceReference));
        return this;
    }

    public TypeMappingBuilder<S, T> bind(String targetName, SourceValueProvider<S> provider) {
        rules.put(targetName, FieldRule.bind(targetName, provider));
        return this;
    }

    public TypeMappingBuilder<S, T> ignore(String targetName) {
        rules.put(targetName, FieldRule.ignore(targetName));
        return this;
    }

    public TypeMappingBuilder<S, T> constant(String targetName, Object value) {
        rules.put(targetName, FieldRule.constant(targetName, value));
        return this;
    }

    public TypeMappingBuilder<S, T> compute(String targetName, FieldComputeFunction fn) {
        rules.put(targetName, FieldRule.compute(targetName, fn));
        return this;
    }

    public TypeMappingRules build() {
        return new TypeMappingRules(sourceType, targetType, rules);
    }

}
