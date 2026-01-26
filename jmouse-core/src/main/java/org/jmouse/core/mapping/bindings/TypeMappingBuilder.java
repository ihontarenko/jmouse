package org.jmouse.core.mapping.bindings;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TypeMappingBuilder<S, T> {

    private final Class<S>                  sourceType;
    private final Class<T>                  targetType;
    private final Map<String, FieldBinding> bindings = new LinkedHashMap<>();

    public TypeMappingBuilder(Class<S> sourceType, Class<T> targetType) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
    }

    public TypeMappingBuilder<S, T> bind(String targetName, String sourceReference) {
        bindings.put(targetName, FieldBinding.bind(targetName, sourceReference));
        return this;
    }

    public TypeMappingBuilder<S, T> bind(String targetName, ValueProvider<? super S> provider) {
        bindings.put(targetName, FieldBinding.bind(targetName, sourceType, provider));
        return this;
    }

    public TypeMappingBuilder<S, T> ignore(String targetName) {
        bindings.put(targetName, FieldBinding.ignore(targetName));
        return this;
    }

    public TypeMappingBuilder<S, T> constant(String targetName, Object value) {
        bindings.put(targetName, FieldBinding.constant(targetName, value));
        return this;
    }

    public TypeMappingBuilder<S, T> compute(String targetName, ComputeFunction<? super S> function) {
        bindings.put(targetName, FieldBinding.compute(targetName, sourceType, function));
        return this;
    }

    public TypeMappingBindings build() {
        return new TypeMappingBindings(sourceType, targetType, bindings);
    }
}
