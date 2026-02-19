package org.jmouse.dom.meterializer;

import org.jmouse.dom.Node;
import org.jmouse.meterializer.AbstractRenderingPipeline;
import org.jmouse.meterializer.PipelineBuilder;

public final class RenderingPipeline extends AbstractRenderingPipeline<Node, RenderingPipeline> {

    public RenderingPipeline(PipelineBuilder<Node, RenderingPipeline> builder) {
        super(builder);
    }

    public static PipelineBuilder<Node, RenderingPipeline> builder() {
        PipelineBuilder<Node, RenderingPipeline> builder = new PipelineBuilder<>();
        builder.instanceFactory(RenderingPipeline::new);
        return builder;
    }

}

