package org.jmouse.core.mapping.binding;

import org.jmouse.core.Customizer;
import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for defining a {@link TypeMappingRule} between {@code S -> T}. 🧱
 *
 * <p>{@code TypeMappingBuilder} collects per-property {@link PropertyMapping} definitions and produces
 * an immutable {@link TypeMappingRule}.</p>
 *
 * <p>This variant exposes a DSL entrypoint via {@link #property(String, Customizer)} which configures a
 * {@link PropertyMappingBuilder} for a single target property.</p>
 *
 * @param <S> source type
 * @param <T> target type
 */
public final class TypeMappingBuilder<S, T> {

    private final Class<S>                     sourceType;
    private final Class<T>                     targetType;
    private final Map<String, PropertyMapping> bindings = new LinkedHashMap<>();

    /**
     * Create a builder for mappings from {@code sourceType} to {@code targetType}.
     *
     * @param sourceType source type (never {@code null})
     * @param targetType target type (never {@code null})
     */
    public TypeMappingBuilder(Class<S> sourceType, Class<T> targetType) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
    }

    /**
     * Configure mapping for a single target property.
     *
     * <p>The provided {@code customizer} receives a {@link PropertyMappingBuilder} which can be used to
     * define how the target property should be populated (reference/ignore/constant/compute/provider, etc.).</p>
     *
     * @param name target property name (must not be blank)
     * @param customizer mapping customizer (never {@code null})
     * @return this builder (fluent)
     */
    public TypeMappingBuilder<S, T> property(String name, Customizer<PropertyMappingBuilder> customizer) {
        Verify.notBlank(name, "name");
        Verify.nonNull(customizer, "customizer");

        PropertyMappingBuilder builder = new PropertyMappingBuilder(name);
        customizer.customize(builder);
        bindings.put(name, builder.build());

        return this;
    }

    /**
     * Convenience overload for the default mapping convention ("same name").
     *
     * <p>Equivalent to {@code property(targetName, p -> {})}. The default {@link PropertyMappingBuilder}
     * behavior should resolve to "reference(targetName -> targetName)".</p>
     *
     * @param targetName target property name (must not be blank)
     * @return this builder (fluent)
     */
    public TypeMappingBuilder<S, T> property(String targetName) {
        return property(targetName, p -> { });
    }

    /**
     * Build an immutable {@link TypeMappingRule} from collected property bindings.
     *
     * @return type mapping rule
     */
    public TypeMappingRule build() {
        return new TypeMappingRule(sourceType, targetType, bindings);
    }
}
