package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of {@link TypeMappingRule} definitions used by the mapping engine to resolve
 * per-(sourceType,targetType) mapping configuration. 🧭
 *
 * <p>{@code TypeMappingRules} is an immutable container built via {@link Builder}.
 * Resolution is performed by {@link #find(Class, Class)} using a deterministic priority order.</p>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * TypeMappingRules rules = TypeMappingRules.builder()
 *     .register(
 *         TypeMappingRule.builder(UserDto.class, User.class)
 *             .bind("id", "id")
 *             .bind("name", "fullName")
 *             .ignore("password")
 *             .build()
 *     )
 *     .register(
 *         TypeMappingRule.builder(Object.class, AuditRecord.class)
 *             .compute("createdAt", Object.class, (src, ctx) -> Instant.now())
 *             .build()
 *     )
 *     .build();
 *
 * TypeMappingRule r1 = rules.find(UserDto.class, User.class);          // exact match
 * TypeMappingRule r2 = rules.find(AdminDto.class, User.class);         // assignable by source supertype
 * TypeMappingRule r3 = rules.find(Any.class, AuditRecord.class);       // wildcard source (Object.class)
 * }</pre>
 *
 * <h3>Lookup priority</h3>
 * <ol>
 *   <li><b>Exact match</b>: {@code rule.sourceType() == sourceType && rule.targetType() == targetType}</li>
 *   <li><b>Assignable source</b>: {@code rule.targetType() == targetType && rule.sourceType().isAssignableFrom(sourceType)}</li>
 *   <li><b>Wildcard source</b>: {@code rule.targetType() == targetType && rule.sourceType() == Object.class}</li>
 * </ol>
 *
 * <p><strong>Important:</strong> when multiple rules satisfy steps (2) or (3),
 * the first registered rule wins (stable iteration order).</p>
 *
 * @see TypeMappingRule
 * @see Builder
 */
public final class TypeMappingRules {

    private final List<TypeMappingRule> mappings;

    private TypeMappingRules(List<TypeMappingRule> mappings) {
        this.mappings = List.copyOf(mappings);
    }

    /**
     * Create an empty rules registry.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * TypeMappingRules rules = TypeMappingRules.empty();
     * }</pre>
     *
     * @return empty {@code TypeMappingRules}
     */
    public static TypeMappingRules empty() {
        return new TypeMappingRules(List.of());
    }

    /**
     * Create a new builder for registering mapping rules.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * TypeMappingRules rules = TypeMappingRules.builder()
     *     .register(myRule)
     *     .build();
     * }</pre>
     *
     * @return new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Resolve a {@link TypeMappingRule} for the given source and target types.
     *
     * <p>The lookup uses the following order:</p>
     * <ol>
     *   <li>Exact match (source and target are identical)</li>
     *   <li>Assignable match by source supertype (same target; rule.sourceType is a supertype of sourceType)</li>
     *   <li>Wildcard source (same target; rule.sourceType == {@code Object.class})</li>
     * </ol>
     *
     * <p>If no rule matches, returns {@code null}.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * TypeMappingRule rule = rules.find(OrderDto.class, Order.class);
     * if (rule != null) {
     *     // apply property bindings...
     * }
     * }</pre>
     *
     * @param sourceType actual runtime source type
     * @param targetType requested target type
     * @return resolved rule, or {@code null} when none matches
     * @throws IllegalArgumentException if {@code sourceType} or {@code targetType} is {@code null}
     */
    public TypeMappingRule find(Class<?> sourceType, Class<?> targetType) {
        Verify.nonNull(sourceType, "sourceType");
        Verify.nonNull(targetType, "targetType");

        // 1) exact
        for (TypeMappingRule bindings : mappings) {
            if (bindings.sourceType() == sourceType && bindings.targetType() == targetType) {
                return bindings;
            }
        }

        // 2) assignable (source supertype)
        for (TypeMappingRule bindings : mappings) {
            if (bindings.targetType() == targetType && bindings.sourceType().isAssignableFrom(sourceType)) {
                return bindings;
            }
        }

        // 3) wildcard source = Object.class
        for (TypeMappingRule bindings : mappings) {
            if (bindings.targetType() == targetType && bindings.sourceType() == Object.class) {
                return bindings;
            }
        }

        return null;
    }

    /**
     * Builder for {@link TypeMappingRules}. 🧱
     *
     * <p>Registration order matters for non-exact matches: the first compatible rule wins.</p>
     */
    public static final class Builder {

        private final List<TypeMappingRule> collection = new ArrayList<>();

        /**
         * Create a fluent {@link TypeMappingBuilder} for the given type pair.
         *
         * <p><strong>Note:</strong> this method only creates the builder. To actually register the rule,
         * build it and pass to {@link #register(TypeMappingRule)}.</p>
         *
         * <h3>Example</h3>
         * <pre>{@code
         * TypeMappingRules rules = TypeMappingRules.builder()
         *     .register(
         *         TypeMappingRules.builder()
         *             .mapping(UserDto.class, User.class)
         *             .bind("name", "fullName")
         *             .build()
         *     )
         *     .build();
         * }</pre>
         *
         * @param sourceType source type
         * @param targetType target type
         * @param <S> source type
         * @param <T> target type
         * @return new {@link TypeMappingBuilder} instance
         */
        public <S, T> TypeMappingBuilder<S, T> mapping(Class<S> sourceType, Class<T> targetType) {
            return new TypeMappingBuilder<>(sourceType, targetType);
        }

        /**
         * Register a pre-built {@link TypeMappingRule}.
         *
         * <h3>Example</h3>
         * <pre>{@code
         * builder.register(rule1).register(rule2);
         * }</pre>
         *
         * @param bindings rule to register
         * @return this builder
         * @throws IllegalArgumentException if {@code bindings} is {@code null}
         */
        public Builder register(TypeMappingRule bindings) {
            collection.add(Verify.nonNull(bindings, "bindings"));
            return this;
        }

        /**
         * Build an immutable {@link TypeMappingRules} instance.
         *
         * @return immutable rules registry
         */
        public TypeMappingRules build() {
            return new TypeMappingRules(collection);
        }

    }
}
