package org.jmouse.core.mapping.bindings;

import org.jmouse.core.Verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MappingRulesRegistry {

    private final List<TypeMappingRules> mappings;

    private MappingRulesRegistry(List<TypeMappingRules> mappings) {
        this.mappings = List.copyOf(mappings);
    }

    public static MappingRulesRegistry empty() {
        return new MappingRulesRegistry(List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public TypeMappingRules find(Class<?> sourceType, Class<?> targetType) {
        Verify.nonNull(sourceType, "sourceType");
        Verify.nonNull(targetType, "targetType");

        // 1) exact
        for (TypeMappingRules rules : mappings) {
            if (rules.sourceType() == sourceType && rules.targetType() == targetType) {
                return rules;
            }
        }

        // 2) assignable (source supertype)
        for (TypeMappingRules rules : mappings) {
            if (rules.targetType() == targetType && rules.sourceType().isAssignableFrom(sourceType)) {
                return rules;
            }
        }

        // 3) wildcard source = Object.class
        for (TypeMappingRules rules : mappings) {
            if (rules.targetType() == targetType && rules.sourceType() == Object.class) {
                return rules;
            }
        }

        return null;
    }

    public static final class Builder {

        private final List<TypeMappingRules> list = new ArrayList<>();

        public <S, T> TypeMappingBuilder<S, T> mapping(Class<S> sourceType, Class<T> targetType) {
            return new TypeMappingBuilder<>(sourceType, targetType);
        }

        public Builder register(TypeMappingRules rules) {
            list.add(Objects.requireNonNull(rules, "rules"));
            return this;
        }

        public MappingRulesRegistry build() {
            return new MappingRulesRegistry(list);
        }

    }
}
