package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.PipelineContext;
import org.jmouse.core.context.mutable.MutableArgumentsContext;

public final class FallbackProcessor implements PipelineProcessor {

    @Override
    public PipelineResult process(PipelineContext context, MutableArgumentsContext arguments, PipelineResult previous) {
        Throwable throwable = context.getValue("EXCEPTION");
        throw new PipelineRuntimeException(
                "ERROR DURING PIPELINE EXECUTION WITH CAUSE: [%s]".formatted(
                        throwable == null ? "UNKNOWN" : throwable.getMessage()
                ),
                throwable
        );
    }
}
