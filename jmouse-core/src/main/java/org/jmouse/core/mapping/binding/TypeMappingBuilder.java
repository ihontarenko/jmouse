package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TypeMappingBuilder<S, T> {

    private final Class<S>                     sourceType;
    private final Class<T>                     targetType;
    private final Map<String, PropertyBinding> bindings = new LinkedHashMap<>();

    public TypeMappingBuilder(Class<S> sourceType, Class<T> targetType) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
    }

    public TypeMappingBuilder<S, T> bind(String targetName, String sourceReference) {
        bindings.put(targetName, PropertyBinding.reference(targetName, sourceReference));
        return this;
    }

    public TypeMappingBuilder<S, T> bind(String targetName, ValueProvider<? super S> provider) {
        bindings.put(targetName, PropertyBinding.provider(targetName, sourceType, provider));
        return this;
    }

    public TypeMappingBuilder<S, T> ignore(String targetName) {
        bindings.put(targetName, PropertyBinding.ignore(targetName));
        return this;
    }

    public TypeMappingBuilder<S, T> constant(String targetName, Object value) {
        bindings.put(targetName, PropertyBinding.constant(targetName, value));
        return this;
    }

    public TypeMappingBuilder<S, T> compute(String targetName, ComputeFunction<? super S> function) {
        bindings.put(targetName, PropertyBinding.compute(targetName, sourceType, function));
        return this;
    }

    public TypeMappingRule build() {
        return new TypeMappingRule(sourceType, targetType, bindings);
    }

}
