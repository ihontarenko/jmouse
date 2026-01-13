package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.DefaultPipelineContext;
import org.jmouse.common.pipeline.context.PipelineContext;

public class PipelineContextFactory {

    public static PipelineContext createByDefault(Object... objects) {
        DefaultPipelineContext context = new DefaultPipelineContext();

        for (Object object : objects) {
            context.setArgument(object);
        }

        return context;
    }

}
