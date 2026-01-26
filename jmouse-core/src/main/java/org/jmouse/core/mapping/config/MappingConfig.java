package org.jmouse.core.mapping.config;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.bindings.MappingRulesRegistry;

import java.util.Objects;

public final class MappingConfig {

    private final MappingPolicy        policy;
    private final Conversion           conversion;
    private final MappingRulesRegistry rules;

    private MappingConfig(MappingPolicy policy, Conversion conversion, MappingRulesRegistry rules) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.conversion = Objects.requireNonNull(conversion, "conversion");
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    public static Builder builder() {
        return new Builder();
    }

    public MappingPolicy policy() {
        return policy;
    }

    public Conversion conversion() {
        return conversion;
    }

    public MappingRulesRegistry rules() {
        return rules;
    }

    public static final class Builder {
        private MappingPolicy        policy;
        private Conversion           conversion;
        private MappingRulesRegistry rules = MappingRulesRegistry.empty();

        public Builder policy(MappingPolicy policy) {
            this.policy = policy;
            return this;
        }

        public Builder conversion(Conversion conversion) {
            this.conversion = conversion;
            return this;
        }

        public Builder rules(MappingRulesRegistry rules) {
            this.rules = rules;
            return this;
        }

        public MappingConfig build() {
            return new MappingConfig(policy, conversion, rules);
        }
    }
}
