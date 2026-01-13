package org.jmouse.common.pipeline.definition.processing;

import org.jmouse.common.pipeline.definition.model.PipelineDefinition;

public interface DefinitionPostProcessor {
    PipelineDefinition process(PipelineDefinition definition);
}