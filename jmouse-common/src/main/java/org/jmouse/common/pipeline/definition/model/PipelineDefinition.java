package org.jmouse.common.pipeline.definition.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record PipelineDefinition(
        String name,
        Map<String, ChainDefinition> chains
) {
    public PipelineDefinition {
        chains = (chains != null ? new LinkedHashMap<>(chains) : new LinkedHashMap<>());
    }

    public ChainDefinition chain(String name) {
        return chains.get(name);
    }
}
