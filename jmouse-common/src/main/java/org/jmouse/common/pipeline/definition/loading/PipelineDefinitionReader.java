package org.jmouse.common.pipeline.definition.loading;

import org.jmouse.common.pipeline.definition.model.PipelineDefinition;

public interface PipelineDefinitionReader {
    boolean supports(DefinitionSource source);
    PipelineDefinition read(DefinitionSource source);
}