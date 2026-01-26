package org.jmouse.core.mapping.plan.build;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.ObjectMapper;
import org.jmouse.core.mapping.config.MappingConfig;

public record PlanBuildContext(ObjectMapper mapper, MappingConfig mappingConfig) {

    public PlanBuildContext(ObjectMapper mapper, MappingConfig mappingConfig) {
        this.mapper = Verify.nonNull(mapper, "mapper");
        this.mappingConfig = Verify.nonNull(mappingConfig, "mappingConfig");
    }

}
