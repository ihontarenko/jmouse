package org.jmouse.pipeline;

import org.jmouse.pipeline.context.PipelineContext;

public interface PipelineChain {
    void proceed(PipelineContext context) throws Exception;
}
