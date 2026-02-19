package org.jmouse.pipeline;

import org.jmouse.pipeline.context.DefaultPipelineContext;
import org.jmouse.pipeline.context.PipelineContext;

public class PipelineContextFactory {

    public static PipelineContext createByDefault(Object... objects) {
        DefaultPipelineContext context = new DefaultPipelineContext();

        for (Object object : objects) {
            context.setArgument(object);
        }

        return context;
    }

}
