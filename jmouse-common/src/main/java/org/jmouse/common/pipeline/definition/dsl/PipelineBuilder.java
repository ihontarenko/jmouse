package org.jmouse.common.pipeline.definition.dsl;

import org.jmouse.common.pipeline.definition.model.ChainDefinition;
import org.jmouse.common.pipeline.definition.model.PipelineDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PipelineBuilder {

    private final String name;
    private final Map<String, ChainBuilder> chains = new LinkedHashMap<>();

    PipelineBuilder(String name) {
        this.name = name;
    }

    public PipelineBuilder chain(String name, Consumer<ChainBuilder> specifier) {
        ChainBuilder chain = chains.computeIfAbsent(name, ChainBuilder::new);
        specifier.accept(chain);
        return this;
    }

    PipelineDefinition build() {
        Map<String, ChainDefinition> built = new LinkedHashMap<>();

        for (ChainBuilder chainBuilder : chains.values()) {
            ChainDefinition cd = chainBuilder.build();
            built.put(cd.name(), cd);
        }

        return new PipelineDefinition(name, built);
    }
}
