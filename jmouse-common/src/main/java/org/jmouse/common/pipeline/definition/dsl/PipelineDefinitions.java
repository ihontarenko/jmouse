package org.jmouse.common.pipeline.definition.dsl;

import org.jmouse.common.pipeline.definition.model.PipelineDefinition;

import java.util.function.Consumer;

public final class PipelineDefinitions {

    private PipelineDefinitions() {}

    public static PipelineDefinition pipeline(String name, Consumer<PipelineBuilder> specifier) {
        PipelineBuilder builder = new PipelineBuilder(name);
        specifier.accept(builder);
        return builder.build();
    }
}
