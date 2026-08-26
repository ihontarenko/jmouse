package org.jmouse.mapper.binding;

import org.jmouse.core.Customizer;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.typed.TypeMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final List<TypeMappingRuleSource>          sources;
    private final Map<RuleKey, List<TypeMappingRule>>  resolved = new ConcurrentHashMap<>();

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
     * <p>
     * The result is memoized per type pair, because this sits on the hot path: an object strategy
     * asks once per property, and the sources behind it scan their rules linearly. A pair that
     * matches nothing resolves to a shared empty list rather than a fresh one.
     * </p>
     *
     * @param sourceType source type
     * @param targetType target type
     * @param context    mapping context
     *
     * @return matching mapping rules, possibly empty and always immutable
     */
    public List<TypeMappingRule> find(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        nonNull(sourceType, "sourceType");
        nonNull(targetType, "targetType");
        nonNull(context, "context");

        return resolved.computeIfAbsent(
                new RuleKey(sourceType, targetType), ignored -> resolve(sourceType, targetType, context));
    }

    /**
     * Queries every registered source once and collects the rules they contribute.
     *
     * @param sourceType source type
     * @param targetType target type
     * @param context    mapping context
     *
     * @return immutable list of matching rules, possibly empty
     */
    private List<TypeMappingRule> resolve(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        List<TypeMappingRule> mappingRules = new ArrayList<>(sources.size());

        for (TypeMappingRuleSource source : sources) {
            TypeMappingRule candidate = source.find(sourceType, targetType, context);

            if (candidate != null) {
                mappingRules.add(candidate);
            }
        }

        return List.copyOf(mappingRules);
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
         * Registers a DSL mapping for the given source and target types.
         *
         * <p>
         * Only properties whose names disagree need an entry - the engine reads same-named
         * properties without being told.
         * </p>
         *
         * <pre>{@code
         * rules.mapping(UserA.class, UserB.class, user -> user.rename("dateOfBirth", "birthDay"));
         * }</pre>
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

    /**
     * Memoization key for a resolved rule list.
     *
     * @param sourceType source type
     * @param targetType target type
     */
    private record RuleKey(Class<?> sourceType, Class<?> targetType) {}

}