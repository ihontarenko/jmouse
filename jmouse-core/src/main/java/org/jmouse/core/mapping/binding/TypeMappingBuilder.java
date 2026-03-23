package org.jmouse.core.mapping.binding;

import org.jmouse.core.Customizer;
import org.jmouse.core.mapping.MappingContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.Verify.notBlank;

/**
 * Fluent builder for defining a {@link TypeMappingRule} between {@code S -> T}. 🧱
 *
 * <p>
 * {@code TypeMappingBuilder} collects per-property {@link PropertyMapping}
 * definitions and produces an immutable {@link TypeMappingRule}.
 * </p>
 *
 * <p>
 * The main DSL entry point is {@link #property(String, Customizer)},
 * which configures a {@link PropertyMappingBuilder} for a single
 * target property.
 * </p>
 *
 * @param <S> source type
 * @param <T> target type
 */
public final class TypeMappingBuilder<S, T> {

    private final Class<S>                     sourceType;
    private final Class<T>                     targetType;
    private final Map<String, PropertyMapping> bindings = new LinkedHashMap<>();

    /**
     * Creates a builder for mappings from {@code sourceType} to {@code targetType}.
     *
     * @param sourceType source type
     * @param targetType target type
     */
    public TypeMappingBuilder(Class<S> sourceType, Class<T> targetType) {
        this.sourceType = nonNull(sourceType, "sourceType");
        this.targetType = nonNull(targetType, "targetType");
    }

    /**
     * Configures mapping for a single target property.
     *
     * <p>
     * The provided {@code customizer} receives a {@link PropertyMappingBuilder}
     * used to define how the target property should be populated
     * (reference, ignore, constant, compute, provider, and so on).
     * </p>
     *
     * @param targetName target property name
     * @param customizer property mapping customizer
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> property(String targetName, Customizer<PropertyMappingBuilder> customizer) {
        PropertyMappingBuilder builder = new PropertyMappingBuilder(notBlank(targetName, "targetName"));
        nonNull(customizer, "customizer").customize(builder);
        bindings.put(targetName, builder.build());
        return this;
    }

    /**
     * Adds a condition for mapping the given target property.
     *
     * @param targetName target property name
     * @param condition  mapping condition
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> when(String targetName, BiPredicate<Object, MappingContext> condition) {
        return property(targetName, builder -> builder.when(condition));
    }

    /**
     * Marks the given target property as required.
     *
     * @param targetName target property name
     * @param code       validation or error code
     * @param message    validation message
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> required(String targetName, String code, String message) {
        return property(targetName, builder -> builder.required(code, message));
    }

    /**
     * Registers a value transformer for the given target property.
     *
     * @param targetName   target property name
     * @param transformer  value transformer
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> transformer(String targetName, ValueTransformer transformer) {
        return property(targetName, builder -> builder.transform(transformer));
    }

    /**
     * Registers a constant default value for the given target property.
     *
     * @param targetName   target property name
     * @param defaultValue default value
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> defaultValue(String targetName, Object defaultValue) {
        return defaultValue(targetName, () -> defaultValue);
    }

    /**
     * Registers a lazy default value supplier for the given target property.
     *
     * @param targetName   target property name
     * @param defaultValue default value supplier
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> defaultValue(String targetName, Supplier<?> defaultValue) {
        return property(targetName, builder -> builder.defaultValue(defaultValue));
    }

    /**
     * Registers coalescing computation for the given target property.
     *
     * <p>
     * Candidate mappings are evaluated in order until a value is resolved.
     * </p>
     *
     * @param targetName target property name
     * @param candidates candidate property mappings
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> compute(String targetName, PropertyMapping... candidates) {
        return property(targetName, builder -> builder.coalesce(candidates));
    }

    /**
     * Registers a compute function for the given target property.
     *
     * @param function   compute function
     * @param targetName target property name
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> compute(ComputeFunction<?> function, String targetName) {
        return property(targetName, builder -> builder.compute(function));
    }

    /**
     * Registers a provider-based mapping for the given target property.
     *
     * @param provider   value provider
     * @param targetName target property name
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> provider(ValueProvider<S> provider, String targetName) {
        return property(targetName, builder -> builder.provider(provider));
    }

    /**
     * Registers source-to-target property reference mapping.
     *
     * @param sourceName source property name
     * @param targetName target property name
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> reference(String sourceName, String targetName) {
        return property(targetName, builder -> builder.reference(sourceName));
    }

    /**
     * Registers constant value mapping for the given target property.
     *
     * @param constant   constant value
     * @param targetName target property name
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> constant(Object constant, String targetName) {
        return property(targetName, builder -> builder.constant(constant));
    }

    /**
     * Builds an immutable {@link TypeMappingRule} from collected bindings.
     *
     * @return type mapping rule
     */
    public TypeMappingRule build() {
        return new TypeMappingRule(sourceType, targetType, bindings);
    }
}