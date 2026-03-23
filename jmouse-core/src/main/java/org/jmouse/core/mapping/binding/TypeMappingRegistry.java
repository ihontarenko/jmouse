package org.jmouse.core.mapping.binding;

import org.jmouse.core.Customizer;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.typed.TypeMapper;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.core.Verify.nonNull;

/**
 * Registry of {@link TypeMappingRuleSource mapping rule sources}. 🧩
 *
 * <p>
 * {@code TypeMappingRegistry} acts as the central lookup component for
 * type mapping rules. It delegates resolution to registered
 * {@link TypeMappingRuleSource sources} and collects all matching
 * {@link TypeMappingRule rules}.
 * </p>
 *
 * <p>
 * Sources are consulted in registration order, allowing callers to combine:
 * </p>
 * <ul>
 *     <li>static DSL-defined mappings</li>
 *     <li>annotation-driven mappings</li>
 *     <li>custom dynamic rule providers</li>
 * </ul>
 */
public final class TypeMappingRegistry {

    private final List<TypeMappingRuleSource> sources;

    /**
     * Creates registry with the given rule sources.
     *
     * @param sources registered rule sources
     */
    private TypeMappingRegistry(List<TypeMappingRuleSource> sources) {
        this.sources = List.copyOf(sources);
    }

    /**
     * Creates a new registry builder.
     *
     * @return registry builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Resolves all mapping rules matching the given source and target types.
     *
     * <p>
     * Each registered {@link TypeMappingRuleSource} is queried in order.
     * Non-null results are collected into the returned list.
     * </p>
     *
     * @param sourceType source type
     * @param targetType target type
     * @param context    mapping context
     *
     * @return matching mapping rules, possibly empty
     */
    public List<TypeMappingRule> find(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        nonNull(sourceType, "sourceType");
        nonNull(targetType, "targetType");
        nonNull(context, "context");

        List<TypeMappingRule> mappingRules = new ArrayList<>();

        for (TypeMappingRuleSource source : sources) {
            TypeMappingRule candidate = source.find(sourceType, targetType, context);

            if (candidate != null) {
                mappingRules.add(candidate);
            }
        }

        return mappingRules;
    }

    /**
     * Builder for {@link TypeMappingRegistry}. 🏗️
     *
     * <p>
     * The builder includes a default static rule source used by the
     * mapping DSL methods such as {@link #mapping(Class, Class, Customizer)}.
     * Additional sources may be registered to extend rule resolution.
     * </p>
     */
    public static final class Builder {

        private final List<TypeMappingRuleSource> sources       = new ArrayList<>();
        private final StaticRuleSource            defaultSource = new StaticRuleSource();

        /**
         * Creates builder with the default static rule source.
         */
        public Builder() {
            sources.add(defaultSource);
        }

        /**
         * Adds an additional mapping rule source.
         *
         * <p>
         * Sources are consulted in registration order.
         * Add custom sources after DSL mappings if static rules should win.
         * </p>
         *
         * @param source rule source
         *
         * @return this builder
         */
        public Builder ruleSource(TypeMappingRuleSource source) {
            sources.add(nonNull(source, "source"));
            return this;
        }

        /**
         * Creates a DSL builder for the given source and target types.
         *
         * <p>
         * The resulting builder is backed by the default static rule source
         * and must be registered explicitly after configuration.
         * </p>
         *
         * @param sourceType source type
         * @param targetType target type
         * @param <S>        source type
         * @param <T>        target type
         *
         * @return type mapping builder
         */
        public <S, T> TypeMappingBuilder<S, T> mapping(Class<S> sourceType, Class<T> targetType) {
            return defaultSource.mapping(sourceType, targetType);
        }

        /**
         * Registers a DSL mapping for the given source and target types.
         *
         * @param sourceType source type
         * @param targetType target type
         * @param customizer mapping builder customizer
         * @param <S>        source type
         * @param <T>        target type
         *
         * @return this builder
         */
        public <S, T> Builder mapping(Class<S> sourceType, Class<T> targetType, Customizer<TypeMappingBuilder<S, T>> customizer) {
            nonNull(customizer, "customizer");

            TypeMappingBuilder<S, T> builder = defaultSource.mapping(sourceType, targetType);
            customizer.customize(builder);
            defaultSource.register(builder.build());

            return this;
        }

        /**
         * Registers a named property mapping backed by a {@link TypeMapper}.
         *
         * <p>
         * The mapper is adapted into a property provider for the target property.
         * </p>
         *
         * @param name       target property name
         * @param typeMapper type mapper
         * @param <S>        source type
         * @param <T>        target type
         *
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public <S, T> Builder mapping(String name, TypeMapper<S, T> typeMapper) {
            return mapping(typeMapper.sourceType(), typeMapper.targetType(), builder -> builder
                    .property(name, property -> property.provider(source -> typeMapper.map((S) source)))
            );
        }

        /**
         * Registers a prebuilt mapping rule in the default static source.
         *
         * @param rule mapping rule
         *
         * @return this builder
         */
        public Builder register(TypeMappingRule rule) {
            defaultSource.register(nonNull(rule, "rule"));
            return this;
        }

        /**
         * Builds the registry instance.
         *
         * @return type mapping registry
         */
        public TypeMappingRegistry build() {
            return new TypeMappingRegistry(sources);
        }
    }

}