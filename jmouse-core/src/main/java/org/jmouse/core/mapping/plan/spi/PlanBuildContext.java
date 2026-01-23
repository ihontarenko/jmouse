package org.jmouse.core.mapping.plan.spi;

import org.jmouse.core.mapping.ObjectMapper;

import java.util.Objects;

public record PlanBuildContext(ObjectMapper mapper) {

    public PlanBuildContext(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

}
