package org.jmouse.pipeline.runtime;

import org.jmouse.pipeline.PipelineChain;
import org.jmouse.pipeline.definition.model.PipelineDefinition;

public interface PipelineCompiler {
    PipelineChain compile(PipelineDefinition definition, String chainName);
}
