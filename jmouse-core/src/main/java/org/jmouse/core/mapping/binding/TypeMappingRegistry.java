package org.jmouse.core.mapping.binding;

import org.jmouse.core.Customizer;
import org.jmouse.core.Verify;
import org.jmouse.core.mapping.MappingContext;

import java.util.ArrayList;
import java.util.List;

public final class TypeMappingRegistry {

    private final List<TypeMappingRuleSource> sources;

    private TypeMappingRegistry(List<TypeMappingRuleSource> sources) {
        this.sources = List.copyOf(sources);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<TypeMappingRule> find(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        Verify.nonNull(sourceType, "sourceType");
        Verify.nonNull(targetType, "targetType");
        Verify.nonNull(context, "context");

        List<TypeMappingRule> mappingRules = new ArrayList<>();

        for (TypeMappingRuleSource source : sources) {
            TypeMappingRule candidate = source.find(sourceType, targetType, context);
            if (candidate != null) {
                mappingRules.add(candidate);
            }
        }

        return mappingRules;
    }

    public static final class Builder {

        private final List<TypeMappingRuleSource> sources       = new ArrayList<>();
        private final StaticRuleSource            defaultSource = new StaticRuleSource();

        public Builder() {
            sources.add(defaultSource);
        }

        /**
         * Add a custom rule source (e.g. annotations).
         * Sources are consulted in order.
         *
         * Tip: add annotation source AFTER DSL registration if you want DSL to win.
         */
        public Builder ruleSource(TypeMappingRuleSource source) {
            sources.add(Verify.nonNull(source, "source"));
            return this;
        }

        /**
         * Your existing DSL entrypoint: TypeMappingRegistry.builder().mapping(A,B, m -> ...)
         * delegates to static rules.
         */
        public <S, T> TypeMappingBuilder<S, T> mapping(Class<S> sourceType, Class<T> targetType) {
            return defaultSource.mapping(sourceType, targetType);
        }

        public <S, T> Builder mapping(Class<S> sourceType, Class<T> targetType, Customizer<TypeMappingBuilder<S, T>> customizer) {
            Verify.nonNull(customizer, "customizer");
            TypeMappingBuilder<S, T> builder = defaultSource.mapping(sourceType, targetType);
            customizer.customize(builder);
            defaultSource.register(builder.build());
            return this;
        }

        public Builder register(TypeMappingRule rule) {
            defaultSource.register(Verify.nonNull(rule, "rule"));
            return this;
        }

        public TypeMappingRegistry build() {
            return new TypeMappingRegistry(sources);
        }
    }

}
