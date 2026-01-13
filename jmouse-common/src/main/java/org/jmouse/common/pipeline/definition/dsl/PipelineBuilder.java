package org.jmouse.common.pipeline.definition.dsl;

import org.jmouse.common.pipeline.definition.model.ChainDefinition;
import org.jmouse.common.pipeline.definition.model.PipelineDefinition;
import org.jmouse.core.Customizer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PipelineBuilder {

    private final String name;
    private final Map<String, ChainBuilder> chains = new LinkedHashMap<>();

    PipelineBuilder(String name) {
        this.name = name;
    }

    public PipelineBuilder chain(String name, Customizer<ChainBuilder> customizer) {
        ChainBuilder chain = chains.computeIfAbsent(name, ChainBuilder::new);
        customizer.customize(chain);
        return this;
    }

    PipelineDefinition build() {
        Map<String, ChainDefinition> built = new LinkedHashMap<>();

        for (ChainBuilder chainBuilder : chains.values()) {
            ChainDefinition chainDefinition = chainBuilder.build();
            built.put(chainDefinition.name(), chainDefinition);
        }

        return new PipelineDefinition(name, built);
    }
}
