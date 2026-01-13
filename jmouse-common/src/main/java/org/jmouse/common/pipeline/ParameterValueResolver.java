package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.PipelineContext;

public interface ParameterValueResolver {
    Object resolve(String name, String value, PipelineContext ctx);
}
