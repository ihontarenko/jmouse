package org.jmouse.common.pipeline.definition.dsl;

import org.jmouse.common.pipeline.definition.model.PipelineDefinition;
import org.jmouse.core.Customizer;

import java.util.function.Consumer;

public final class PipelineDefinitions {

    private PipelineDefinitions() {}

    public static PipelineDefinition pipeline(String name, Customizer<PipelineBuilder> customizer) {
        PipelineBuilder builder = new PipelineBuilder(name);
        customizer.customize(builder);
        return builder.build();
    }
}
