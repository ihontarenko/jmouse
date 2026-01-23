package org.jmouse.core.mapping.runtime;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.ObjectMapper;
import org.jmouse.core.mapping.config.MappingPolicy;

import java.util.Objects;

public record MappingContext(ObjectMapper mapper, MappingPolicy policy, Conversion conversion) {

    public MappingContext(
            ObjectMapper mapper,
            MappingPolicy policy,
            Conversion conversion
    ) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.conversion = Objects.requireNonNull(conversion, "conversion");
    }

}
