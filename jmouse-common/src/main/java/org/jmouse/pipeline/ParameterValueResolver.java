package org.jmouse.pipeline;

import org.jmouse.pipeline.context.PipelineContext;

public interface ParameterValueResolver {
    Object resolve(String name, String value, PipelineContext ctx);
}
