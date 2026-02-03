package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for defining a {@link TypeMappingRule} between {@code S -> T}. 🧱
 *
 * <p>{@code TypeMappingBuilder} collects per-property {@link PropertyMapping} definitions
 * (reference/provider/ignore/constant/compute) and produces an immutable {@link TypeMappingRule}.</p>
 *
 * <p>The builder is typically obtained from a registry builder (e.g. {@code TypeMappingRegistry.builder().mapping(...)})
 * and configured using a fluent DSL.</p>
 *
 * @param <S> source type
 * @param <T> target type
 */
public final class TypeMappingBuilder<S, T> {

    private final Class<S>                     sourceType;
    private final Class<T>                     targetType;
    private final Map<String, PropertyMapping> bindings = new LinkedHashMap<>();

    /**
     * Create a mapping builder for a given type pair.
     *
     * @param sourceType source type (never {@code null})
     * @param targetType target type (never {@code null})
     */
    public TypeMappingBuilder(Class<S> sourceType, Class<T> targetType) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
    }

    /**
     * Bind a target property to a value referenced from the source accessor.
     *
     * @param targetName target property name
     * @param sourceReference source path/reference (accessor-dependent)
     * @return this builder
     */
    public TypeMappingBuilder<S, T> reference(String targetName, String sourceReference) {
        bindings.put(targetName, PropertyMapping.reference(targetName, sourceReference));
        return this;
    }

    /**
     * Bind a target property to a value produced by a provider function.
     *
     * <p>The provider is adapted to a runtime-safe {@code Object} provider by
     * {@link PropertyMapping#provider(String, Class, ValueProvider)}.</p>
     *
     * @param targetName target property name
     * @param provider source-based value provider
     * @return this builder
     */
    public TypeMappingBuilder<S, T> provider(String targetName, ValueProvider<? super S> provider) {
        bindings.put(targetName, PropertyMapping.provider(targetName, sourceType, provider));
        return this;
    }

    /**
     * Mark the target property as ignored.
     *
     * @param targetName target property name
     * @return this builder
     */
    public TypeMappingBuilder<S, T> ignore(String targetName) {
        bindings.put(targetName, PropertyMapping.ignore(targetName));
        return this;
    }

    /**
     * Bind the target property to a constant value.
     *
     * @param targetName target property name
     * @param value constant value (may be {@code null})
     * @return this builder
     */
    public TypeMappingBuilder<S, T> constant(String targetName, Object value) {
        bindings.put(targetName, PropertyMapping.constant(targetName, value));
        return this;
    }

    /**
     * Bind a target property to a computed value.
     *
     * <p>The compute function receives the typed source value and mapping context.</p>
     *
     * @param targetName target property name
     * @param function compute function
     * @return this builder
     */
    public TypeMappingBuilder<S, T> compute(String targetName, ComputeFunction<? super S> function) {
        bindings.put(targetName, PropertyMapping.compute(targetName, sourceType, function));
        return this;
    }

    /**
     * Build an immutable {@link TypeMappingRule}.
     *
     * @return type mapping rule
     */
    public TypeMappingRule build() {
        return new TypeMappingRule(sourceType, targetType, bindings);
    }

}
