package org.jmouse.core.mapping.config;

import org.jmouse.core.Verify;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.virtuals.VirtualPropertyResolver;

import java.util.Objects;

/**
 * Immutable mapper configuration.
 */
public final class MappingConfig {

    private final MappingPolicy policy;
    private final Conversion conversion;
    private final VirtualPropertyResolver virtualPropertyResolver;

    private MappingConfig(Builder builder) {
        this.policy = Objects.requireNonNull(builder.policy, "policy");
        this.conversion = Objects.requireNonNull(builder.conversion, "conversion");
        this.virtualPropertyResolver = Objects.requireNonNull(builder.virtualPropertyResolver, "virtualPropertyResolver");
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

    public VirtualPropertyResolver virtualPropertyResolver() {
        return virtualPropertyResolver;
    }

    public static final class Builder {

        private MappingPolicy           policy                  = MappingPolicy.defaults();
        private Conversion              conversion;
        private VirtualPropertyResolver virtualPropertyResolver = VirtualPropertyResolver.noop();

        public Builder policy(MappingPolicy value) {
            this.policy = value;
            return this;
        }

        public Builder conversion(Conversion value) {
            this.conversion = value;
            return this;
        }

        public Builder virtualPropertyResolver(VirtualPropertyResolver value) {
            this.virtualPropertyResolver = value;
            return this;
        }

        public MappingConfig build() {
            Verify.nonNull(conversion, "conversion");
            return new MappingConfig(this);
        }
    }
}
