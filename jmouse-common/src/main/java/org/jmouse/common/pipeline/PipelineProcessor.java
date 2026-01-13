package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.PipelineContext;
import org.jmouse.core.context.ArgumentsContext;

public interface PipelineProcessor {

    default Enum<?> process(PipelineContext context) throws Exception {
        return process(context, context.getArgumentsContext());
    }

    Enum<?> process(PipelineContext context, ArgumentsContext arguments)
            throws Exception;

}
