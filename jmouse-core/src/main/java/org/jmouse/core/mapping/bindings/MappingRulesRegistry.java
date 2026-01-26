package org.jmouse.core.mapping.bindings;

import org.jmouse.core.Verify;

import java.util.ArrayList;
import java.util.List;

public final class MappingRulesRegistry {

    private final List<TypeMappingBindings> mappings;

    private MappingRulesRegistry(List<TypeMappingBindings> mappings) {
        this.mappings = List.copyOf(mappings);
    }

    public static MappingRulesRegistry empty() {
        return new MappingRulesRegistry(List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public TypeMappingBindings find(Class<?> sourceType, Class<?> targetType) {
        Verify.nonNull(sourceType, "sourceType");
        Verify.nonNull(targetType, "targetType");

        // 1) exact
        for (TypeMappingBindings bindings : mappings) {
            if (bindings.sourceType() == sourceType && bindings.targetType() == targetType) {
                return bindings;
            }
        }

        // 2) assignable (source supertype)
        for (TypeMappingBindings bindings : mappings) {
            if (bindings.targetType() == targetType && bindings.sourceType().isAssignableFrom(sourceType)) {
                return bindings;
            }
        }

        // 3) wildcard source = Object.class
        for (TypeMappingBindings bindings : mappings) {
            if (bindings.targetType() == targetType && bindings.sourceType() == Object.class) {
                return bindings;
            }
        }

        return null;
    }

    public static final class Builder {

        private final List<TypeMappingBindings> collection = new ArrayList<>();

        public <S, T> TypeMappingBuilder<S, T> mapping(Class<S> sourceType, Class<T> targetType) {
            return new TypeMappingBuilder<>(sourceType, targetType);
        }

        public Builder register(TypeMappingBindings bindings) {
            collection.add(Verify.nonNull(bindings, "bindings"));
            return this;
        }

        public MappingRulesRegistry build() {
            return new MappingRulesRegistry(collection);
        }

    }
}
