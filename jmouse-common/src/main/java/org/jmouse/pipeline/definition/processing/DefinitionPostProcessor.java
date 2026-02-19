package org.jmouse.pipeline.definition.processing;

import org.jmouse.pipeline.definition.model.PipelineDefinition;

public interface DefinitionPostProcessor {
    PipelineDefinition process(PipelineDefinition definition);
}