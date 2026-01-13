package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.PipelineContext;

public interface PipelineChain {
    void proceed(PipelineContext context) throws Exception;
}
