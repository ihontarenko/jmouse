package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.MappingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Static in-memory {@link TypeMappingRuleSource}. 🧩
 *
 * <p>
 * Stores registered {@link TypeMappingRule mapping rules} and resolves
 * the most suitable rule for a source/target type pair.
 * </p>
 *
 * <p>
 * Resolution order:
 * </p>
 * <ol>
 *     <li>exact source and target type match</li>
 *     <li>assignable source supertype with exact target type</li>
 *     <li>wildcard source type ({@link Object}) with exact target type</li>
 * </ol>
 */
final class StaticRuleSource implements TypeMappingRuleSource {

    private final List<TypeMappingRule> mappingRules = new ArrayList<>();

    /**
     * Creates a mapping builder for the given source and target types.
     *
     * @param sourceType source type
     * @param targetType target type
     * @param <S>        source type
     * @param <T>        target type
     *
     * @return new type mapping builder
     */
    <S, T> TypeMappingBuilder<S, T> mapping(Class<S> sourceType, Class<T> targetType) {
        return new TypeMappingBuilder<>(sourceType, targetType);
    }

    /**
     * Registers a mapping rule.
     *
     * @param mappingRule mapping rule to register
     */
    void register(TypeMappingRule mappingRule) {
        mappingRules.add(Verify.nonNull(mappingRule, "mappingRule"));
    }

    /**
     * Finds the best matching mapping rule for the given types.
     *
     * <p>
     * Matching is performed in the following order:
     * </p>
     * <ol>
     *     <li>exact source and target match</li>
     *     <li>assignable source supertype with exact target match</li>
     *     <li>{@link Object}-based wildcard source with exact target match</li>
     * </ol>
     *
     * @param sourceType source type
     * @param targetType target type
     * @param context    mapping context
     *
     * @return matching mapping rule or {@code null} if none found
     */
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