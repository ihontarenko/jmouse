package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.PipelineContext;
import org.jmouse.core.context.mutable.MutableArgumentsContext;

public interface PipelineProcessor {

    default PipelineResult process(PipelineContext context) throws Exception {
        return process(context, context.getArgumentsContext(), null);
    }

    default PipelineResult process(PipelineContext context, MutableArgumentsContext arguments) throws Exception {
        return process(context, arguments, null);
    }

    PipelineResult process(PipelineContext context, MutableArgumentsContext arguments, PipelineResult previous)
            throws Exception;
}
