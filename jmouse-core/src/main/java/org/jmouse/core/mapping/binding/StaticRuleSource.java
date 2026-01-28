package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.MappingContext;

import java.util.ArrayList;
import java.util.List;

final class StaticRuleSource implements TypeMappingRuleSource {

    private final List<TypeMappingRule> mappingRules = new ArrayList<>();

    <S, T> TypeMappingBuilder<S, T> mapping(Class<S> sourceType, Class<T> targetType) {
        return new TypeMappingBuilder<>(sourceType, targetType);
    }

    void register(TypeMappingRule mappingRule) {
        mappingRules.add(Verify.nonNull(mappingRule, "mappingRule"));
    }

    @Override
    public TypeMappingRule find(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        // 1) exact
        for (TypeMappingRule mappingRule : mappingRules) {
            if (mappingRule.sourceType() == sourceType && mappingRule.targetType() == targetType) {
                return mappingRule;
            }
        }

        // 2) assignable (source supertype)
        for (TypeMappingRule mappingRule : mappingRules) {
            if (mappingRule.targetType() == targetType && mappingRule.sourceType().isAssignableFrom(sourceType)) {
                return mappingRule;
            }
        }

        // 3) wildcard source = Object.class
        for (TypeMappingRule mappingRule : mappingRules) {
            if (mappingRule.targetType() == targetType && mappingRule.sourceType() == Object.class) {
                return mappingRule;
            }
        }

        return null;
    }
}
