package org.jmouse.common.pipeline.runtime;

import org.jmouse.common.pipeline.PipelineChain;
import org.jmouse.common.pipeline.definition.model.PipelineDefinition;

public interface PipelineCompiler {
    PipelineChain compile(PipelineDefinition definition, String chainName);
}
