package org.jmouse.pipeline.definition.loading;

import org.jmouse.pipeline.definition.model.PipelineDefinition;

public interface PipelineDefinitionReader {
    boolean supports(DefinitionSource source);
    PipelineDefinition read(DefinitionSource source);
}